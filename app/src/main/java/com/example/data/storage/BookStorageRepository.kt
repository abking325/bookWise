package com.example.data.storage

import android.content.Context
import android.net.Uri
import com.example.model.AppSettings
import com.example.model.Book
import com.example.model.BookEdition
import com.example.model.BookEditorDraft
import com.example.model.BookNote
import com.example.model.Bookmark
import com.example.model.CategoryItem
import com.example.model.ChapterContent
import com.example.model.ChapterMeta
import com.example.model.EditableChapterDraft
import com.example.model.Highlight
import com.example.model.LanguageCode
import com.example.model.ReadingProgress
import com.example.model.ReadingStatus
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SearchHit(
  val chapterIndex: Int,
  val chapterTitle: String,
  val paragraphIndex: Int,
  val snippet: String,
)

class BookStorageRepository(private val context: Context) {

  private val mutex = Mutex()

  val rootDir: File
    get() = File(context.filesDir, "bookwise_library")

  private val booksRootDir: File
    get() = File(rootDir, "books")

  private val indexFile: File
    get() = File(rootDir, "library_index.json")

  private val categoriesFile: File
    get() = File(rootDir, "categories.json")

  private val settingsFile: File
    get() = File(rootDir, "settings.json")

  private val draftFile: File
    get() = File(rootDir, "drafts/book_draft.json")

  private val _books = MutableStateFlow<List<Book>>(emptyList())
  val books: StateFlow<List<Book>> = _books.asStateFlow()

  private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
  val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  @Volatile private var initialized = false

  suspend fun initialize() =
    withContext(Dispatchers.IO) {
      if (initialized) return@withContext
      mutex.withLock {
        if (initialized) return@withContext
        rootDir.mkdirs()
        booksRootDir.mkdirs()

        // Load or initialize settings
        if (settingsFile.exists()) {
          runCatching {
            val json = JSONObject(settingsFile.readText())
            _settings.value = BookJsonSerializer.settingsFromJson(json)
          }
        } else {
          AtomicFileWriter.writeTextAtomically(
            settingsFile,
            BookJsonSerializer.settingsToJson(_settings.value).toString(2),
          )
        }

        // Load or initialize categories
        if (categoriesFile.exists()) {
          runCatching {
            val arr = JSONArray(categoriesFile.readText())
            _categories.value = BookJsonSerializer.categoriesFromJson(arr)
          }.onFailure {
            _categories.value = StarterLibrarySeeder.defaultCategories()
          }
        } else {
          val defaults = StarterLibrarySeeder.defaultCategories()
          _categories.value = defaults
          AtomicFileWriter.writeTextAtomically(
            categoriesFile,
            BookJsonSerializer.categoriesToJson(defaults).toString(2),
          )
        }

        // Load library index or seed initial public-domain library on very first launch
        if (!indexFile.exists()) {
          seedStarterBooksLocked()
        } else {
          loadLibraryFromDiskLocked()
        }
        initialized = true
      }
    }

  private fun seedStarterBooksLocked() {
    val now = System.currentTimeMillis()
    val specs = StarterLibrarySeeder.starterBooks(now)
    val coverKeys =
      mapOf(
        "book_meditations_aurelius" to "img_cover_meditations",
        "book_art_of_war_sun_tzu" to "img_cover_art_of_war",
        "book_ethiopian_proverbs_bilingual" to "img_cover_wisdom_bilingual",
        "book_meditations_summary" to "img_cover_meditations",
      )
    val accentHexes =
      mapOf(
        "book_meditations_aurelius" to "#1F242B",
        "book_art_of_war_sun_tzu" to "#8C3B3B",
        "book_ethiopian_proverbs_bilingual" to "#9E472A",
        "book_meditations_summary" to "#2C3542",
      )
    val seeded = mutableListOf<Book>()
    for (spec in specs) {
      val editionsMap = linkedMapOf<String, BookEdition>()
      for ((lang, drafts) in spec.editionChapters) {
        val metas = writeChaptersToDisk(spec.book.id, lang, drafts)
        editionsMap[lang] =
          BookEdition(
            languageCode = lang,
            languageLabel = LanguageCode.labelFor(lang),
            bundledCoverKey = coverKeys[spec.book.id],
            accentColorHex = accentHexes[spec.book.id] ?: "#162032",
            chapters = metas,
            progress = ReadingProgress(),
          )
      }
      val completeBook = spec.book.copy(editions = editionsMap)
      writeBookMetadataLocked(completeBook)
      seeded.add(completeBook)
    }
    _books.value = seeded
    writeIndexLocked(seeded)
  }

  private fun loadLibraryFromDiskLocked() {
    val loaded = mutableListOf<Book>()
    runCatching {
      val arr = JSONArray(indexFile.readText())
      for (i in 0 until arr.length()) {
        val entry = arr.optJSONObject(i) ?: continue
        val id = entry.optString("id")
        if (id.isBlank()) continue
        val metaFile = File(bookFolder(id), "metadata.json")
        if (metaFile.exists()) {
          runCatching {
            val bookJson = JSONObject(metaFile.readText())
            loaded.add(BookJsonSerializer.bookFromJson(bookJson))
          }
        }
      }
    }
    // Recover any orphaned book folders not in index
    booksRootDir.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
      if (loaded.none { it.id == folder.name }) {
        val metaFile = File(folder, "metadata.json")
        if (metaFile.exists()) {
          runCatching {
            loaded.add(BookJsonSerializer.bookFromJson(JSONObject(metaFile.readText())))
          }
        }
      }
    }
    _books.value = loaded
    writeIndexLocked(loaded)
  }

  fun bookFolder(bookId: String): File = File(booksRootDir, bookId)

  private fun editionChaptersDir(bookId: String, languageCode: String): File =
    File(bookFolder(bookId), "editions/$languageCode/chapters")

  private fun editionProgressFile(bookId: String, languageCode: String): File =
    File(bookFolder(bookId), "editions/$languageCode/progress.json")

  private fun writeChaptersToDisk(
    bookId: String,
    languageCode: String,
    chapters: List<EditableChapterDraft>,
  ): List<ChapterMeta> {
    val dir = editionChaptersDir(bookId, languageCode)
    if (dir.exists()) {
      dir.listFiles()?.forEach { it.delete() }
    } else {
      dir.mkdirs()
    }
    return chapters.mapIndexed { index, draft ->
      val safeId = draft.id.ifBlank { "ch_${index + 1}_${UUID.randomUUID().toString().take(6)}" }
      val fileName = "${index.toString().padStart(4, '0')}_$safeId.txt"
      val file = File(dir, fileName)
      AtomicFileWriter.writeTextAtomically(file, draft.bodyText)
      val words = draft.bodyText.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
      ChapterMeta(
        id = safeId,
        index = index,
        title = draft.title.ifBlank { "Chapter ${index + 1}" },
        fileName = fileName,
        wordCount = words,
        estimatedMinutes = (words / 220).coerceAtLeast(1),
      )
    }
  }

  private fun writeBookMetadataLocked(book: Book) {
    val metaFile = File(bookFolder(book.id), "metadata.json")
    AtomicFileWriter.writeTextAtomically(metaFile, BookJsonSerializer.bookToJson(book).toString(2))
  }

  private fun writeIndexLocked(books: List<Book>) {
    val arr = JSONArray()
    books.forEach { b ->
      arr.put(
        JSONObject().apply {
          put("id", b.id)
          put("title", b.title)
          put("author", b.author)
          put("updatedAt", b.updatedAt)
        }
      )
    }
    AtomicFileWriter.writeTextAtomically(indexFile, arr.toString(2))
  }

  suspend fun saveBookDraftEdition(draft: BookEditorDraft): Book =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val now = System.currentTimeMillis()
        val existing = draft.editingBookId?.let { id -> _books.value.firstOrNull { it.id == id } }
        val bookId = existing?.id ?: "book_${UUID.randomUUID().toString().take(12)}"
        val lang = draft.languageCode.lowercase().ifBlank { "en" }

        val preparedChapters =
          if (draft.chapters.isEmpty()) {
            listOf(EditableChapterDraft("ch_1", "Chapter 1", ""))
          } else {
            draft.chapters
          }
        val chapterMetas = writeChaptersToDisk(bookId, lang, preparedChapters)
        val existingEdition = existing?.editions?.get(lang)
        val coverPath = persistCoverIfNeeded(bookId, lang, draft.coverImagePath)

        val newEdition =
          BookEdition(
            languageCode = lang,
            languageLabel = LanguageCode.labelFor(lang),
            coverImagePath = coverPath ?: existingEdition?.coverImagePath,
            bundledCoverKey = draft.bundledCoverKey ?: existingEdition?.bundledCoverKey,
            accentColorHex = existingEdition?.accentColorHex ?: "#162032",
            chapters = chapterMetas,
            progress = existingEdition?.progress ?: ReadingProgress(),
          )

        val mergedEditions =
          (existing?.editions?.toMutableMap() ?: linkedMapOf()).apply {
            put(lang, newEdition)
          }

        val tags =
          draft.tagsText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val savedBook =
          Book(
            id = bookId,
            title = draft.title.trim().ifBlank { "Untitled Book" },
            subtitle = draft.subtitle.trim(),
            author = draft.author.trim().ifBlank { "Unknown Author" },
            category = draft.category.trim().ifBlank { "General" },
            subcategory = draft.subcategory.trim(),
            description = draft.description.trim(),
            publicationYear = draft.publicationYear.trim(),
            bookType = draft.bookType,
            defaultLanguageCode = existing?.defaultLanguageCode ?: lang,
            tags = tags,
            personalNotes = draft.personalNotes.trim(),
            isFavorite = existing?.isFavorite ?: false,
            readingStatus = existing?.readingStatus ?: ReadingStatus.UNREAD,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            lastReadAt = existing?.lastReadAt ?: 0L,
            editions = mergedEditions,
          )

        writeBookMetadataLocked(savedBook)
        ensureCategoryExistsLocked(savedBook.category, savedBook.subcategory)

        val updatedList =
          _books.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == bookId }
            if (idx >= 0) set(idx, savedBook) else add(0, savedBook)
          }
        _books.value = updatedList
        writeIndexLocked(updatedList)
        clearDraftLocked()
        savedBook
      }
    }

  private fun persistCoverIfNeeded(bookId: String, lang: String, sourcePath: String?): String? {
    if (sourcePath.isNullOrBlank()) return null
    val srcFile = File(sourcePath)
    if (!srcFile.exists()) return null
    val coversDir = File(bookFolder(bookId), "covers")
    coversDir.mkdirs()
    val destFile = File(coversDir, "${lang}_cover.jpg")
    if (srcFile.canonicalPath == destFile.canonicalPath) return destFile.absolutePath
    srcFile.copyTo(destFile, overwrite = true)
    return destFile.absolutePath
  }

  private fun ensureCategoryExistsLocked(categoryName: String, subcategoryName: String) {
    if (categoryName.isBlank()) return
    val current = _categories.value.toMutableList()
    val idx = current.indexOfFirst { it.name.equals(categoryName, ignoreCase = true) }
    if (idx < 0) {
      current.add(
        CategoryItem(
          id = "cat_${UUID.randomUUID().toString().take(8)}",
          name = categoryName,
          subcategories = if (subcategoryName.isNotBlank()) listOf(subcategoryName) else emptyList(),
          isCustom = true,
        )
      )
    } else if (
      subcategoryName.isNotBlank() &&
        current[idx].subcategories.none { it.equals(subcategoryName, ignoreCase = true) }
    ) {
      val item = current[idx]
      current[idx] = item.copy(subcategories = item.subcategories + subcategoryName)
    } else {
      return
    }
    _categories.value = current
    AtomicFileWriter.writeTextAtomically(
      categoriesFile,
      BookJsonSerializer.categoriesToJson(current).toString(2),
    )
  }

  suspend fun loadEditableChapters(bookId: String, languageCode: String): List<EditableChapterDraft> =
    withContext(Dispatchers.IO) {
      val book = _books.value.firstOrNull { it.id == bookId } ?: return@withContext emptyList()
      val edition = book.edition(languageCode) ?: return@withContext emptyList()
      val dir = editionChaptersDir(bookId, edition.languageCode)
      edition.chapters.map { meta ->
        val file = File(dir, meta.fileName)
        val body = if (file.exists()) file.readText() else ""
        EditableChapterDraft(id = meta.id, title = meta.title, bodyText = body)
      }
    }

  suspend fun loadChapterContent(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
  ): ChapterContent? =
    withContext(Dispatchers.IO) {
      val book = _books.value.firstOrNull { it.id == bookId } ?: return@withContext null
      val edition = book.edition(languageCode) ?: return@withContext null
      val meta = edition.chapters.getOrNull(chapterIndex) ?: return@withContext null
      val file = File(editionChaptersDir(bookId, edition.languageCode), meta.fileName)
      val raw = if (file.exists()) file.readText() else ""
      val paragraphs = splitIntoParagraphs(raw)
      ChapterContent(meta = meta, paragraphs = paragraphs)
    }

  fun splitIntoParagraphs(raw: String): List<String> {
    val normalized = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
    if (normalized.isEmpty()) return listOf("")
    val blocks = normalized.split(Regex("\n\\s*\n+")).map { it.trim() }.filter { it.isNotEmpty() }
    return if (blocks.isNotEmpty()) blocks else listOf(normalized)
  }

  suspend fun searchInBook(
    bookId: String,
    languageCode: String,
    query: String,
    maxHits: Int = 80,
  ): List<SearchHit> =
    withContext(Dispatchers.IO) {
      val cleanQuery = query.trim()
      if (cleanQuery.isEmpty()) return@withContext emptyList()
      val book = _books.value.firstOrNull { it.id == bookId } ?: return@withContext emptyList()
      val edition = book.edition(languageCode) ?: return@withContext emptyList()
      val dir = editionChaptersDir(bookId, edition.languageCode)
      val hits = mutableListOf<SearchHit>()
      for (meta in edition.chapters) {
        val file = File(dir, meta.fileName)
        if (!file.exists()) continue
        val paragraphs = splitIntoParagraphs(file.readText())
        paragraphs.forEachIndexed { pIdx, para ->
          if (para.contains(cleanQuery, ignoreCase = true)) {
            hits.add(
              SearchHit(
                chapterIndex = meta.index,
                chapterTitle = meta.title,
                paragraphIndex = pIdx,
                snippet = para.take(180),
              )
            )
            if (hits.size >= maxHits) return@withContext hits
          }
        }
      }
      hits
    }

  suspend fun updateReadingProgress(
    bookId: String,
    languageCode: String,
    progress: ReadingProgress,
  ) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = _books.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookId }
        if (idx < 0) return@withLock
        val book = list[idx]
        val ed = book.editions[languageCode] ?: return@withLock
        val updatedEd = ed.copy(progress = progress)
        val newStatus =
          when {
            progress.overallProgress >= 0.98f -> ReadingStatus.COMPLETED
            progress.overallProgress > 0.01f || progress.chapterIndex > 0 || progress.paragraphIndex > 0 ->
              ReadingStatus.READING
            else -> book.readingStatus
          }
        val updatedBook =
          book.copy(
            lastReadAt = progress.lastUpdatedAt,
            updatedAt = System.currentTimeMillis(),
            readingStatus = newStatus,
            editions = book.editions.toMutableMap().apply { put(languageCode, updatedEd) },
          )
        list[idx] = updatedBook
        _books.value = list
        AtomicFileWriter.writeTextAtomically(
          editionProgressFile(bookId, languageCode),
          BookJsonSerializer.progressToJson(progress).toString(2),
        )
        writeBookMetadataLocked(updatedBook)
      }
    }

  suspend fun toggleFavorite(bookId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = _books.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookId }
        if (idx < 0) return@withLock
        val updated = list[idx].copy(isFavorite = !list[idx].isFavorite, updatedAt = System.currentTimeMillis())
        list[idx] = updated
        _books.value = list
        writeBookMetadataLocked(updated)
      }
    }

  suspend fun updateReadingStatus(bookId: String, status: ReadingStatus) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val list = _books.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookId }
        if (idx < 0) return@withLock
        val updated = list[idx].copy(readingStatus = status, updatedAt = System.currentTimeMillis())
        list[idx] = updated
        _books.value = list
        writeBookMetadataLocked(updated)
      }
    }

  suspend fun deleteBook(bookId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val folder = bookFolder(bookId)
        if (folder.exists()) folder.deleteRecursively()
        val updated = _books.value.filterNot { it.id == bookId }
        _books.value = updated
        writeIndexLocked(updated)
      }
    }

  // Bookmarks, Highlights, and Notes
  suspend fun getBookmarks(bookId: String): List<Bookmark> =
    withContext(Dispatchers.IO) {
      val file = File(bookFolder(bookId), "bookmarks.json")
      if (!file.exists()) return@withContext emptyList()
      runCatching { BookJsonSerializer.bookmarksFromJson(JSONArray(file.readText())) }
        .getOrDefault(emptyList())
    }

  suspend fun addBookmark(bookmark: Bookmark) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(bookmark.bookId), "bookmarks.json")
        val current =
          if (file.exists()) {
            runCatching { BookJsonSerializer.bookmarksFromJson(JSONArray(file.readText())) }
              .getOrDefault(emptyList())
          } else emptyList()
        val updated = listOf(bookmark) + current
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.bookmarksToJson(updated).toString(2))
      }
    }

  suspend fun removeBookmark(bookId: String, bookmarkId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(bookId), "bookmarks.json")
        if (!file.exists()) return@withLock
        val current =
          runCatching { BookJsonSerializer.bookmarksFromJson(JSONArray(file.readText())) }
            .getOrDefault(emptyList())
        val updated = current.filterNot { it.id == bookmarkId }
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.bookmarksToJson(updated).toString(2))
      }
    }

  suspend fun getHighlights(bookId: String): List<Highlight> =
    withContext(Dispatchers.IO) {
      val file = File(bookFolder(bookId), "highlights.json")
      if (!file.exists()) return@withContext emptyList()
      runCatching { BookJsonSerializer.highlightsFromJson(JSONArray(file.readText())) }
        .getOrDefault(emptyList())
    }

  suspend fun addHighlight(highlight: Highlight) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(highlight.bookId), "highlights.json")
        val current =
          if (file.exists()) {
            runCatching { BookJsonSerializer.highlightsFromJson(JSONArray(file.readText())) }
              .getOrDefault(emptyList())
          } else emptyList()
        val updated = listOf(highlight) + current
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.highlightsToJson(updated).toString(2))
      }
    }

  suspend fun removeHighlight(bookId: String, highlightId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(bookId), "highlights.json")
        if (!file.exists()) return@withLock
        val current =
          runCatching { BookJsonSerializer.highlightsFromJson(JSONArray(file.readText())) }
            .getOrDefault(emptyList())
        val updated = current.filterNot { it.id == highlightId }
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.highlightsToJson(updated).toString(2))
      }
    }

  suspend fun getNotes(bookId: String): List<BookNote> =
    withContext(Dispatchers.IO) {
      val file = File(bookFolder(bookId), "notes.json")
      if (!file.exists()) return@withContext emptyList()
      runCatching { BookJsonSerializer.notesFromJson(JSONArray(file.readText())) }
        .getOrDefault(emptyList())
    }

  suspend fun saveNote(note: BookNote) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(note.bookId), "notes.json")
        val current =
          if (file.exists()) {
            runCatching { BookJsonSerializer.notesFromJson(JSONArray(file.readText())) }
              .getOrDefault(emptyList())
          } else emptyList()
        val updated =
          current.toMutableList().apply {
            val idx = indexOfFirst { it.id == note.id }
            if (idx >= 0) set(idx, note) else add(0, note)
          }
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.notesToJson(updated).toString(2))
      }
    }

  suspend fun deleteNote(bookId: String, noteId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val file = File(bookFolder(bookId), "notes.json")
        if (!file.exists()) return@withLock
        val current =
          runCatching { BookJsonSerializer.notesFromJson(JSONArray(file.readText())) }
            .getOrDefault(emptyList())
        val updated = current.filterNot { it.id == noteId }
        AtomicFileWriter.writeTextAtomically(file, BookJsonSerializer.notesToJson(updated).toString(2))
      }
    }

  // Category management
  suspend fun createCategory(name: String, subcategory: String = "") =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        ensureCategoryExistsLocked(name.trim(), subcategory.trim())
      }
    }

  suspend fun renameCategory(categoryId: String, newName: String) =
    withContext(Dispatchers.IO) {
      val trimmed = newName.trim()
      if (trimmed.isEmpty()) return@withContext
      mutex.withLock {
        val cats = _categories.value.toMutableList()
        val idx = cats.indexOfFirst { it.id == categoryId }
        if (idx < 0) return@withLock
        val oldName = cats[idx].name
        cats[idx] = cats[idx].copy(name = trimmed)
        _categories.value = cats
        AtomicFileWriter.writeTextAtomically(
          categoriesFile,
          BookJsonSerializer.categoriesToJson(cats).toString(2),
        )
        // Update books referencing old category name
        val updatedBooks =
          _books.value.map { b ->
            if (b.category.equals(oldName, ignoreCase = true)) {
              val nb = b.copy(category = trimmed, updatedAt = System.currentTimeMillis())
              writeBookMetadataLocked(nb)
              nb
            } else b
          }
        _books.value = updatedBooks
        writeIndexLocked(updatedBooks)
      }
    }

  suspend fun deleteCategory(categoryId: String) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val target = _categories.value.firstOrNull { it.id == categoryId } ?: return@withLock
        val cats = _categories.value.filterNot { it.id == categoryId }
        _categories.value = cats
        AtomicFileWriter.writeTextAtomically(
          categoriesFile,
          BookJsonSerializer.categoriesToJson(cats).toString(2),
        )
        val updatedBooks =
          _books.value.map { b ->
            if (b.category.equals(target.name, ignoreCase = true)) {
              val nb = b.copy(category = "Uncategorized")
              writeBookMetadataLocked(nb)
              nb
            } else b
          }
        _books.value = updatedBooks
        writeIndexLocked(updatedBooks)
      }
    }

  // Settings
  suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val updated = transform(_settings.value)
        _settings.value = updated
        AtomicFileWriter.writeTextAtomically(
          settingsFile,
          BookJsonSerializer.settingsToJson(updated).toString(2),
        )
      }
    }

  suspend fun replaceSettings(newSettings: AppSettings) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        _settings.value = newSettings
        AtomicFileWriter.writeTextAtomically(
          settingsFile,
          BookJsonSerializer.settingsToJson(newSettings).toString(2),
        )
      }
    }

  // Auto-saved Editor Draft
  suspend fun saveDraft(draft: BookEditorDraft) =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        AtomicFileWriter.writeTextAtomically(
          draftFile,
          BookJsonSerializer.draftToJson(draft).toString(2),
        )
      }
    }

  suspend fun loadDraft(): BookEditorDraft? =
    withContext(Dispatchers.IO) {
      if (!draftFile.exists()) return@withContext null
      runCatching { BookJsonSerializer.draftFromJson(JSONObject(draftFile.readText())) }.getOrNull()
    }

  suspend fun clearDraft() =
    withContext(Dispatchers.IO) {
      mutex.withLock { clearDraftLocked() }
    }

  private fun clearDraftLocked() {
    if (draftFile.exists()) draftFile.delete()
  }

  // Temporary file helpers & storage diagnostics
  suspend fun copyUriToTempCoverFile(uri: Uri): File? =
    withContext(Dispatchers.IO) {
      runCatching {
        val tempDir = File(context.cacheDir, "cover_temp").apply { mkdirs() }
        val outFile = File(tempDir, "picked_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          outFile.outputStream().use { output -> input.copyTo(output) }
        }
        outFile
      }.getOrNull()
    }

  suspend fun computeLibrarySizeBytes(): Long =
    withContext(Dispatchers.IO) {
      folderSize(rootDir)
    }

  suspend fun computeCacheSizeBytes(): Long =
    withContext(Dispatchers.IO) {
      folderSize(context.cacheDir)
    }

  suspend fun clearTemporaryCache(): Long =
    withContext(Dispatchers.IO) {
      context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
      0L
    }

  suspend fun importBookFromPreparedFolder(
    book: Book,
    chaptersByLang: Map<String, List<EditableChapterDraft>>,
    coverFilesByLang: Map<String, ByteArray>,
    bookmarks: List<Bookmark>,
    highlights: List<Highlight>,
    notes: List<BookNote>,
  ): Book =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val newEditions = linkedMapOf<String, BookEdition>()
        book.editions.forEach { (lang, ed) ->
          val drafts = chaptersByLang[lang] ?: emptyList()
          val metas = writeChaptersToDisk(book.id, lang, drafts)
          var coverPath = ed.coverImagePath
          val coverBytes = coverFilesByLang[lang]
          if (coverBytes != null && coverBytes.isNotEmpty()) {
            val coversDir = File(bookFolder(book.id), "covers").apply { mkdirs() }
            val dest = File(coversDir, "${lang}_cover.jpg")
            AtomicFileWriter.writeBytesAtomically(dest, coverBytes)
            coverPath = dest.absolutePath
          }
          newEditions[lang] =
            ed.copy(
              chapters = metas.ifEmpty { ed.chapters },
              coverImagePath = coverPath,
            )
        }
        val finalBook = book.copy(editions = newEditions)
        writeBookMetadataLocked(finalBook)
        ensureCategoryExistsLocked(finalBook.category, finalBook.subcategory)
        if (bookmarks.isNotEmpty()) {
          AtomicFileWriter.writeTextAtomically(
            File(bookFolder(finalBook.id), "bookmarks.json"),
            BookJsonSerializer.bookmarksToJson(bookmarks.map { it.copy(bookId = finalBook.id) })
              .toString(2),
          )
        }
        if (highlights.isNotEmpty()) {
          AtomicFileWriter.writeTextAtomically(
            File(bookFolder(finalBook.id), "highlights.json"),
            BookJsonSerializer.highlightsToJson(highlights.map { it.copy(bookId = finalBook.id) })
              .toString(2),
          )
        }
        if (notes.isNotEmpty()) {
          AtomicFileWriter.writeTextAtomically(
            File(bookFolder(finalBook.id), "notes.json"),
            BookJsonSerializer.notesToJson(notes.map { it.copy(bookId = finalBook.id) }).toString(2),
          )
        }

        val updated =
          _books.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == finalBook.id }
            if (idx >= 0) set(idx, finalBook) else add(0, finalBook)
          }
        _books.value = updated
        writeIndexLocked(updated)
        finalBook
      }
    }

  private fun folderSize(file: File): Long {
    if (!file.exists()) return 0L
    if (file.isFile) return file.length()
    return file.listFiles()?.sumOf { folderSize(it) } ?: 0L
  }
}
