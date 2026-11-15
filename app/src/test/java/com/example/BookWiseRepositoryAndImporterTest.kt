package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.BackupManager
import com.example.data.importer.BackupValidationResult
import com.example.data.importer.BookImporter
import com.example.data.importer.ImportResult
import com.example.data.storage.BookStorageRepository
import com.example.model.BookEditorDraft
import com.example.model.BookNote
import com.example.model.BookType
import com.example.model.Bookmark
import com.example.model.DuplicateResolution
import com.example.model.EditableChapterDraft
import com.example.model.Highlight
import com.example.model.ReadingProgress
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookWiseRepositoryAndImporterTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    File(context.filesDir, "bookwise_library").deleteRecursively()
  }

  @Test
  fun `fresh install seeds starter books and persists bilingual editions across restart`() = runTest {
    val repo1 = BookStorageRepository(context)
    repo1.initialize()

    assertTrue(repo1.books.value.size >= 4)
    val bilingual = repo1.books.value.firstOrNull { it.editions.containsKey("am") && it.editions.containsKey("en") }
    assertNotNull(bilingual)

    // Create a custom book with English edition, then link Amharic edition
    val enDraft =
      BookEditorDraft(
        title = "Notebook on Patience",
        author = "Test Author",
        category = "Philosophy",
        languageCode = "en",
        bookType = BookType.FULL_BOOK,
        chapters =
          listOf(
            EditableChapterDraft("c1", "Chapter 1: Stillness", "First paragraph.\n\nSecond paragraph."),
            EditableChapterDraft("c2", "Chapter 2: Clarity", "Third paragraph with clarity."),
          ),
      )
    val savedEn = repo1.saveBookDraftEdition(enDraft)

    val amDraft =
      enDraft.copy(
        editingBookId = savedEn.id,
        languageCode = "am",
        chapters =
          listOf(
            EditableChapterDraft("am1", "ምዕራፍ ፩", "ትዕግሥት መልካም ጥበብ ነው።")
          ),
      )
    val savedBoth = repo1.saveBookDraftEdition(amDraft)
    assertEquals(2, savedBoth.editions.size)

    // Save reading progress, bookmark, highlight, and note
    repo1.updateReadingProgress(
      savedBoth.id,
      "en",
      ReadingProgress(chapterIndex = 1, paragraphIndex = 0, overallProgress = 0.75f, lastUpdatedAt = 12345L),
    )
    repo1.addBookmark(Bookmark("bm1", savedBoth.id, "en", 1, "Chapter 2: Clarity", 0, "Third paragraph"))
    repo1.addHighlight(Highlight("hl1", savedBoth.id, "en", 0, 1, "Second paragraph."))
    repo1.saveNote(BookNote("n1", savedBoth.id, "en", 0, 0, "Reflection", "Remember stillness."))

    // Simulate app restart with new repository instance reading from local folders
    val repo2 = BookStorageRepository(context)
    repo2.initialize()
    val reloaded = repo2.books.value.firstOrNull { it.id == savedBoth.id }
    assertNotNull(reloaded)
    assertEquals(2, reloaded!!.editions.size)
    assertEquals(0.75f, reloaded.editions["en"]!!.progress.overallProgress, 0.01f)

    val chContent = repo2.loadChapterContent(savedBoth.id, "am", 0)
    assertNotNull(chContent)
    assertTrue(chContent!!.paragraphs.first().contains("ትዕግሥት"))
    assertEquals(1, repo2.getBookmarks(savedBoth.id).size)
    assertEquals(1, repo2.getHighlights(savedBoth.id).size)
    assertEquals(1, repo2.getNotes(savedBoth.id).size)

    val searchHits = repo2.searchInBook(savedBoth.id, "en", "clarity")
    assertEquals(1, searchHits.size)
    assertEquals(1, searchHits.first().chapterIndex)
  }

  @Test
  fun `category creation rename and deletion update books cleanly`() = runTest {
    val repo = BookStorageRepository(context)
    repo.initialize()

    repo.createCategory("Poetry", "Sonnets")
    val created = repo.categories.value.first { it.name == "Poetry" }
    val saved =
      repo.saveBookDraftEdition(
        BookEditorDraft(
          title = "Collected Verse",
          author = "Poet",
          category = "Poetry",
          chapters = listOf(EditableChapterDraft("c1", "Poem 1", "Line one.")),
        )
      )

    repo.renameCategory(created.id, "Lyric Poetry")
    assertEquals("Lyric Poetry", repo.books.value.first { it.id == saved.id }.category)

    repo.deleteCategory(created.id)
    assertEquals("Uncategorized", repo.books.value.first { it.id == saved.id }.category)
  }

  @Test
  fun `plain text and trebedit html migration preserve chapters without truncation`() = runTest {
    val text =
      """
      Title: Desert Wisdom
      Author: Abba Anthony

      Chapter 1: Solitude
      First line of chapter one.

      Chapter 2: Humility
      First line of chapter two.
      """.trimIndent()
    val draft = BookImporter.parsePlainTextToDraft("fallback", text)
    assertEquals("Desert Wisdom", draft.title)
    assertEquals("Abba Anthony", draft.author)
    assertEquals(2, draft.chapters.size)

    val htmlFile =
      File(context.cacheDir, "bookwise_trebedit.html").apply {
        writeText(
          """
          <html><body>
          <script>
            const books = [{
              "title": "TrebEdit Classic",
              "author": "Original Author",
              "category": "Literature & Heritage",
              "editions": {
                "en": { "chapters": [{ "title": "Ch 1", "content": "Preserved full sentence." }] },
                "am": { "chapters": [{ "title": "ምዕራፍ ፩", "content": "ሙሉ ጽሑፍ።" }] }
              }
            }];
          </script>
          </body></html>
          """.trimIndent()
        )
      }
    val res = BookImporter.importFromUri(context, Uri.fromFile(htmlFile))
    assertTrue(res is ImportResult.MultiBookMigration)
    val bundle = (res as ImportResult.MultiBookMigration).books.first()
    assertEquals("TrebEdit Classic", bundle.book.title)
    assertEquals("Preserved full sentence.", bundle.chaptersByLang["en"]!!.first().bodyText)
    assertEquals("ሙሉ ጽሑፍ።", bundle.chaptersByLang["am"]!!.first().bodyText)
  }

  @Test
  fun `backup zip export and restore handle duplicates properly`() = runTest {
    val repo = BookStorageRepository(context)
    repo.initialize()
    val initialCount = repo.books.value.size

    val backupFile = File(context.cacheDir, "test_backup.zip")
    val exportRes = BackupManager.exportLibraryZip(context, repo, Uri.fromFile(backupFile))
    assertTrue(exportRes.isSuccess)
    assertTrue(backupFile.length() > 100L)

    val validation = BackupManager.inspectBackupUri(context, repo, Uri.fromFile(backupFile))
    assertTrue(validation is BackupValidationResult.Valid)
    val inspection = (validation as BackupValidationResult.Valid).inspection
    assertEquals(initialCount, inspection.entries.size)
    assertEquals(initialCount, inspection.duplicateCount)

    // Keep both duplicates
    val restored = BackupManager.restoreBackup(repo, inspection, DuplicateResolution.KEEP_BOTH, false)
    assertEquals(initialCount, restored)
    assertEquals(initialCount * 2, repo.books.value.size)
  }
}
