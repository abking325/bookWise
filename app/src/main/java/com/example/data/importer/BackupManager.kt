package com.example.data.importer

import android.content.Context
import android.net.Uri
import com.example.data.storage.BookJsonSerializer
import com.example.data.storage.BookStorageRepository
import com.example.model.AppSettings
import com.example.model.Book
import com.example.model.BookNote
import com.example.model.Bookmark
import com.example.model.CategoryItem
import com.example.model.DuplicateResolution
import com.example.model.EditableChapterDraft
import com.example.model.Highlight
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupBookEntry(
  val book: Book,
  val chaptersByLang: Map<String, List<EditableChapterDraft>>,
  val coversByLang: Map<String, ByteArray>,
  val bookmarks: List<Bookmark>,
  val highlights: List<Highlight>,
  val notes: List<BookNote>,
  val duplicatesExistingBookId: String? = null,
)

data class BackupInspection(
  val entries: List<BackupBookEntry>,
  val categories: List<CategoryItem>,
  val settings: AppSettings?,
  val duplicateCount: Int,
)

sealed interface BackupValidationResult {
  data class Valid(val inspection: BackupInspection) : BackupValidationResult

  data class Invalid(val reason: String) : BackupValidationResult
}

object BackupManager {

  suspend fun exportBookZip(
    context: Context,
    repository: BookStorageRepository,
    bookId: String,
    outputUri: Uri,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val book =
          repository.books.value.firstOrNull { it.id == bookId }
            ?: error("Book not found")
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
          ZipOutputStream(out).use { zos ->
            writeManifest(zos, type = "single_book", bookCount = 1)
            writeBookFolderToZip(zos, repository, book)
          }
        } ?: error("Cannot open output destination")
      }
    }

  suspend fun exportLibraryZip(
    context: Context,
    repository: BookStorageRepository,
    outputUri: Uri,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val books = repository.books.value
        val categories = repository.categories.value
        val settings = repository.settings.value
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
          ZipOutputStream(out).use { zos ->
            writeManifest(zos, type = "full_library", bookCount = books.size)
            putTextEntry(
              zos,
              "categories.json",
              BookJsonSerializer.categoriesToJson(categories).toString(2),
            )
            putTextEntry(
              zos,
              "settings.json",
              BookJsonSerializer.settingsToJson(settings).toString(2),
            )
            books.forEach { book -> writeBookFolderToZip(zos, repository, book) }
          }
        } ?: error("Cannot open output destination")
      }
    }

  suspend fun exportSettingsJson(
    context: Context,
    repository: BookStorageRepository,
    outputUri: Uri,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val json = BookJsonSerializer.settingsToJson(repository.settings.value).toString(2)
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
          out.write(json.toByteArray(StandardCharsets.UTF_8))
        } ?: error("Cannot open output destination")
      }
    }

  private fun writeManifest(zos: ZipOutputStream, type: String, bookCount: Int) {
    val manifest =
      JSONObject().apply {
        put("format", "bookwise-backup-v1")
        put("type", type)
        put("bookCount", bookCount)
        put("exportedAt", System.currentTimeMillis())
      }
    putTextEntry(zos, "manifest.json", manifest.toString(2))
  }

  private fun writeBookFolderToZip(
    zos: ZipOutputStream,
    repository: BookStorageRepository,
    book: Book,
  ) {
    val folder = repository.bookFolder(book.id)
    val prefix = "books/${book.id}/"
    putTextEntry(zos, "${prefix}metadata.json", BookJsonSerializer.bookToJson(book).toString(2))

    listOf("bookmarks.json", "highlights.json", "notes.json").forEach { name ->
      val f = File(folder, name)
      if (f.exists()) putTextEntry(zos, "$prefix$name", f.readText())
    }

    book.editions.forEach { (lang, ed) ->
      ed.coverImagePath?.let { path ->
        val cf = File(path)
        if (cf.exists()) putBytesEntry(zos, "${prefix}covers/${lang}_cover.jpg", cf.readBytes())
      }
      val chDir = File(folder, "editions/$lang/chapters")
      ed.chapters.forEach { ch ->
        val chFile = File(chDir, ch.fileName)
        if (chFile.exists()) {
          putTextEntry(zos, "${prefix}editions/$lang/chapters/${ch.fileName}", chFile.readText())
        }
      }
    }
  }

  private fun putTextEntry(zos: ZipOutputStream, name: String, content: String) {
    putBytesEntry(zos, name, content.toByteArray(StandardCharsets.UTF_8))
  }

  private fun putBytesEntry(zos: ZipOutputStream, name: String, bytes: ByteArray) {
    zos.putNextEntry(ZipEntry(name))
    zos.write(bytes)
    zos.closeEntry()
  }

  suspend fun inspectBackupUri(
    context: Context,
    repository: BookStorageRepository,
    uri: Uri,
  ): BackupValidationResult =
    withContext(Dispatchers.IO) {
      val files = linkedMapOf<String, ByteArray>()
      val opened =
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { input ->
            val rawBytes = input.readBytes()
            // Check if it's a standalone settings.json
            val preview = rawBytes.toString(StandardCharsets.UTF_8).trim()
            if (preview.startsWith("{") && preview.contains("navigationStyle")) {
              val settings = BookJsonSerializer.settingsFromJson(JSONObject(preview))
              return@withContext BackupValidationResult.Valid(
                BackupInspection(emptyList(), emptyList(), settings, 0)
              )
            }
            ZipInputStream(rawBytes.inputStream()).use { zis ->
              var entry = zis.nextEntry
              while (entry != null) {
                if (!entry.isDirectory) {
                  files[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
              }
            }
          }
        }
      if (opened.isFailure || files.isEmpty()) {
        return@withContext BackupValidationResult.Invalid(
          "Selected file is not a valid BookWise ZIP backup or settings JSON."
        )
      }

      val manifestBytes = files["manifest.json"]
      if (manifestBytes != null) {
        val format = runCatching {
          JSONObject(manifestBytes.toString(StandardCharsets.UTF_8)).optString("format")
        }.getOrNull()
        if (format != null && !format.startsWith("bookwise-backup")) {
          return@withContext BackupValidationResult.Invalid("Unsupported backup format: $format")
        }
      }

      val settings =
        files["settings.json"]?.let {
          runCatching {
            BookJsonSerializer.settingsFromJson(JSONObject(it.toString(StandardCharsets.UTF_8)))
          }.getOrNull()
        }

      val categories =
        files["categories.json"]?.let {
          runCatching {
            BookJsonSerializer.categoriesFromJson(JSONArray(it.toString(StandardCharsets.UTF_8)))
          }.getOrDefault(emptyList())
        } ?: emptyList()

      val bookIds =
        files.keys
          .filter { it.startsWith("books/") && it.endsWith("/metadata.json") }
          .map { it.removePrefix("books/").substringBefore("/") }
          .distinct()

      if (bookIds.isEmpty() && settings == null) {
        return@withContext BackupValidationResult.Invalid(
          "Backup archive contains no BookWise books or settings."
        )
      }

      val existingBooks = repository.books.value
      val parsedEntries = mutableListOf<BackupBookEntry>()

      for (bId in bookIds) {
        val prefix = "books/$bId/"
        val metaBytes = files["${prefix}metadata.json"] ?: continue
        val book =
          runCatching {
            BookJsonSerializer.bookFromJson(JSONObject(metaBytes.toString(StandardCharsets.UTF_8)))
          }.getOrNull() ?: continue

        val chaptersByLang = linkedMapOf<String, List<EditableChapterDraft>>()
        val coversByLang = linkedMapOf<String, ByteArray>()

        book.editions.forEach { (lang, ed) ->
          val drafts =
            ed.chapters.map { ch ->
              val chBytes = files["${prefix}editions/$lang/chapters/${ch.fileName}"]
              EditableChapterDraft(
                id = ch.id,
                title = ch.title,
                bodyText = chBytes?.toString(StandardCharsets.UTF_8) ?: "",
              )
            }
          chaptersByLang[lang] = drafts
          files["${prefix}covers/${lang}_cover.jpg"]?.let { coversByLang[lang] = it }
        }

        val bookmarks =
          files["${prefix}bookmarks.json"]?.let {
            runCatching {
              BookJsonSerializer.bookmarksFromJson(JSONArray(it.toString(StandardCharsets.UTF_8)))
            }.getOrDefault(emptyList())
          } ?: emptyList()

        val highlights =
          files["${prefix}highlights.json"]?.let {
            runCatching {
              BookJsonSerializer.highlightsFromJson(JSONArray(it.toString(StandardCharsets.UTF_8)))
            }.getOrDefault(emptyList())
          } ?: emptyList()

        val notes =
          files["${prefix}notes.json"]?.let {
            runCatching {
              BookJsonSerializer.notesFromJson(JSONArray(it.toString(StandardCharsets.UTF_8)))
            }.getOrDefault(emptyList())
          } ?: emptyList()

        val dupMatch =
          existingBooks.firstOrNull {
            it.id == book.id ||
              (it.title.equals(book.title, ignoreCase = true) &&
                it.author.equals(book.author, ignoreCase = true))
          }

        parsedEntries.add(
          BackupBookEntry(
            book = book,
            chaptersByLang = chaptersByLang,
            coversByLang = coversByLang,
            bookmarks = bookmarks,
            highlights = highlights,
            notes = notes,
            duplicatesExistingBookId = dupMatch?.id,
          )
        )
      }

      val dupCount = parsedEntries.count { it.duplicatesExistingBookId != null }
      BackupValidationResult.Valid(
        BackupInspection(
          entries = parsedEntries,
          categories = categories,
          settings = settings,
          duplicateCount = dupCount,
        )
      )
    }

  suspend fun restoreBackup(
    repository: BookStorageRepository,
    inspection: BackupInspection,
    duplicateResolution: DuplicateResolution,
    restoreSettings: Boolean,
  ): Int =
    withContext(Dispatchers.IO) {
      inspection.categories.forEach { cat ->
        repository.createCategory(cat.name)
      }
      if (restoreSettings && inspection.settings != null) {
        repository.replaceSettings(inspection.settings)
      }
      var imported = 0
      for (entry in inspection.entries) {
        val dupId = entry.duplicatesExistingBookId
        val targetBook =
          if (dupId != null) {
            when (duplicateResolution) {
              DuplicateResolution.SKIP -> continue
              DuplicateResolution.REPLACE -> entry.book.copy(id = dupId)
              DuplicateResolution.KEEP_BOTH ->
                entry.book.copy(
                  id = "book_${UUID.randomUUID().toString().take(12)}",
                  title = "${entry.book.title} (Copy)",
                )
            }
          } else {
            entry.book
          }
        repository.importBookFromPreparedFolder(
          book = targetBook,
          chaptersByLang = entry.chaptersByLang,
          coverFilesByLang = entry.coversByLang,
          bookmarks = entry.bookmarks,
          highlights = entry.highlights,
          notes = entry.notes,
        )
        imported++
      }
      imported
    }
}
