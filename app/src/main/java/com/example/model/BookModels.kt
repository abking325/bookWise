package com.example.model

enum class BookType(val id: String, val labelEn: String, val labelAm: String) {
  FULL_BOOK("full_book", "Full Book", "ሙሉ መጽሐፍ"),
  SUMMARY("summary", "Book Summary", "የመጽሐፍ ማጠቃለያ");

  companion object {
    fun fromId(id: String?): BookType = entries.firstOrNull { it.id == id } ?: FULL_BOOK
  }
}

enum class ReadingStatus(val id: String, val labelEn: String, val labelAm: String) {
  UNREAD("unread", "Unread", "ያልተነበበ"),
  READING("reading", "Reading", "በማንበብ ላይ"),
  COMPLETED("completed", "Completed", "የተጠናቀቀ");

  companion object {
    fun fromId(id: String?): ReadingStatus = entries.firstOrNull { it.id == id } ?: UNREAD
  }
}

enum class SortOption(val id: String, val labelEn: String, val labelAm: String) {
  RECENTLY_READ("recently_read", "Recently Read", "በቅርቡ የተነበበ"),
  RECENTLY_ADDED("recently_added", "Recently Added", "በቅርቡ የታከለ"),
  TITLE_ASC("title_asc", "Title (A–Z)", "በርዕስ (ሀ–ፐ / A–Z)"),
  AUTHOR_ASC("author_asc", "Author (A–Z)", "በደራሲ");

  companion object {
    fun fromId(id: String?): SortOption = entries.firstOrNull { it.id == id } ?: RECENTLY_READ
  }
}

enum class LanguageCode(val code: String, val displayNameEn: String, val displayNameAm: String) {
  ENGLISH("en", "English", "እንግሊዝኛ"),
  AMHARIC("am", "Amharic", "አማርኛ");

  companion object {
    fun fromCode(code: String?): LanguageCode =
      when (code?.lowercase()?.take(2)) {
        "am" -> AMHARIC
        else -> ENGLISH
      }

    fun labelFor(code: String): String =
      when (code.lowercase().take(2)) {
        "en" -> "English"
        "am" -> "አማርኛ (Amharic)"
        else -> code.uppercase()
      }
  }
}

enum class SectionRole(val id: String, val labelEn: String, val labelAm: String) {
  TITLE_PAGE("title_page", "Title Page", "የርዕስ ገጽ"),
  PREFACE("preface", "Preface", "መቅድም"),
  FOREWORD("foreword", "Foreword", "መንደርደሪያ"),
  INTRODUCTION("introduction", "Introduction", "መግቢያ"),
  TABLE_OF_CONTENTS("table_of_contents", "Table of Contents", "ማውጫ"),
  CHAPTER("chapter", "Chapter", "ምዕራፍ"),
  CONCLUSION("conclusion", "Conclusion", "ማጠቃለያ"),
  APPENDIX("appendix", "Appendix", "አባሪ"),
  REFERENCES("references", "References", "ማጣቀሻዎች");

  companion object {
    fun fromId(id: String?): SectionRole = entries.firstOrNull { it.id == id } ?: CHAPTER
  }
}

enum class HeadingConfidence(val id: String, val labelEn: String) {
  HIGH("high", "High"),
  MEDIUM("medium", "Medium"),
  LOW("low", "Review"),
  MANUAL("manual", "Manual");

  companion object {
    fun fromId(id: String?): HeadingConfidence = entries.firstOrNull { it.id == id } ?: HIGH
  }
}

data class TocEntry(
  val title: String,
  val chapterId: String = "",
  val chapterIndex: Int = 0,
  val pageLabel: String = "",
  val role: SectionRole = SectionRole.CHAPTER,
)

data class ChapterMeta(
  val id: String,
  val index: Int,
  val title: String,
  val fileName: String,
  val wordCount: Int = 0,
  val estimatedMinutes: Int = 1,
  val role: SectionRole = SectionRole.CHAPTER,
  val confidence: HeadingConfidence = HeadingConfidence.HIGH,
  val needsReview: Boolean = false,
)

data class ChapterContent(
  val meta: ChapterMeta,
  val paragraphs: List<String>,
)

data class EditableChapterDraft(
  val id: String,
  val title: String,
  val bodyText: String,
  val role: SectionRole = SectionRole.CHAPTER,
  val confidence: HeadingConfidence = HeadingConfidence.HIGH,
  val needsReview: Boolean = false,
  val detectionNote: String = "",
  val isIncluded: Boolean = true,
)

data class ReadingProgress(
  val chapterIndex: Int = 0,
  val paragraphIndex: Int = 0,
  val scrollOffset: Int = 0,
  val charOffset: Int = 0,
  val chapterId: String = "",
  val paragraphId: String = "",
  val overallProgress: Float = 0f,
  val lastUpdatedAt: Long = 0L,
)

data class AudioProgress(
  val chapterIndex: Int = 0,
  val chapterId: String = "",
  val paragraphIndex: Int = 0,
  val sentenceIndex: Int = 0,
  val charOffset: Int = 0,
  val lastUpdatedAt: Long = 0L,
)

data class BookEdition(
  val languageCode: String,
  val languageLabel: String,
  val coverImagePath: String? = null,
  val bundledCoverKey: String? = null,
  val accentColorHex: String = "#162032",
  val chapters: List<ChapterMeta> = emptyList(),
  val tocEntries: List<TocEntry> = emptyList(),
  val progress: ReadingProgress = ReadingProgress(),
  val audioProgress: AudioProgress = AudioProgress(),
) {
  val totalWords: Int
    get() = chapters.sumOf { it.wordCount }
}

data class Book(
  val id: String,
  val title: String,
  val subtitle: String = "",
  val author: String,
  val category: String,
  val subcategory: String = "",
  val description: String = "",
  val publicationYear: String = "",
  val bookType: BookType = BookType.FULL_BOOK,
  val defaultLanguageCode: String = "en",
  val tags: List<String> = emptyList(),
  val personalNotes: String = "",
  val isFavorite: Boolean = false,
  val readingStatus: ReadingStatus = ReadingStatus.UNREAD,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val lastReadAt: Long = 0L,
  val editions: Map<String, BookEdition> = emptyMap(),
) {
  fun edition(languageCode: String?): BookEdition? {
    if (languageCode != null && editions.containsKey(languageCode)) {
      return editions[languageCode]
    }
    return editions[defaultLanguageCode] ?: editions.values.firstOrNull()
  }

  val availableLanguages: List<String>
    get() = editions.keys.toList()

  val primaryProgress: Float
    get() = edition(defaultLanguageCode)?.progress?.overallProgress ?: 0f
}

data class Bookmark(
  val id: String,
  val bookId: String,
  val languageCode: String,
  val chapterIndex: Int,
  val chapterTitle: String,
  val paragraphIndex: Int,
  val excerpt: String,
  val note: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

data class Highlight(
  val id: String,
  val bookId: String,
  val languageCode: String,
  val chapterIndex: Int,
  val paragraphIndex: Int,
  val text: String,
  val colorTag: String = "gold",
  val note: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

data class BookNote(
  val id: String,
  val bookId: String,
  val languageCode: String,
  val chapterIndex: Int,
  val paragraphIndex: Int,
  val title: String,
  val content: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

data class CategoryItem(
  val id: String,
  val name: String,
  val subcategories: List<String> = emptyList(),
  val isCustom: Boolean = false,
)

data class LibraryFilter(
  val searchQuery: String = "",
  val category: String? = null,
  val languageCode: String? = null,
  val favoritesOnly: Boolean = false,
  val readingStatus: ReadingStatus? = null,
  val sortOption: SortOption = SortOption.RECENTLY_READ,
)

data class BookEditorDraft(
  val editingBookId: String? = null,
  val title: String = "",
  val subtitle: String = "",
  val author: String = "",
  val category: String = "Philosophy",
  val subcategory: String = "",
  val description: String = "",
  val publicationYear: String = "",
  val languageCode: String = "en",
  val bookType: BookType = BookType.FULL_BOOK,
  val tagsText: String = "",
  val personalNotes: String = "",
  val coverImagePath: String? = null,
  val bundledCoverKey: String? = null,
  val chapters: List<EditableChapterDraft> = emptyList(),
  val tocEntries: List<TocEntry> = emptyList(),
  val showTitlePage: Boolean = true,
  val showTableOfContents: Boolean = true,
  val detectionSummary: String = "",
)

enum class DuplicateResolution {
  SKIP,
  REPLACE,
  KEEP_BOTH,
}
