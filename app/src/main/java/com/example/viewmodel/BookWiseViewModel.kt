package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookWiseApplication
import com.example.data.importer.BackupInspection
import com.example.data.importer.BackupManager
import com.example.data.importer.BackupValidationResult
import com.example.data.importer.BookImporter
import com.example.data.importer.ImportResult
import com.example.data.storage.SearchHit
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.Book
import com.example.model.BookEditorDraft
import com.example.model.BookNote
import com.example.model.BookType
import com.example.model.Bookmark
import com.example.model.CategoryItem
import com.example.model.ChapterContent
import com.example.model.DuplicateResolution
import com.example.model.EditableChapterDraft
import com.example.model.Highlight
import com.example.model.LibraryFilter
import com.example.model.ReadingProgress
import com.example.model.ReadingStatus
import com.example.model.SortOption
import com.example.tts.OfflineVoiceInfo
import com.example.tts.TtsPlaybackState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StorageStats(
  val libraryBytes: Long = 0L,
  val cacheBytes: Long = 0L,
)

class BookWiseViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as BookWiseApplication
  val repository = app.repository
  val ttsManager = app.ttsManager

  val books: StateFlow<List<Book>> = repository.books
  val categories: StateFlow<List<CategoryItem>> = repository.categories
  val settings: StateFlow<AppSettings> = repository.settings
  val ttsState: StateFlow<TtsPlaybackState> = ttsManager.state
  val offlineVoices: StateFlow<List<OfflineVoiceInfo>> = ttsManager.offlineVoices

  private val _isReady = MutableStateFlow(false)
  val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _pendingBackup = MutableStateFlow<BackupInspection?>(null)
  val pendingBackup: StateFlow<BackupInspection?> = _pendingBackup.asStateFlow()

  private val _pendingMigration = MutableStateFlow<ImportResult.MultiBookMigration?>(null)
  val pendingMigration: StateFlow<ImportResult.MultiBookMigration?> = _pendingMigration.asStateFlow()

  private val _storageStats = MutableStateFlow(StorageStats())
  val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

  // Reader active state
  private val _activeChapter = MutableStateFlow<ChapterContent?>(null)
  val activeChapter: StateFlow<ChapterContent?> = _activeChapter.asStateFlow()

  private val _readerBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
  val readerBookmarks: StateFlow<List<Bookmark>> = _readerBookmarks.asStateFlow()

  private val _readerHighlights = MutableStateFlow<List<Highlight>>(emptyList())
  val readerHighlights: StateFlow<List<Highlight>> = _readerHighlights.asStateFlow()

  private val _readerNotes = MutableStateFlow<List<BookNote>>(emptyList())
  val readerNotes: StateFlow<List<BookNote>> = _readerNotes.asStateFlow()

  private val _inBookSearchHits = MutableStateFlow<List<SearchHit>>(emptyList())
  val inBookSearchHits: StateFlow<List<SearchHit>> = _inBookSearchHits.asStateFlow()

  private var draftSaveJob: Job? = null

  init {
    viewModelScope.launch {
      repository.initialize()
      ttsManager.ensureInitialized()
      refreshStorageStats()
      _isReady.value = true
    }
  }

  fun clearMessage() {
    _snackbarMessage.value = null
  }

  fun showMessage(msg: String) {
    _snackbarMessage.value = msg
  }

  fun toggleLightDarkQuick() {
    viewModelScope.launch {
      repository.updateSettings { cur ->
        val next = if (cur.themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
        cur.copy(themeMode = next)
      }
    }
  }

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    viewModelScope.launch { repository.updateSettings(transform) }
  }

  fun filterAndSort(
    allBooks: List<Book>,
    bookType: BookType?,
    filter: LibraryFilter,
  ): List<Book> {
    val q = filter.searchQuery.trim().lowercase()
    val filtered =
      allBooks.filter { book ->
        val matchesType = bookType == null || book.bookType == bookType
        val matchesQuery =
          q.isEmpty() ||
            book.title.lowercase().contains(q) ||
            book.subtitle.lowercase().contains(q) ||
            book.author.lowercase().contains(q) ||
            book.category.lowercase().contains(q) ||
            book.tags.any { it.lowercase().contains(q) }
        val matchesCat =
          filter.category.isNullOrBlank() ||
            book.category.equals(filter.category, ignoreCase = true)
        val matchesLang =
          filter.languageCode.isNullOrBlank() ||
            book.editions.containsKey(filter.languageCode)
        val matchesFav = !filter.favoritesOnly || book.isFavorite
        val matchesStatus = filter.readingStatus == null || book.readingStatus == filter.readingStatus
        matchesType && matchesQuery && matchesCat && matchesLang && matchesFav && matchesStatus
      }

    return when (filter.sortOption) {
      SortOption.RECENTLY_READ ->
        filtered.sortedWith(compareByDescending<Book> { it.lastReadAt }.thenByDescending { it.updatedAt })
      SortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt }
      SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
      SortOption.AUTHOR_ASC -> filtered.sortedBy { it.author.lowercase() }
    }
  }

  // Book actions
  fun toggleFavorite(bookId: String) {
    viewModelScope.launch { repository.toggleFavorite(bookId) }
  }

  fun setReadingStatus(bookId: String, status: ReadingStatus) {
    viewModelScope.launch { repository.updateReadingStatus(bookId, status) }
  }

  fun deleteBook(bookId: String, onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      if (ttsState.value.bookId == bookId) {
        ttsManager.stop()
      }
      repository.deleteBook(bookId)
      refreshStorageStats()
      _snackbarMessage.value = "Book removed from library."
      onDeleted()
    }
  }

  // Category actions
  fun createCategory(name: String, subcategory: String = "") {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.createCategory(name, subcategory)
      _snackbarMessage.value = "Category '$name' saved."
    }
  }

  fun renameCategory(categoryId: String, newName: String) {
    if (newName.isBlank()) return
    viewModelScope.launch {
      repository.renameCategory(categoryId, newName)
      _snackbarMessage.value = "Category renamed."
    }
  }

  fun deleteCategory(categoryId: String) {
    viewModelScope.launch {
      repository.deleteCategory(categoryId)
      _snackbarMessage.value = "Category deleted."
    }
  }

  // Reader actions
  fun loadBookAnnotationsAndChapter(bookId: String, languageCode: String, chapterIndex: Int) {
    viewModelScope.launch {
      _activeChapter.value = repository.loadChapterContent(bookId, languageCode, chapterIndex)
      _readerBookmarks.value = repository.getBookmarks(bookId)
      _readerHighlights.value = repository.getHighlights(bookId)
      _readerNotes.value = repository.getNotes(bookId)
    }
  }

  fun loadChapterOnly(bookId: String, languageCode: String, chapterIndex: Int) {
    viewModelScope.launch {
      _activeChapter.value = repository.loadChapterContent(bookId, languageCode, chapterIndex)
    }
  }

  fun saveReadingProgress(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
    paragraphIndex: Int,
    scrollOffset: Int,
    totalChapters: Int,
    totalParagraphsInChapter: Int,
  ) {
    if (!settings.value.autoSaveReadingPosition) return
    viewModelScope.launch {
      val safeChTotal = totalChapters.coerceAtLeast(1)
      val safeParaTotal = totalParagraphsInChapter.coerceAtLeast(1)
      val chBase = chapterIndex.toFloat() / safeChTotal
      val paraFrac = (paragraphIndex + 1).toFloat() / safeParaTotal / safeChTotal
      val overall = (chBase + paraFrac).coerceIn(0f, 1f)
      repository.updateReadingProgress(
        bookId = bookId,
        languageCode = languageCode,
        progress =
          ReadingProgress(
            chapterIndex = chapterIndex,
            paragraphIndex = paragraphIndex,
            scrollOffset = scrollOffset,
            overallProgress = overall,
            lastUpdatedAt = System.currentTimeMillis(),
          ),
      )
    }
  }

  fun addBookmark(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
    chapterTitle: String,
    paragraphIndex: Int,
    excerpt: String,
    note: String = "",
  ) {
    viewModelScope.launch {
      val bm =
        Bookmark(
          id = "bm_${UUID.randomUUID().toString().take(8)}",
          bookId = bookId,
          languageCode = languageCode,
          chapterIndex = chapterIndex,
          chapterTitle = chapterTitle,
          paragraphIndex = paragraphIndex,
          excerpt = excerpt.take(140),
          note = note,
        )
      repository.addBookmark(bm)
      _readerBookmarks.value = repository.getBookmarks(bookId)
      _snackbarMessage.value = "Bookmark saved."
    }
  }

  fun removeBookmark(bookId: String, bookmarkId: String) {
    viewModelScope.launch {
      repository.removeBookmark(bookId, bookmarkId)
      _readerBookmarks.value = repository.getBookmarks(bookId)
    }
  }

  fun addHighlight(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
    paragraphIndex: Int,
    text: String,
    note: String = "",
  ) {
    viewModelScope.launch {
      val hl =
        Highlight(
          id = "hl_${UUID.randomUUID().toString().take(8)}",
          bookId = bookId,
          languageCode = languageCode,
          chapterIndex = chapterIndex,
          paragraphIndex = paragraphIndex,
          text = text,
          note = note,
        )
      repository.addHighlight(hl)
      _readerHighlights.value = repository.getHighlights(bookId)
      _snackbarMessage.value = "Passage highlighted."
    }
  }

  fun removeHighlight(bookId: String, highlightId: String) {
    viewModelScope.launch {
      repository.removeHighlight(bookId, highlightId)
      _readerHighlights.value = repository.getHighlights(bookId)
    }
  }

  fun saveNote(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
    paragraphIndex: Int,
    title: String,
    content: String,
  ) {
    if (content.isBlank()) return
    viewModelScope.launch {
      val note =
        BookNote(
          id = "note_${UUID.randomUUID().toString().take(8)}",
          bookId = bookId,
          languageCode = languageCode,
          chapterIndex = chapterIndex,
          paragraphIndex = paragraphIndex,
          title = title.ifBlank { "Reading Note" },
          content = content.trim(),
        )
      repository.saveNote(note)
      _readerNotes.value = repository.getNotes(bookId)
      _snackbarMessage.value = "Note saved offline."
    }
  }

  fun deleteNote(bookId: String, noteId: String) {
    viewModelScope.launch {
      repository.deleteNote(bookId, noteId)
      _readerNotes.value = repository.getNotes(bookId)
    }
  }

  fun searchInBook(bookId: String, languageCode: String, query: String) {
    viewModelScope.launch {
      _inBookSearchHits.value = repository.searchInBook(bookId, languageCode, query)
    }
  }

  fun clearInBookSearch() {
    _inBookSearchHits.value = emptyList()
  }

  // Editor & Drafts
  suspend fun loadBookIntoDraft(bookId: String?, languageCode: String?): BookEditorDraft {
    if (bookId == null) {
      return repository.loadDraft()
        ?: BookEditorDraft(
          chapters = listOf(EditableChapterDraft("ch_1", "Chapter 1", ""))
        )
    }
    val book = books.value.firstOrNull { it.id == bookId } ?: return BookEditorDraft()
    val targetLang = languageCode ?: book.defaultLanguageCode
    val existingEdition = book.editions[targetLang]
    val chapters =
      if (existingEdition != null) {
        repository.loadEditableChapters(book.id, targetLang)
      } else {
        listOf(EditableChapterDraft("ch_1", if (targetLang == "am") "ምዕራፍ ፩" else "Chapter 1", ""))
      }
    return BookEditorDraft(
      editingBookId = book.id,
      title = book.title,
      subtitle = book.subtitle,
      author = book.author,
      category = book.category,
      subcategory = book.subcategory,
      description = book.description,
      publicationYear = book.publicationYear,
      languageCode = targetLang,
      bookType = book.bookType,
      tagsText = book.tags.joinToString(", "),
      personalNotes = book.personalNotes,
      coverImagePath = existingEdition?.coverImagePath,
      bundledCoverKey = existingEdition?.bundledCoverKey,
      chapters = chapters,
    )
  }

  fun autoSaveDraft(draft: BookEditorDraft) {
    if (draft.editingBookId != null) return // Only auto-save new-book drafts
    draftSaveJob?.cancel()
    draftSaveJob =
      viewModelScope.launch {
        delay(500)
        repository.saveDraft(draft)
      }
  }

  fun clearDraft() {
    draftSaveJob?.cancel()
    viewModelScope.launch { repository.clearDraft() }
  }

  suspend fun saveBookFromDraft(draft: BookEditorDraft): Book {
    val saved = repository.saveBookDraftEdition(draft)
    refreshStorageStats()
    _snackbarMessage.value = "'${saved.title}' saved offline."
    return saved
  }

  suspend fun copyPickedCoverToTemp(uri: Uri): File? = repository.copyUriToTempCoverFile(uri)

  suspend fun importFileToDraft(uri: Uri): ImportResult = BookImporter.importFromUri(app, uri)

  fun confirmMultiBookMigration(resolution: DuplicateResolution) {
    val migration = _pendingMigration.value ?: return
    viewModelScope.launch {
      var imported = 0
      migration.books.forEach { bundle ->
        val existing =
          books.value.firstOrNull {
            it.id == bundle.book.id ||
              (it.title.equals(bundle.book.title, true) &&
                it.author.equals(bundle.book.author, true))
          }
        val finalBook =
          if (existing != null) {
            when (resolution) {
              DuplicateResolution.SKIP -> return@forEach
              DuplicateResolution.REPLACE -> bundle.book.copy(id = existing.id)
              DuplicateResolution.KEEP_BOTH ->
                bundle.book.copy(
                  id = "book_${UUID.randomUUID().toString().take(12)}",
                  title = "${bundle.book.title} (Migrated)",
                )
            }
          } else {
            bundle.book
          }
        repository.importBookFromPreparedFolder(
          book = finalBook,
          chaptersByLang = bundle.chaptersByLang,
          coverFilesByLang = bundle.coverBytesByLang,
          bookmarks = emptyList(),
          highlights = emptyList(),
          notes = emptyList(),
        )
        imported++
      }
      _pendingMigration.value = null
      refreshStorageStats()
      _snackbarMessage.value = "Migrated $imported book(s) from ${migration.sourceLabel}."
    }
  }

  fun dismissMultiBookMigration() {
    _pendingMigration.value = null
  }

  // Backup & Restore
  fun exportBookZip(bookId: String, outputUri: Uri) {
    viewModelScope.launch {
      val result = BackupManager.exportBookZip(app, repository, bookId, outputUri)
      _snackbarMessage.value =
        if (result.isSuccess) "Book exported as BookWise ZIP."
        else "Export failed: ${result.exceptionOrNull()?.message}"
    }
  }

  fun exportLibraryZip(outputUri: Uri) {
    viewModelScope.launch {
      val result = BackupManager.exportLibraryZip(app, repository, outputUri)
      _snackbarMessage.value =
        if (result.isSuccess) "Full library backup saved."
        else "Backup failed: ${result.exceptionOrNull()?.message}"
    }
  }

  fun exportSettingsJson(outputUri: Uri) {
    viewModelScope.launch {
      val result = BackupManager.exportSettingsJson(app, repository, outputUri)
      _snackbarMessage.value =
        if (result.isSuccess) "Settings exported."
        else "Export failed: ${result.exceptionOrNull()?.message}"
    }
  }

  fun inspectBackupFile(uri: Uri) {
    viewModelScope.launch {
      when (val res = BackupManager.inspectBackupUri(app, repository, uri)) {
        is BackupValidationResult.Valid -> {
          if (res.inspection.entries.isEmpty() && res.inspection.settings != null) {
            repository.replaceSettings(res.inspection.settings)
            _snackbarMessage.value = "Settings restored from backup."
          } else {
            _pendingBackup.value = res.inspection
          }
        }
        is BackupValidationResult.Invalid -> {
          // Also try BookWise JSON/TrebEdit migration if user picked a non-ZIP migration file
          when (val imp = BookImporter.importFromUri(app, uri)) {
            is ImportResult.MultiBookMigration -> _pendingMigration.value = imp
            is ImportResult.DraftReady -> {
              val draft = imp.draft
              repository.saveBookDraftEdition(draft)
              refreshStorageStats()
              _snackbarMessage.value = "Imported '${draft.title}'."
            }
            else -> _snackbarMessage.value = res.reason
          }
        }
      }
    }
  }

  fun confirmRestoreBackup(resolution: DuplicateResolution, restoreSettings: Boolean) {
    val inspection = _pendingBackup.value ?: return
    viewModelScope.launch {
      val count =
        BackupManager.restoreBackup(
          repository = repository,
          inspection = inspection,
          duplicateResolution = resolution,
          restoreSettings = restoreSettings,
        )
      _pendingBackup.value = null
      refreshStorageStats()
      _snackbarMessage.value = "Restored $count book(s) into offline library."
    }
  }

  fun dismissPendingBackup() {
    _pendingBackup.value = null
  }

  fun refreshStorageStats() {
    viewModelScope.launch {
      _storageStats.value =
        StorageStats(
          libraryBytes = repository.computeLibrarySizeBytes(),
          cacheBytes = repository.computeCacheSizeBytes(),
        )
    }
  }

  fun clearTempCache() {
    viewModelScope.launch {
      repository.clearTemporaryCache()
      refreshStorageStats()
      _snackbarMessage.value = "Temporary cache cleared."
    }
  }
}
