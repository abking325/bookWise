package com.example.data.storage

import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.AudioProgress
import com.example.model.Book
import com.example.model.BookEdition
import com.example.model.BookEditorDraft
import com.example.model.BookNote
import com.example.model.BookType
import com.example.model.Bookmark
import com.example.model.CategoryItem
import com.example.model.ChapterMeta
import com.example.model.EditableChapterDraft
import com.example.model.HeadingConfidence
import com.example.model.Highlight
import com.example.model.LanguageCode
import com.example.model.NavigationStyle
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.model.ReadingProgress
import com.example.model.ReadingStatus
import com.example.model.SectionRole
import com.example.model.TextAlignmentMode
import com.example.model.TocEntry
import org.json.JSONArray
import org.json.JSONObject

object BookJsonSerializer {

  fun bookToJson(book: Book): JSONObject =
    JSONObject().apply {
      put("id", book.id)
      put("title", book.title)
      put("subtitle", book.subtitle)
      put("author", book.author)
      put("category", book.category)
      put("subcategory", book.subcategory)
      put("description", book.description)
      put("publicationYear", book.publicationYear)
      put("bookType", book.bookType.id)
      put("defaultLanguageCode", book.defaultLanguageCode)
      put("tags", JSONArray(book.tags))
      put("personalNotes", book.personalNotes)
      put("isFavorite", book.isFavorite)
      put("readingStatus", book.readingStatus.id)
      put("createdAt", book.createdAt)
      put("updatedAt", book.updatedAt)
      put("lastReadAt", book.lastReadAt)
      val editionsJson = JSONObject()
      book.editions.forEach { (lang, edition) ->
        editionsJson.put(lang, editionToJson(edition))
      }
      put("editions", editionsJson)
    }

  fun bookFromJson(obj: JSONObject): Book {
    val editionsObj = obj.optJSONObject("editions") ?: JSONObject()
    val editions = linkedMapOf<String, BookEdition>()
    val keys = editionsObj.keys()
    while (keys.hasNext()) {
      val lang = keys.next()
      val edJson = editionsObj.optJSONObject(lang) ?: continue
      editions[lang] = editionFromJson(edJson, lang)
    }
    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
    val tags = buildList {
      for (i in 0 until tagsArr.length()) {
        val tag = tagsArr.optString(i).trim()
        if (tag.isNotEmpty()) add(tag)
      }
    }
    return Book(
      id = obj.getString("id"),
      title = obj.optString("title", "Untitled"),
      subtitle = obj.optString("subtitle", ""),
      author = obj.optString("author", "Unknown Author"),
      category = obj.optString("category", "General"),
      subcategory = obj.optString("subcategory", ""),
      description = obj.optString("description", ""),
      publicationYear = obj.optString("publicationYear", ""),
      bookType = BookType.fromId(obj.optString("bookType")),
      defaultLanguageCode = obj.optString("defaultLanguageCode", editions.keys.firstOrNull() ?: "en"),
      tags = tags,
      personalNotes = obj.optString("personalNotes", ""),
      isFavorite = obj.optBoolean("isFavorite", false),
      readingStatus = ReadingStatus.fromId(obj.optString("readingStatus")),
      createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
      updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
      lastReadAt = obj.optLong("lastReadAt", 0L),
      editions = editions,
    )
  }

  fun editionToJson(edition: BookEdition): JSONObject =
    JSONObject().apply {
      put("languageCode", edition.languageCode)
      put("languageLabel", edition.languageLabel)
      put("coverImagePath", edition.coverImagePath ?: JSONObject.NULL)
      put("bundledCoverKey", edition.bundledCoverKey ?: JSONObject.NULL)
      put("accentColorHex", edition.accentColorHex)
      put(
        "chapters",
        JSONArray().apply {
          edition.chapters.forEach { put(chapterMetaToJson(it)) }
        },
      )
      put(
        "tocEntries",
        JSONArray().apply {
          edition.tocEntries.forEach { put(tocEntryToJson(it)) }
        },
      )
      put("progress", progressToJson(edition.progress))
      put("audioProgress", audioProgressToJson(edition.audioProgress))
    }

  fun editionFromJson(obj: JSONObject, fallbackLang: String): BookEdition {
    val langCode = obj.optString("languageCode", fallbackLang)
    val chaptersArr = obj.optJSONArray("chapters") ?: JSONArray()
    val chapters = buildList {
      for (i in 0 until chaptersArr.length()) {
        val chObj = chaptersArr.optJSONObject(i) ?: continue
        add(chapterMetaFromJson(chObj, i))
      }
    }
    val tocArr = obj.optJSONArray("tocEntries") ?: JSONArray()
    val tocEntries = buildList {
      for (i in 0 until tocArr.length()) {
        val tObj = tocArr.optJSONObject(i) ?: continue
        add(tocEntryFromJson(tObj, i))
      }
    }
    val progressObj = obj.optJSONObject("progress")
    val audioObj = obj.optJSONObject("audioProgress")
    val coverPath = obj.optString("coverImagePath", "").takeIf { it.isNotBlank() && it != "null" }
    val bundledKey = obj.optString("bundledCoverKey", "").takeIf { it.isNotBlank() && it != "null" }
    return BookEdition(
      languageCode = langCode,
      languageLabel = obj.optString("languageLabel", LanguageCode.labelFor(langCode)),
      coverImagePath = coverPath,
      bundledCoverKey = bundledKey,
      accentColorHex = obj.optString("accentColorHex", "#162032"),
      chapters = chapters,
      tocEntries = tocEntries,
      progress = if (progressObj != null) progressFromJson(progressObj) else ReadingProgress(),
      audioProgress = if (audioObj != null) audioProgressFromJson(audioObj) else AudioProgress(),
    )
  }

  fun tocEntryToJson(entry: TocEntry): JSONObject =
    JSONObject().apply {
      put("title", entry.title)
      put("chapterId", entry.chapterId)
      put("chapterIndex", entry.chapterIndex)
      put("pageLabel", entry.pageLabel)
      put("role", entry.role.id)
    }

  fun tocEntryFromJson(obj: JSONObject, fallbackIndex: Int): TocEntry =
    TocEntry(
      title = obj.optString("title", "Chapter ${fallbackIndex + 1}"),
      chapterId = obj.optString("chapterId", ""),
      chapterIndex = obj.optInt("chapterIndex", fallbackIndex),
      pageLabel = obj.optString("pageLabel", ""),
      role = SectionRole.fromId(obj.optString("role")),
    )

  fun chapterMetaToJson(meta: ChapterMeta): JSONObject =
    JSONObject().apply {
      put("id", meta.id)
      put("index", meta.index)
      put("title", meta.title)
      put("fileName", meta.fileName)
      put("wordCount", meta.wordCount)
      put("estimatedMinutes", meta.estimatedMinutes)
      put("role", meta.role.id)
      put("confidence", meta.confidence.id)
      put("needsReview", meta.needsReview)
    }

  fun chapterMetaFromJson(obj: JSONObject, fallbackIndex: Int): ChapterMeta =
    ChapterMeta(
      id = obj.optString("id", "ch_$fallbackIndex"),
      index = obj.optInt("index", fallbackIndex),
      title = obj.optString("title", "Chapter ${fallbackIndex + 1}"),
      fileName = obj.optString("fileName", "ch_$fallbackIndex.txt"),
      wordCount = obj.optInt("wordCount", 0),
      estimatedMinutes = obj.optInt("estimatedMinutes", 1),
      role = SectionRole.fromId(obj.optString("role")),
      confidence = HeadingConfidence.fromId(obj.optString("confidence")),
      needsReview = obj.optBoolean("needsReview", false),
    )

  fun progressToJson(progress: ReadingProgress): JSONObject =
    JSONObject().apply {
      put("chapterIndex", progress.chapterIndex)
      put("paragraphIndex", progress.paragraphIndex)
      put("scrollOffset", progress.scrollOffset)
      put("charOffset", progress.charOffset)
      put("chapterId", progress.chapterId)
      put("paragraphId", progress.paragraphId)
      put("overallProgress", progress.overallProgress.toDouble())
      put("lastUpdatedAt", progress.lastUpdatedAt)
    }

  fun progressFromJson(obj: JSONObject): ReadingProgress =
    ReadingProgress(
      chapterIndex = obj.optInt("chapterIndex", 0),
      paragraphIndex = obj.optInt("paragraphIndex", 0),
      scrollOffset = obj.optInt("scrollOffset", 0),
      charOffset = obj.optInt("charOffset", 0),
      chapterId = obj.optString("chapterId", ""),
      paragraphId = obj.optString("paragraphId", ""),
      overallProgress = obj.optDouble("overallProgress", 0.0).toFloat().coerceIn(0f, 1f),
      lastUpdatedAt = obj.optLong("lastUpdatedAt", 0L),
    )

  fun audioProgressToJson(progress: AudioProgress): JSONObject =
    JSONObject().apply {
      put("chapterIndex", progress.chapterIndex)
      put("chapterId", progress.chapterId)
      put("paragraphIndex", progress.paragraphIndex)
      put("sentenceIndex", progress.sentenceIndex)
      put("charOffset", progress.charOffset)
      put("lastUpdatedAt", progress.lastUpdatedAt)
    }

  fun audioProgressFromJson(obj: JSONObject): AudioProgress =
    AudioProgress(
      chapterIndex = obj.optInt("chapterIndex", 0),
      chapterId = obj.optString("chapterId", ""),
      paragraphIndex = obj.optInt("paragraphIndex", 0),
      sentenceIndex = obj.optInt("sentenceIndex", 0),
      charOffset = obj.optInt("charOffset", 0),
      lastUpdatedAt = obj.optLong("lastUpdatedAt", 0L),
    )

  fun bookmarksToJson(items: List<Bookmark>): JSONArray =
    JSONArray().apply {
      items.forEach { b ->
        put(
          JSONObject().apply {
            put("id", b.id)
            put("bookId", b.bookId)
            put("languageCode", b.languageCode)
            put("chapterIndex", b.chapterIndex)
            put("chapterTitle", b.chapterTitle)
            put("paragraphIndex", b.paragraphIndex)
            put("excerpt", b.excerpt)
            put("note", b.note)
            put("createdAt", b.createdAt)
          }
        )
      }
    }

  fun bookmarksFromJson(arr: JSONArray): List<Bookmark> =
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        add(
          Bookmark(
            id = o.getString("id"),
            bookId = o.getString("bookId"),
            languageCode = o.optString("languageCode", "en"),
            chapterIndex = o.optInt("chapterIndex", 0),
            chapterTitle = o.optString("chapterTitle", ""),
            paragraphIndex = o.optInt("paragraphIndex", 0),
            excerpt = o.optString("excerpt", ""),
            note = o.optString("note", ""),
            createdAt = o.optLong("createdAt", 0L),
          )
        )
      }
    }

  fun highlightsToJson(items: List<Highlight>): JSONArray =
    JSONArray().apply {
      items.forEach { h ->
        put(
          JSONObject().apply {
            put("id", h.id)
            put("bookId", h.bookId)
            put("languageCode", h.languageCode)
            put("chapterIndex", h.chapterIndex)
            put("paragraphIndex", h.paragraphIndex)
            put("text", h.text)
            put("colorTag", h.colorTag)
            put("note", h.note)
            put("createdAt", h.createdAt)
          }
        )
      }
    }

  fun highlightsFromJson(arr: JSONArray): List<Highlight> =
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        add(
          Highlight(
            id = o.getString("id"),
            bookId = o.getString("bookId"),
            languageCode = o.optString("languageCode", "en"),
            chapterIndex = o.optInt("chapterIndex", 0),
            paragraphIndex = o.optInt("paragraphIndex", 0),
            text = o.optString("text", ""),
            colorTag = o.optString("colorTag", "gold"),
            note = o.optString("note", ""),
            createdAt = o.optLong("createdAt", 0L),
          )
        )
      }
    }

  fun notesToJson(items: List<BookNote>): JSONArray =
    JSONArray().apply {
      items.forEach { n ->
        put(
          JSONObject().apply {
            put("id", n.id)
            put("bookId", n.bookId)
            put("languageCode", n.languageCode)
            put("chapterIndex", n.chapterIndex)
            put("paragraphIndex", n.paragraphIndex)
            put("title", n.title)
            put("content", n.content)
            put("createdAt", n.createdAt)
            put("updatedAt", n.updatedAt)
          }
        )
      }
    }

  fun notesFromJson(arr: JSONArray): List<BookNote> =
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        add(
          BookNote(
            id = o.getString("id"),
            bookId = o.getString("bookId"),
            languageCode = o.optString("languageCode", "en"),
            chapterIndex = o.optInt("chapterIndex", 0),
            paragraphIndex = o.optInt("paragraphIndex", 0),
            title = o.optString("title", ""),
            content = o.optString("content", ""),
            createdAt = o.optLong("createdAt", 0L),
            updatedAt = o.optLong("updatedAt", 0L),
          )
        )
      }
    }

  fun categoriesToJson(items: List<CategoryItem>): JSONArray =
    JSONArray().apply {
      items.forEach { c ->
        put(
          JSONObject().apply {
            put("id", c.id)
            put("name", c.name)
            put("subcategories", JSONArray(c.subcategories))
            put("isCustom", c.isCustom)
          }
        )
      }
    }

  fun categoriesFromJson(arr: JSONArray): List<CategoryItem> =
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val subs = o.optJSONArray("subcategories") ?: JSONArray()
        val subList = buildList {
          for (j in 0 until subs.length()) {
            val s = subs.optString(j).trim()
            if (s.isNotEmpty()) add(s)
          }
        }
        add(
          CategoryItem(
            id = o.optString("id", "cat_$i"),
            name = o.optString("name", "General"),
            subcategories = subList,
            isCustom = o.optBoolean("isCustom", false),
          )
        )
      }
    }

  fun settingsToJson(s: AppSettings): JSONObject =
    JSONObject().apply {
      put("themeMode", s.themeMode.id)
      put("navigationStyle", s.navigationStyle.id)
      put("carouselDepthEnabled", s.carouselDepthEnabled)
      put("carouselShowSpineThickness", s.carouselShowSpineThickness)
      put("readerFont", s.readerFont.id)
      put("readerFontSizeSp", s.readerFontSizeSp.toDouble())
      put("readerLineHeightMultiplier", s.readerLineHeightMultiplier.toDouble())
      put("readerParagraphSpacingDp", s.readerParagraphSpacingDp.toDouble())
      put("readerMarginDp", s.readerMarginDp.toDouble())
      put("readerTheme", s.readerTheme.id)
      put("readerTextAlignment", s.readerTextAlignment.id)
      put("autoSaveReadingPosition", s.autoSaveReadingPosition)
      put("ttsVoiceName", s.ttsVoiceName)
      put("ttsSpeechRate", s.ttsSpeechRate.toDouble())
      put("ttsPitch", s.ttsPitch.toDouble())
      put("ttsDoubleTapEnabled", s.ttsDoubleTapEnabled)
      put("ttsBackgroundPlaybackEnabled", s.ttsBackgroundPlaybackEnabled)
      put("ttsAutoAdvanceChapter", s.ttsAutoAdvanceChapter)
      put("ttsHighlightSentence", s.ttsHighlightSentence)
      put("appLanguage", s.appLanguage.code)
    }

  fun settingsFromJson(obj: JSONObject): AppSettings =
    AppSettings(
      themeMode = AppThemeMode.fromId(obj.optString("themeMode")),
      navigationStyle = NavigationStyle.fromId(obj.optString("navigationStyle")),
      carouselDepthEnabled = obj.optBoolean("carouselDepthEnabled", true),
      carouselShowSpineThickness = obj.optBoolean("carouselShowSpineThickness", true),
      readerFont = ReaderFont.fromId(obj.optString("readerFont")),
      readerFontSizeSp = obj.optDouble("readerFontSizeSp", 18.0).toFloat().coerceIn(13f, 30f),
      readerLineHeightMultiplier =
        obj.optDouble("readerLineHeightMultiplier", 1.65).toFloat().coerceIn(1.2f, 2.2f),
      readerParagraphSpacingDp =
        obj.optDouble("readerParagraphSpacingDp", 14.0).toFloat().coerceIn(4f, 32f),
      readerMarginDp = obj.optDouble("readerMarginDp", 22.0).toFloat().coerceIn(8f, 44f),
      readerTheme = ReaderTheme.fromId(obj.optString("readerTheme")),
      readerTextAlignment = TextAlignmentMode.fromId(obj.optString("readerTextAlignment")),
      autoSaveReadingPosition = obj.optBoolean("autoSaveReadingPosition", true),
      ttsVoiceName = obj.optString("ttsVoiceName", ""),
      ttsSpeechRate = obj.optDouble("ttsSpeechRate", 1.0).toFloat().coerceIn(0.5f, 2.5f),
      ttsPitch = obj.optDouble("ttsPitch", 1.0).toFloat().coerceIn(0.6f, 1.6f),
      ttsDoubleTapEnabled = obj.optBoolean("ttsDoubleTapEnabled", true),
      ttsBackgroundPlaybackEnabled = obj.optBoolean("ttsBackgroundPlaybackEnabled", true),
      ttsAutoAdvanceChapter = obj.optBoolean("ttsAutoAdvanceChapter", true),
      ttsHighlightSentence = obj.optBoolean("ttsHighlightSentence", true),
      appLanguage = AppLanguage.fromCode(obj.optString("appLanguage")),
    )

  fun draftToJson(draft: BookEditorDraft): JSONObject =
    JSONObject().apply {
      put("editingBookId", draft.editingBookId ?: JSONObject.NULL)
      put("title", draft.title)
      put("subtitle", draft.subtitle)
      put("author", draft.author)
      put("category", draft.category)
      put("subcategory", draft.subcategory)
      put("description", draft.description)
      put("publicationYear", draft.publicationYear)
      put("languageCode", draft.languageCode)
      put("bookType", draft.bookType.id)
      put("tagsText", draft.tagsText)
      put("personalNotes", draft.personalNotes)
      put("coverImagePath", draft.coverImagePath ?: JSONObject.NULL)
      put("bundledCoverKey", draft.bundledCoverKey ?: JSONObject.NULL)
      put("showTitlePage", draft.showTitlePage)
      put("showTableOfContents", draft.showTableOfContents)
      put("detectionSummary", draft.detectionSummary)
      put(
        "tocEntries",
        JSONArray().apply { draft.tocEntries.forEach { put(tocEntryToJson(it)) } },
      )
      put(
        "chapters",
        JSONArray().apply {
          draft.chapters.forEach { ch ->
            put(
              JSONObject().apply {
                put("id", ch.id)
                put("title", ch.title)
                put("bodyText", ch.bodyText)
                put("role", ch.role.id)
                put("confidence", ch.confidence.id)
                put("needsReview", ch.needsReview)
                put("detectionNote", ch.detectionNote)
                put("isIncluded", ch.isIncluded)
              }
            )
          }
        },
      )
    }

  fun draftFromJson(obj: JSONObject): BookEditorDraft {
    val chArr = obj.optJSONArray("chapters") ?: JSONArray()
    val chapters = buildList {
      for (i in 0 until chArr.length()) {
        val c = chArr.optJSONObject(i) ?: continue
        add(
          EditableChapterDraft(
            id = c.optString("id", "draft_ch_$i"),
            title = c.optString("title", "Chapter ${i + 1}"),
            bodyText = c.optString("bodyText", ""),
            role = SectionRole.fromId(c.optString("role")),
            confidence = HeadingConfidence.fromId(c.optString("confidence")),
            needsReview = c.optBoolean("needsReview", false),
            detectionNote = c.optString("detectionNote", ""),
            isIncluded = c.optBoolean("isIncluded", true),
          )
        )
      }
    }
    val tocArr = obj.optJSONArray("tocEntries") ?: JSONArray()
    val tocEntries = buildList {
      for (i in 0 until tocArr.length()) {
        val t = tocArr.optJSONObject(i) ?: continue
        add(tocEntryFromJson(t, i))
      }
    }
    return BookEditorDraft(
      editingBookId = obj.optString("editingBookId", "").takeIf { it.isNotBlank() && it != "null" },
      title = obj.optString("title", ""),
      subtitle = obj.optString("subtitle", ""),
      author = obj.optString("author", ""),
      category = obj.optString("category", "Philosophy"),
      subcategory = obj.optString("subcategory", ""),
      description = obj.optString("description", ""),
      publicationYear = obj.optString("publicationYear", ""),
      languageCode = obj.optString("languageCode", "en"),
      bookType = BookType.fromId(obj.optString("bookType")),
      tagsText = obj.optString("tagsText", ""),
      personalNotes = obj.optString("personalNotes", ""),
      coverImagePath = obj.optString("coverImagePath", "").takeIf { it.isNotBlank() && it != "null" },
      bundledCoverKey = obj.optString("bundledCoverKey", "").takeIf { it.isNotBlank() && it != "null" },
      chapters = chapters,
      tocEntries = tocEntries,
      showTitlePage = obj.optBoolean("showTitlePage", true),
      showTableOfContents = obj.optBoolean("showTableOfContents", true),
      detectionSummary = obj.optString("detectionSummary", ""),
    )
  }
}
