package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.BackupManager
import com.example.data.importer.BackupValidationResult
import com.example.data.importer.BookImporter
import com.example.data.importer.ImportResult
import com.example.data.storage.BookStorageRepository
import com.example.model.AudioProgress
import com.example.model.BookEditorDraft
import com.example.model.BookNote
import com.example.model.BookType
import com.example.model.Bookmark
import com.example.model.DuplicateResolution
import com.example.model.EditableChapterDraft
import com.example.model.HeadingConfidence
import com.example.model.Highlight
import com.example.model.ReadingProgress
import com.example.model.SectionRole
import com.example.tts.OfflineTtsManager
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun `fresh install seeds starter books and persists bilingual editions and exact progress across restart`() = runTest {
    val repo1 = BookStorageRepository(context)
    repo1.initialize()

    assertTrue(repo1.books.value.size >= 4)
    val bilingual = repo1.books.value.firstOrNull { it.editions.containsKey("am") && it.editions.containsKey("en") }
    assertNotNull(bilingual)

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

    // Save reading progress with scrollOffset + chapterId + audio progress
    repo1.updateReadingProgress(
      savedBoth.id,
      "en",
      ReadingProgress(
        chapterIndex = 1,
        chapterId = "ch_2",
        paragraphIndex = 0,
        paragraphId = "ch_2_p_0",
        scrollOffset = 148,
        overallProgress = 0.75f,
        lastUpdatedAt = 12345L,
      ),
    )
    repo1.updateAudioProgress(
      savedBoth.id,
      "en",
      AudioProgress(
        chapterIndex = 1,
        chapterId = "ch_2",
        paragraphIndex = 0,
        sentenceIndex = 1,
        charOffset = 22,
        lastUpdatedAt = 12999L,
      ),
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
    val reloadedProg = reloaded.editions["en"]!!.progress
    assertEquals(1, reloadedProg.chapterIndex)
    assertEquals(148, reloadedProg.scrollOffset)
    assertEquals(0.75f, reloadedProg.overallProgress, 0.01f)
    val reloadedAudio = reloaded.editions["en"]!!.audioProgress
    assertEquals(1, reloadedAudio.sentenceIndex)
    assertEquals(22, reloadedAudio.charOffset)

    // Reset reading progress works cleanly
    repo2.resetReadingProgress(savedBoth.id, "en")
    val afterReset = repo2.books.value.first { it.id == savedBoth.id }.editions["en"]!!.progress
    assertEquals(0, afterReset.chapterIndex)
    assertEquals(0, afterReset.paragraphIndex)
    assertEquals(0, afterReset.scrollOffset)
    assertEquals(0f, afterReset.overallProgress, 0.001f)

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
  fun `multi-stage parser handles toc dot-leaders roles title-case and preserves unheaded books completely`() = runTest {
    val structuredText =
      """
      Title: Habits of Quiet Focus
      Author: Elena Brooks
      Year: 2024

      Table of Contents
      Preface ........ 1
      Recognizing Your Weaknesses ........ 4
      THE POWER OF DISCIPLINE ........ 12
      Conclusion ........ 20

      Preface
      This is the opening preface explaining why focus matters.

      Recognizing Your Weaknesses
      Every sentence ending with a period stays inside the paragraph.
      Even this ordinary line. Must never create a false chapter split!

      THE POWER OF DISCIPLINE
      Consistency compounds over quiet seasons without noise.

      Conclusion
      Walk forward gently and consistently.
      """.trimIndent()

    val parsed = BookImporter.parsePlainTextToDraft("fallback", structuredText)
    assertEquals("Habits of Quiet Focus", parsed.title)
    assertEquals("Elena Brooks", parsed.author)
    assertEquals("2024", parsed.publicationYear)
    assertEquals(4, parsed.chapters.size)
    assertEquals(SectionRole.PREFACE, parsed.chapters[0].role)
    assertEquals("Recognizing Your Weaknesses", parsed.chapters[1].title)
    assertTrue(parsed.chapters[1].bodyText.contains("Even this ordinary line."))
    assertEquals("THE POWER OF DISCIPLINE", parsed.chapters[2].title)
    assertEquals(SectionRole.CONCLUSION, parsed.chapters[3].role)

    // Unheaded raw prose must NOT fabricate fake sections or drop a single character
    val unheaded =
      "First sentence of a continuous essay that contains no explicit chapter markers at all. " +
        "Second sentence continues the thought naturally across several lines without headings."
    val unheadedDraft = BookImporter.parsePlainTextToDraft("Continuous Essay", unheaded)
    assertEquals(1, unheadedDraft.chapters.size)
    assertEquals(HeadingConfidence.MANUAL, unheadedDraft.chapters.first().confidence)
    assertTrue(unheadedDraft.chapters.first().bodyText.contains("Second sentence continues"))
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
  fun `canonical paragraph formatter repairs hyphen wraps and splits unformatted long blocks on sentence boundaries`() {
    val repo = BookStorageRepository(context)
    val wrappedAndWall =
      "This is a hyphen-\nated word that continues smoothly.\n\n" +
        List(18) { "Each thoughtful sentence builds steady discipline over quiet days." }
          .joinToString(" ")
    val paras = repo.splitIntoParagraphs(wrappedAndWall)
    assertTrue(paras.first().contains("hyphenated word"))
    assertTrue(paras.size >= 3)
    assertTrue(paras.all { it.length <= 760 })

    // Amharic wall split on Ethiopic sentence marker ።
    val amWall =
      List(22) { "ትዕግሥትና ትጋት ለዘላቂ ዕድገት መሠረት ናቸው።" }.joinToString(" ")
    val amParas = repo.splitIntoParagraphs(amWall)
    assertTrue(amParas.size >= 2)
  }

  @Test
  fun `tts sentence spans and initial hidden player state behave reliably`() {
    val repo = BookStorageRepository(context)
    val ttsManager = OfflineTtsManager(context, repo)
    assertFalse(ttsManager.state.value.isPlayerVisible)

    val para = "First sentence is calm. Second sentence builds clarity! Does the third ask?"
    val spans = OfflineTtsManager.splitParagraphIntoSentenceSpans(para)
    assertEquals(3, spans.size)
    assertEquals("First sentence is calm.", spans[0].text)
    assertEquals("Second sentence builds clarity!", spans[1].text)
    assertEquals("Does the third ask?", spans[2].text)
    assertEquals(0, spans[0].startChar)
    assertTrue(spans[1].startChar > spans[0].endChar)
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

    val restored = BackupManager.restoreBackup(repo, inspection, DuplicateResolution.KEEP_BOTH, false)
    assertEquals(initialCount, restored)
    assertEquals(initialCount * 2, repo.books.value.size)
  }
}
