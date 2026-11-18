package com.example.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.Book
import com.example.model.BookEdition
import com.example.model.BookEditorDraft
import com.example.model.BookType
import com.example.model.EditableChapterDraft
import com.example.model.HeadingConfidence
import com.example.model.LanguageCode
import com.example.model.ReadingProgress
import com.example.model.ReadingStatus
import com.example.model.SectionRole
import com.example.model.TocEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

sealed interface ImportResult {
  data class DraftReady(val draft: BookEditorDraft, val notice: String? = null) : ImportResult

  data class MultiBookMigration(val books: List<MigratedBookBundle>, val sourceLabel: String) :
    ImportResult

  data class ScannedPdfDetected(
    val suggestedTitle: String,
    val explanation: String,
  ) : ImportResult

  data class Error(val message: String) : ImportResult
}

data class MigratedBookBundle(
  val book: Book,
  val chaptersByLang: Map<String, List<EditableChapterDraft>>,
  val coverBytesByLang: Map<String, ByteArray>,
)

object BookImporter {

  private val minorTitleWords =
    setOf(
      "a", "an", "the", "and", "or", "but", "for", "nor", "on", "at", "to", "from", "by", "of",
      "in", "with", "into", "onto", "upon", "as", "vs", "via", "your", "our", "my", "is", "are",
    )

  private val wordNumberMap =
    mapOf(
      "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
      "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
      "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
      "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
    )

  suspend fun importFromUri(context: Context, uri: Uri): ImportResult =
    withContext(Dispatchers.IO) {
      val name = resolveFileName(context, uri) ?: "imported_book"
      val ext = name.substringAfterLast('.', "").lowercase()
      val bytes =
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext ImportResult.Error("Could not read selected file.")

      when (ext) {
        "txt", "md", "text" -> {
          val text = bytes.toString(StandardCharsets.UTF_8)
          val draft = parsePlainTextToDraft(name.substringBeforeLast('.'), text)
          ImportResult.DraftReady(draft, draft.detectionSummary)
        }
        "epub" -> parseEpub(context, name.substringBeforeLast('.'), bytes)
        "pdf" -> parsePdf(name.substringBeforeLast('.'), bytes)
        "json" -> parseBookWiseJson(name, bytes.toString(StandardCharsets.UTF_8))
        "html", "htm", "js" -> parseTrebEditBookWise(name, bytes.toString(StandardCharsets.UTF_8))
        else -> {
          val preview = bytes.take(512).toByteArray().toString(StandardCharsets.UTF_8)
          if (preview.startsWith("%PDF")) {
            parsePdf(name.substringBeforeLast('.'), bytes)
          } else if (preview.contains("{") || preview.contains("[")) {
            parseBookWiseJson(name, bytes.toString(StandardCharsets.UTF_8))
          } else {
            val draft =
              parsePlainTextToDraft(
                name.substringBeforeLast('.'),
                bytes.toString(StandardCharsets.UTF_8),
              )
            ImportResult.DraftReady(draft, draft.detectionSummary)
          }
        }
      }
    }

  private data class HeadingMatch(
    val title: String,
    val role: SectionRole,
    val confidence: HeadingConfidence,
    val note: String,
  )

  /**
   * Multi-stage confidence-scored offline book parser.
   * Never deletes, truncates, summarizes, or invents split chapters when headings are absent.
   */
  fun parsePlainTextToDraft(titleHint: String, rawText: String): BookEditorDraft {
    val clean = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
    val detectedLanguage = detectEthiopicOrEnglish(clean)
    val lines = clean.lines()

    var extractedTitle =
      titleHint.replace('_', ' ').replace('-', ' ').trim().ifBlank { "Untitled Book" }
    var extractedAuthor = ""
    var extractedYear = ""
    var bodyStartIndex = 0

    // Stage 1: Front-matter metadata extraction from top lines without discarding body text
    val topScanCount = minOf(lines.size, 18)
    for (idx in 0 until topScanCount) {
      val trimmed = lines[idx].trim()
      if (trimmed.isEmpty()) continue
      when {
        trimmed.startsWith("Title:", ignoreCase = true) -> {
          extractedTitle = trimmed.substringAfter(":").trim().ifBlank { extractedTitle }
          bodyStartIndex = idx + 1
        }
        trimmed.startsWith("Author:", ignoreCase = true) -> {
          extractedAuthor = trimmed.substringAfter(":").trim()
          bodyStartIndex = idx + 1
        }
        trimmed.startsWith("By ", ignoreCase = false) && trimmed.length < 70 && idx <= 5 -> {
          extractedAuthor = trimmed.removePrefix("By ").trim()
          bodyStartIndex = idx + 1
        }
        trimmed.startsWith("Year:", ignoreCase = true) -> {
          extractedYear = trimmed.substringAfter(":").trim()
          bodyStartIndex = idx + 1
        }
      }
    }

    val workingLines = lines.drop(bodyStartIndex)

    // Stage 2: Detect separate Table of Contents block (strip dot leaders / page numbers without creating empty stub chapters)
    val parsedTocTitles = mutableListOf<String>()
    val filteredLines = mutableListOf<String>()
    var i = 0
    while (i < workingLines.size) {
      val trimmed = workingLines[i].trim()
      if (isTocHeader(trimmed)) {
        // Scan following TOC lines (with dot leaders, trailing digits, or short clustered list)
        var j = i + 1
        val candidateEntries = mutableListOf<String>()
        while (j < workingLines.size && j - i <= 80) {
          val lineTrim = workingLines[j].trim()
          if (lineTrim.isEmpty()) {
            if (candidateEntries.isNotEmpty() && j + 1 < workingLines.size && !looksLikeTocEntry(workingLines[j + 1].trim())) {
              break
            }
            j++
            continue
          }
          if (looksLikeTocEntry(lineTrim)) {
            candidateEntries.add(cleanTocEntryTitle(lineTrim))
            j++
          } else {
            break
          }
        }
        if (candidateEntries.size >= 2) {
          parsedTocTitles.addAll(candidateEntries.filter { it.isNotBlank() })
          i = j
          continue
        }
      }
      filteredLines.add(workingLines[i])
      i++
    }

    // Stage 3: Multi-signal structural heading scan across remaining lines
    val chapters = mutableListOf<EditableChapterDraft>()
    var currentMatch: HeadingMatch? = null
    val currentBody = StringBuilder()

    for (idx in filteredLines.indices) {
      val line = filteredLines[idx]
      val trimmed = line.trim()
      val prevBlankOrStart = idx == 0 || filteredLines[idx - 1].isBlank()
      val nextBlankOrEnd = idx == filteredLines.lastIndex || filteredLines[idx + 1].isBlank()

      val match =
        classifyHeadingCandidate(
          line = trimmed,
          prevBlankOrStart = prevBlankOrStart,
          nextBlankOrEnd = nextBlankOrEnd,
          tocTitles = parsedTocTitles,
        )

      if (match != null) {
        if (currentMatch != null || currentBody.isNotBlank()) {
          val body = currentBody.toString().trim()
          if (currentMatch != null || body.isNotEmpty()) {
            val m =
              currentMatch
                ?: HeadingMatch(
                  title = if (detectedLanguage == "am") "መግቢያ" else "Introduction",
                  role = SectionRole.INTRODUCTION,
                  confidence = HeadingConfidence.HIGH,
                  note = "Opening text before first detected heading",
                )
            chapters.add(
              EditableChapterDraft(
                id = "ch_${chapters.size + 1}",
                title = m.title,
                bodyText = body,
                role = m.role,
                confidence = m.confidence,
                needsReview = m.confidence == HeadingConfidence.LOW,
                detectionNote = m.note,
              )
            )
          }
          currentBody.clear()
        }
        currentMatch = match
      } else {
        currentBody.appendLine(line)
      }
    }

    if (currentMatch != null || currentBody.isNotBlank()) {
      val body = currentBody.toString().trim()
      val m =
        currentMatch
          ?: HeadingMatch(
            title = if (detectedLanguage == "am") "ምዕራፍ ፩" else "Chapter 1",
            role = SectionRole.CHAPTER,
            confidence = HeadingConfidence.MANUAL,
            note = "No explicit chapter headings found; preserved complete untouched text.",
          )
      chapters.add(
        EditableChapterDraft(
          id = "ch_${chapters.size + 1}",
          title = m.title,
          bodyText = body,
          role = m.role,
          confidence = m.confidence,
          needsReview = m.confidence == HeadingConfidence.LOW || m.confidence == HeadingConfidence.MANUAL,
          detectionNote = m.note,
        )
      )
    }

    // Merge consecutive empty-body heading artifacts (e.g., "PART ONE" followed immediately by "Chapter 1")
    val mergedChapters = mergeEmptyHeadingStubs(chapters)

    val finalChapters =
      if (mergedChapters.isEmpty()) {
        listOf(
          EditableChapterDraft(
            id = "ch_1",
            title = if (detectedLanguage == "am") "ምዕራፍ ፩" else "Chapter 1",
            bodyText = clean,
            role = SectionRole.CHAPTER,
            confidence = HeadingConfidence.MANUAL,
            needsReview = true,
            detectionNote = "No headings detected; complete text preserved untouched.",
          )
        )
      } else {
        mergedChapters
      }

    val tocEntries =
      finalChapters.mapIndexed { idx, ch ->
        TocEntry(title = ch.title, chapterId = ch.id, chapterIndex = idx, role = ch.role)
      }

    val reviewCount = finalChapters.count { it.needsReview }
    val summary =
      when {
        finalChapters.size == 1 && finalChapters.first().confidence == HeadingConfidence.MANUAL ->
          "No chapter headings detected. Preserved 100% of original text in 1 section — you can split chapters manually."
        reviewCount > 0 ->
          "Detected ${finalChapters.size} section(s) ($reviewCount flagged for review)."
        else ->
          "Detected ${finalChapters.size} section(s) cleanly."
      }

    return BookEditorDraft(
      title = extractedTitle,
      author = extractedAuthor,
      publicationYear = extractedYear,
      languageCode = detectedLanguage,
      chapters = finalChapters,
      tocEntries = tocEntries,
      detectionSummary = summary,
    )
  }

  private fun mergeEmptyHeadingStubs(
    raw: List<EditableChapterDraft>,
  ): List<EditableChapterDraft> {
    if (raw.size <= 1) return raw
    val out = mutableListOf<EditableChapterDraft>()
    var idx = 0
    while (idx < raw.size) {
      val cur = raw[idx]
      if (cur.bodyText.isBlank() && idx + 1 < raw.size) {
        val next = raw[idx + 1]
        out.add(
          next.copy(
            title = "${cur.title} — ${next.title}",
            detectionNote = "Combined section header + chapter heading",
          )
        )
        idx += 2
      } else {
        out.add(cur)
        idx++
      }
    }
    return out.mapIndexed { i, ch -> ch.copy(id = "ch_${i + 1}") }
  }

  private fun isTocHeader(line: String): Boolean {
    val clean = line.removeSuffix(":").trim().lowercase()
    return clean == "table of contents" ||
      clean == "contents" ||
      clean == "toc" ||
      clean == "ማውጫ" ||
      clean == "የመጽሐፉ ማውጫ"
  }

  private fun looksLikeTocEntry(line: String): Boolean {
    if (line.length > 110) return false
    if (Regex(""".*\.{3,}\s*\d*\s*$""").matches(line)) return true
    if (Regex("""^.{3,75}\s{2,}\d{1,4}\s*$""").matches(line)) return true
    if (Regex("""^(?:chapter|part|ምዕራፍ|\d+\.)\s+.{2,65}\s+\d{1,4}\s*$""", RegexOption.IGNORE_CASE).matches(line)) {
      return true
    }
    return false
  }

  private fun cleanTocEntryTitle(line: String): String {
    return line
      .replace(Regex("""\.{2,}\s*\d*\s*$"""), "")
      .replace(Regex("""\s+\d{1,4}\s*$"""), "")
      .trim()
  }

  private fun classifyHeadingCandidate(
    line: String,
    prevBlankOrStart: Boolean,
    nextBlankOrEnd: Boolean,
    tocTitles: List<String>,
  ): HeadingMatch? {
    if (line.isBlank()) return null
    if (line.length > 105) return null
    if (looksLikeTocEntry(line)) return null

    // Stage A: Negative sentence filter
    val words = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size > 14) return null
    if (line.endsWith(",") || line.endsWith(";") || line.endsWith("—") || line.endsWith("-")) return null

    // Markdown explicit heading
    if (Regex("""^#{1,3}\s+.+$""").matches(line)) {
      val cleanTitle = line.removePrefix("###").removePrefix("##").removePrefix("#").trim()
      return HeadingMatch(
        title = cleanTitle,
        role = inferRoleFromTitle(cleanTitle),
        confidence = HeadingConfidence.HIGH,
        note = "Markdown heading",
      )
    }

    // Explicit English chapter/part/book/section (numbers, word-numbers, or Roman numerals)
    val wordNumPattern = wordNumberMap.keys.joinToString("|")
    val explicitEn =
      Regex(
        """^(chapter|part|book|section|unit|lesson|act)\s+([0-9]+|[ivxlcdm]+|$wordNumPattern)\b(?:[\s:.\-—–]*(.*))?$""",
        RegexOption.IGNORE_CASE,
      )
    explicitEn.find(line)?.let { m ->
      val keyword = m.groupValues[1].lowercase()
      val role =
        when (keyword) {
          else -> SectionRole.CHAPTER
        }
      return HeadingMatch(
        title = line.trimEnd('.'),
        role = role,
        confidence = HeadingConfidence.HIGH,
        note = "Explicit $keyword heading",
      )
    }

    // Explicit Amharic chapter / part headings (ምዕራፍ ፩ / ምዕራፍ 1 / ክፍል ፪)
    val explicitAm = Regex("""^(ምዕራፍ|ክፍል)\s+[0-9፩-፼]+\b.*$""")
    if (explicitAm.matches(line)) {
      return HeadingMatch(
        title = line,
        role = SectionRole.CHAPTER,
        confidence = HeadingConfidence.HIGH,
        note = "Amharic chapter marker",
      )
    }

    // Explicit structural front/back matter
    val structuralRole = matchExactFrontBackMatter(line)
    if (structuralRole != null && (prevBlankOrStart || nextBlankOrEnd)) {
      return HeadingMatch(
        title = line.removeSuffix(":").trim(),
        role = structuralRole,
        confidence = HeadingConfidence.HIGH,
        note = "Structural ${structuralRole.labelEn}",
      )
    }

    // Reject ordinary narrative sentences ending in punctuation after checking explicit Chapter markers
    if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?") || line.endsWith("።")) {
      return null
    }
    if (line.firstOrNull()?.isLowerCase() == true) return null

    // Matches parsed TOC title directly
    if (tocTitles.any { it.equals(line, ignoreCase = true) }) {
      return HeadingMatch(
        title = line,
        role = inferRoleFromTitle(line),
        confidence = HeadingConfidence.HIGH,
        note = "Matched Table of Contents entry",
      )
    }

    // Roman numeral standalone heading: "I. Foundations" or "IV — Discipline" or "III"
    val romanMatch = Regex("""^[IVXLCDM]{1,6}(?:\s*[.:—–-]\s*.+)?$""")
    if (romanMatch.matches(line) && prevBlankOrStart && nextBlankOrEnd) {
      return HeadingMatch(
        title = line,
        role = SectionRole.CHAPTER,
        confidence = HeadingConfidence.MEDIUM,
        note = "Roman numeral section",
      )
    }

    // Numbered chapter heading: "1. The Power of Habits" or "2 - Focus"
    val numberedMatch = Regex("""^\d{1,3}\s*[.)—–-]\s+[A-Z\u1200-\u135A].{2,80}$""")
    if (numberedMatch.matches(line) && prevBlankOrStart) {
      return HeadingMatch(
        title = line,
        role = SectionRole.CHAPTER,
        confidence = HeadingConfidence.MEDIUM,
        note = "Numbered heading",
      )
    }

    // Isolated standalone ALL-CAPS title ("THE POWER OF DISCIPLINE")
    val lettersOnly = line.filter { it.isLetter() }
    if (
      lettersOnly.length >= 4 &&
        lettersOnly.all { it.isUpperCase() } &&
        words.size in 1..10 &&
        prevBlankOrStart &&
        nextBlankOrEnd
    ) {
      return HeadingMatch(
        title = line,
        role = inferRoleFromTitle(line),
        confidence = HeadingConfidence.MEDIUM,
        note = "Standalone uppercase heading",
      )
    }

    // Isolated Title-Case heading ("Recognizing Your Weaknesses", "Building Strong Habits")
    if (prevBlankOrStart && nextBlankOrEnd && words.size in 2..9 && isTitleCaseHeading(words)) {
      return HeadingMatch(
        title = line,
        role = inferRoleFromTitle(line),
        confidence = HeadingConfidence.LOW,
        note = "Standalone Title-Case heading (verify in Structure step)",
      )
    }

    return null
  }

  private fun isTitleCaseHeading(words: List<String>): Boolean {
    if (words.isEmpty()) return false
    var contentWords = 0
    var capitalizedContentWords = 0
    words.forEachIndexed { idx, w ->
      val cleanWord = w.trim('"', '“', '”', '\'', ':', '(', ')')
      if (cleanWord.isEmpty()) return@forEachIndexed
      val lower = cleanWord.lowercase()
      val isMinor = idx != 0 && lower in minorTitleWords
      if (!isMinor) {
        contentWords++
        val first = cleanWord.firstOrNull()
        if (first != null && (first.isUpperCase() || first in '\u1200'..'\u135A')) {
          capitalizedContentWords++
        }
      }
    }
    return contentWords >= 2 && capitalizedContentWords == contentWords
  }

  private fun matchExactFrontBackMatter(line: String): SectionRole? {
    val clean = line.removeSuffix(":").trim().lowercase()
    return when {
      clean == "title page" -> SectionRole.TITLE_PAGE
      clean == "preface" || clean == "መቅድም" -> SectionRole.PREFACE
      clean == "foreword" || clean == "መንደርደሪያ" -> SectionRole.FOREWORD
      clean == "introduction" || clean == "prologue" || clean == "መግቢያ" -> SectionRole.INTRODUCTION
      clean == "conclusion" || clean == "epilogue" || clean == "summary" || clean == "ማጠቃለያ" ->
        SectionRole.CONCLUSION
      clean == "appendix" || clean.startsWith("appendix ") || clean == "አባሪ" -> SectionRole.APPENDIX
      clean == "references" || clean == "bibliography" || clean == "notes" || clean == "ማጣቀሻዎች" ->
        SectionRole.REFERENCES
      else -> null
    }
  }

  private fun inferRoleFromTitle(title: String): SectionRole {
    val lower = title.trim().lowercase()
    return when {
      lower.contains("preface") || lower.contains("መቅድም") -> SectionRole.PREFACE
      lower.contains("foreword") || lower.contains("መንደርደሪያ") -> SectionRole.FOREWORD
      lower.contains("introduction") || lower.contains("prologue") || lower.contains("መግቢያ") ->
        SectionRole.INTRODUCTION
      lower.contains("conclusion") || lower.contains("epilogue") || lower.contains("ማጠቃለያ") ->
        SectionRole.CONCLUSION
      lower.contains("appendix") || lower.contains("አባሪ") -> SectionRole.APPENDIX
      lower.contains("reference") || lower.contains("bibliography") || lower.contains("ማጣቀሻ") ->
        SectionRole.REFERENCES
      else -> SectionRole.CHAPTER
    }
  }

  private fun parseEpub(context: Context, fallbackTitle: String, bytes: ByteArray): ImportResult {
    val entries = linkedMapOf<String, ByteArray>()
    runCatching {
      ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) {
            entries[entry.name] = zis.readBytes()
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
    }.onFailure {
      return ImportResult.Error("Invalid EPUB archive: ${it.message}")
    }

    val containerXml =
      entries["META-INF/container.xml"]?.toString(StandardCharsets.UTF_8)
        ?: return ImportResult.Error("EPUB missing META-INF/container.xml.")
    val opfPath =
      Regex("""full-path=["']([^"']+)["']""").find(containerXml)?.groupValues?.get(1)
        ?: entries.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
        ?: return ImportResult.Error("EPUB missing OPF package file.")

    val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
    val opfXml =
      entries[opfPath]?.toString(StandardCharsets.UTF_8)
        ?: return ImportResult.Error("Could not read OPF file inside EPUB.")

    var title = fallbackTitle
    var author = ""
    var description = ""
    var language = "en"
    val manifest = mutableMapOf<String, Pair<String, String>>() // id -> (href, mediaType)
    val spineIds = mutableListOf<String>()
    var coverItemId: String? = null
    var ncxHref: String? = null
    var navHref: String? = null

    runCatching {
      val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
      val parser = factory.newPullParser()
      parser.setInput(opfXml.reader())
      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
          when (parser.name.lowercase()) {
            "title" -> {
              val t = parser.nextText().trim()
              if (t.isNotEmpty()) title = t
            }
            "creator" -> {
              val a = parser.nextText().trim()
              if (a.isNotEmpty() && author.isEmpty()) author = a
            }
            "description" -> {
              val d = stripHtml(parser.nextText()).trim()
              if (d.isNotEmpty()) description = d
            }
            "language" -> {
              val l = parser.nextText().trim().lowercase()
              if (l.startsWith("am")) language = "am" else if (l.isNotEmpty()) language = l.take(2)
            }
            "meta" -> {
              val nameAttr = parser.getAttributeValue(null, "name")
              val contentAttr = parser.getAttributeValue(null, "content")
              if (nameAttr.equals("cover", ignoreCase = true) && !contentAttr.isNullOrBlank()) {
                coverItemId = contentAttr
              }
            }
            "item" -> {
              val id = parser.getAttributeValue(null, "id") ?: ""
              val href = parser.getAttributeValue(null, "href") ?: ""
              val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
              val props = parser.getAttributeValue(null, "properties") ?: ""
              if (id.isNotEmpty() && href.isNotEmpty()) {
                manifest[id] = href to mediaType
                if (props.contains("cover-image", ignoreCase = true)) {
                  coverItemId = id
                }
                if (props.contains("nav", ignoreCase = true)) {
                  navHref = href
                }
                if (mediaType.equals("application/x-dtbncx+xml", ignoreCase = true)) {
                  ncxHref = href
                }
              }
            }
            "itemref" -> {
              val idref = parser.getAttributeValue(null, "idref")
              if (!idref.isNullOrBlank()) spineIds.add(idref)
            }
          }
        }
        event = parser.next()
      }
    }

    // Extract href -> title map from NCX or EPUB3 nav
    val tocLabelByHref = parseEpubTocLabels(entries, opfDir, ncxHref, navHref)

    val chapters = mutableListOf<EditableChapterDraft>()
    spineIds.forEachIndexed { idx, idref ->
      val (href, _) = manifest[idref] ?: return@forEachIndexed
      val cleanHref = href.substringBefore("#")
      val normalizedHref = (opfDir + cleanHref).replace("./", "")
      val htmlBytes = entries[normalizedHref] ?: entries[cleanHref] ?: return@forEachIndexed
      val htmlText = htmlBytes.toString(StandardCharsets.UTF_8)
      val chapterTitle =
        tocLabelByHref[cleanHref]
          ?: tocLabelByHref[normalizedHref]
          ?: extractHtmlHeading(htmlText)
          ?: "Chapter ${chapters.size + 1}"
      val plainBody = stripHtml(htmlText).trim()
      if (plainBody.length > 20) {
        chapters.add(
          EditableChapterDraft(
            id = "epub_ch_${idx + 1}",
            title = chapterTitle,
            bodyText = plainBody,
            role = inferRoleFromTitle(chapterTitle),
            confidence = HeadingConfidence.HIGH,
            detectionNote = "Extracted from EPUB spine & navigation",
          )
        )
      }
    }

    if (chapters.isEmpty()) {
      return ImportResult.Error("EPUB contained no readable XHTML chapters.")
    }

    var savedCoverPath: String? = null
    val coverHref =
      coverItemId?.let { manifest[it]?.first }
        ?: manifest.values.firstOrNull { (href, type) ->
          type.startsWith("image/") && href.contains("cover", ignoreCase = true)
        }?.first
    if (coverHref != null) {
      val coverBytes = entries[opfDir + coverHref] ?: entries[coverHref]
      if (coverBytes != null) {
        val coverTemp =
          File(context.cacheDir, "epub_cover_${System.currentTimeMillis()}.jpg").apply {
            writeBytes(coverBytes)
          }
        savedCoverPath = coverTemp.absolutePath
      }
    }

    val finalLang =
      if (language == "en" && detectEthiopicOrEnglish(chapters.first().bodyText) == "am") "am"
      else if (language == "am") "am"
      else "en"

    val tocEntries =
      chapters.mapIndexed { i, ch -> TocEntry(title = ch.title, chapterId = ch.id, chapterIndex = i, role = ch.role) }

    return ImportResult.DraftReady(
      BookEditorDraft(
        title = title,
        author = author,
        description = description,
        languageCode = finalLang,
        coverImagePath = savedCoverPath,
        chapters = chapters,
        tocEntries = tocEntries,
        detectionSummary = "Extracted ${chapters.size} chapter(s) from EPUB.",
      ),
      notice = "Extracted ${chapters.size} chapter(s) from EPUB.",
    )
  }

  private fun parseEpubTocLabels(
    entries: Map<String, ByteArray>,
    opfDir: String,
    ncxHref: String?,
    navHref: String?,
  ): Map<String, String> {
    val map = linkedMapOf<String, String>()
    if (navHref != null) {
      val navText = (entries[opfDir + navHref] ?: entries[navHref])?.toString(StandardCharsets.UTF_8)
      if (navText != null) {
        Regex("""<a[^>]*href=["']([^"'#]+)(?:#[^"']*)?["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .findAll(navText)
          .forEach { m ->
            val h = m.groupValues[1].trim()
            val label = stripHtml(m.groupValues[2]).trim()
            if (h.isNotEmpty() && label.isNotEmpty()) map.putIfAbsent(h, label)
          }
      }
    }
    if (ncxHref != null) {
      val ncxText = (entries[opfDir + ncxHref] ?: entries[ncxHref])?.toString(StandardCharsets.UTF_8)
      if (ncxText != null) {
        Regex(
          """<navLabel>\s*<text>(.*?)</text>\s*</navLabel>\s*<content[^>]*src=["']([^"'#]+)(?:#[^"']*)?["']""",
          setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
          .findAll(ncxText)
          .forEach { m ->
            val label = stripHtml(m.groupValues[1]).trim()
            val h = m.groupValues[2].trim()
            if (h.isNotEmpty() && label.isNotEmpty()) map.putIfAbsent(h, label)
          }
      }
    }
    return map
  }

  private fun parsePdf(fallbackTitle: String, bytes: ByteArray): ImportResult {
    // Pure-Kotlin offline PDF text, metadata & /Outlines extraction (supports FlateDecode streams)
    val latin = String(bytes, Charsets.ISO_8859_1)
    val extractedTitle =
      Regex("""/Title\s*\(([^)]{1,180})\)""").find(latin)?.groupValues?.get(1)?.let(::decodePdfLiteral)
        ?.takeIf { it.isNotBlank() } ?: fallbackTitle.replace('_', ' ')
    val extractedAuthor =
      Regex("""/Author\s*\(([^)]{1,120})\)""").find(latin)?.groupValues?.get(1)?.let(::decodePdfLiteral)
        ?.trim() ?: ""

    val textChunks = mutableListOf<String>()
    var idx = 0
    while (true) {
      val streamStart = latin.indexOf("stream", idx)
      if (streamStart < 0) break
      val dictStart = (streamStart - 400).coerceAtLeast(0)
      val dictHeader = latin.substring(dictStart, streamStart)
      var dataStart = streamStart + 6
      if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
      if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++
      val streamEnd = latin.indexOf("endstream", dataStart)
      if (streamEnd < 0) break

      val rawStream = bytes.copyOfRange(dataStart, streamEnd)
      val streamBytes =
        if (dictHeader.contains("/FlateDecode")) {
          inflateSafe(rawStream)
        } else {
          rawStream
        }

      if (streamBytes != null) {
        val content = String(streamBytes, Charsets.ISO_8859_1)
        if (content.contains("BT") && (content.contains("Tj") || content.contains("TJ"))) {
          val extracted = extractPdfTextOperators(content)
          if (extracted.isNotBlank()) textChunks.add(extracted)
        }
      }
      idx = streamEnd + 9
    }

    val combinedText = textChunks.joinToString("\n\n").trim()
    val hasImageXObjects = latin.contains("/Subtype /Image") || latin.contains("/Subtype/Image")

    if (combinedText.length < 60) {
      val explanation =
        if (hasImageXObjects) {
          "This PDF appears to be a scanned image PDF without embedded searchable text. BookWise operates 100% offline without cloud OCR. You can extract text using an offline tool or enter/paste the chapters manually."
        } else {
          "No readable embedded text stream could be extracted from this PDF (it may use custom font encodings or scanned pages). BookWise never uses cloud OCR; you can paste or type the book content manually below."
        }
      return ImportResult.ScannedPdfDetected(
        suggestedTitle = extractedTitle,
        explanation = explanation,
      )
    }

    val draft =
      parsePlainTextToDraft(extractedTitle, combinedText).copy(
        title = extractedTitle,
        author = extractedAuthor,
      )
    return ImportResult.DraftReady(
      draft,
      notice = "Extracted ${draft.chapters.size} chapter(s) offline from PDF.",
    )
  }

  private fun inflateSafe(input: ByteArray): ByteArray? =
    runCatching {
      val inflater = Inflater()
      inflater.setInput(input)
      val out = ByteArrayOutputStream()
      val buf = ByteArray(4096)
      while (!inflater.finished()) {
        val count = inflater.inflate(buf)
        if (count == 0) {
          if (inflater.needsInput() || inflater.needsDictionary()) break
        } else {
          out.write(buf, 0, count)
        }
      }
      inflater.end()
      out.toByteArray()
    }.getOrNull()

  private fun extractPdfTextOperators(stream: String): String {
    val sb = StringBuilder()
    val tjRegex = Regex("""\(([^()\\]*(?:\\.[^()\\]*)*)\)\s*Tj""")
    val tjArrayRegex = Regex("""\[(.*?)]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
    val btBlocks = Regex("""BT(.*?)ET""", RegexOption.DOT_MATCHES_ALL).findAll(stream)
    for (block in btBlocks) {
      val body = block.groupValues[1]
      val matches = mutableListOf<Pair<Int, String>>()
      tjRegex.findAll(body).forEach { m ->
        matches.add(m.range.first to decodePdfLiteral(m.groupValues[1]))
      }
      tjArrayRegex.findAll(body).forEach { m ->
        val inner = Regex("""\(([^()\\]*(?:\\.[^()\\]*)*)\)""").findAll(m.groupValues[1])
        val joined = inner.joinToString("") { decodePdfLiteral(it.groupValues[1]) }
        if (joined.isNotEmpty()) matches.add(m.range.first to joined)
      }
      matches.sortBy { it.first }
      val lineText = matches.joinToString(" ") { it.second }.replace(Regex("\\s+"), " ").trim()
      if (lineText.isNotEmpty()) sb.appendLine(lineText)
    }
    return sb.toString()
  }

  private fun decodePdfLiteral(raw: String): String {
    val unescaped =
      raw
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\(", "(")
        .replace("\\)", ")")
        .replace("\\\\", "\\")
    // Support UTF-16BE BOM (\u00FE\u00FF) if present
    if (unescaped.length >= 2 && unescaped[0] == '\u00FE' && unescaped[1] == '\u00FF') {
      val bytes = unescaped.drop(2).toByteArray(Charsets.ISO_8859_1)
      return String(bytes, Charsets.UTF_16BE)
    }
    return unescaped
  }

  private fun parseBookWiseJson(fileName: String, jsonText: String): ImportResult {
    val trimmed = jsonText.trim()
    val bundles = mutableListOf<MigratedBookBundle>()
    runCatching {
      if (trimmed.startsWith("[")) {
        val arr = JSONArray(trimmed)
        for (i in 0 until arr.length()) {
          arr.optJSONObject(i)?.let { obj -> jsonObjectToMigratedBundle(obj)?.let(bundles::add) }
        }
      } else {
        val obj = JSONObject(trimmed)
        if (obj.has("books") && obj.optJSONArray("books") != null) {
          val arr = obj.getJSONArray("books")
          for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { b -> jsonObjectToMigratedBundle(b)?.let(bundles::add) }
          }
        } else {
          jsonObjectToMigratedBundle(obj)?.let(bundles::add)
        }
      }
    }.onFailure {
      return ImportResult.Error("Could not parse JSON book file: ${it.message}")
    }

    if (bundles.isEmpty()) {
      return ImportResult.Error("No books found inside $fileName.")
    }
    if (bundles.size == 1 && bundles.first().chaptersByLang.size == 1) {
      val single = bundles.first()
      val lang = single.chaptersByLang.keys.first()
      return ImportResult.DraftReady(
        BookEditorDraft(
          title = single.book.title,
          subtitle = single.book.subtitle,
          author = single.book.author,
          category = single.book.category,
          subcategory = single.book.subcategory,
          description = single.book.description,
          publicationYear = single.book.publicationYear,
          languageCode = lang,
          bookType = single.book.bookType,
          tagsText = single.book.tags.joinToString(", "),
          personalNotes = single.book.personalNotes,
          chapters = single.chaptersByLang[lang] ?: emptyList(),
          detectionSummary = "Loaded BookWise JSON book '${single.book.title}'.",
        ),
        notice = "Loaded BookWise JSON book '${single.book.title}'.",
      )
    }
    return ImportResult.MultiBookMigration(bundles, fileName)
  }

  private fun parseTrebEditBookWise(fileName: String, raw: String): ImportResult {
    val jsArrayMatch =
      Regex("""(?:books|library|bookData)\s*=\s*(\[[\s\S]*?\])\s*;""", RegexOption.IGNORE_CASE)
        .find(raw)
    if (jsArrayMatch != null) {
      val res = parseBookWiseJson(fileName, jsArrayMatch.groupValues[1])
      if (res !is ImportResult.Error) return res
    }
    val jsonScriptMatch =
      Regex("""<script[^>]*type=["']application/json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
        .find(raw)
    if (jsonScriptMatch != null) {
      val res = parseBookWiseJson(fileName, jsonScriptMatch.groupValues[1])
      if (res !is ImportResult.Error) return res
    }

    val htmlTitle =
      Regex("""<title>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(raw)?.groupValues?.get(1)?.let(::stripHtml)?.trim()
        ?: Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(raw)?.groupValues?.get(1)?.let(::stripHtml)?.trim()
        ?: fileName.substringBeforeLast('.')

    val sectionMatches =
      Regex("""<(?:section|article)[^>]*>([\s\S]*?)</(?:section|article)>""", RegexOption.IGNORE_CASE)
        .findAll(raw)
        .toList()

    val chapters =
      if (sectionMatches.isNotEmpty()) {
        sectionMatches.mapIndexedNotNull { idx, m ->
          val chunk = m.groupValues[1]
          val chTitle = extractHtmlHeading(chunk) ?: "Chapter ${idx + 1}"
          val body = stripHtml(chunk).trim()
          if (body.isNotEmpty()) {
            EditableChapterDraft("html_ch_$idx", chTitle, body, role = inferRoleFromTitle(chTitle))
          } else null
        }
      } else {
        val plain = stripHtml(raw)
        return ImportResult.DraftReady(
          parsePlainTextToDraft(htmlTitle, plain),
          notice = "Migrated HTML document into editable chapters.",
        )
      }

    return ImportResult.DraftReady(
      BookEditorDraft(
        title = htmlTitle,
        languageCode = detectEthiopicOrEnglish(chapters.firstOrNull()?.bodyText ?: ""),
        chapters = chapters.ifEmpty { listOf(EditableChapterDraft("ch_1", "Chapter 1", stripHtml(raw))) },
        detectionSummary = "Migrated ${chapters.size} section(s) from BookWise HTML/JS.",
      ),
      notice = "Migrated ${chapters.size} section(s) from BookWise HTML/JS.",
    )
  }

  private fun jsonObjectToMigratedBundle(obj: JSONObject): MigratedBookBundle? {
    val title = obj.optString("title", "").trim()
    if (title.isEmpty()) return null
    val id = obj.optString("id", "").ifBlank { "book_${UUID.randomUUID().toString().take(10)}" }
    val author = obj.optString("author", "Unknown Author")
    val subtitle = obj.optString("subtitle", "")
    val category = obj.optString("category", "General")
    val subcategory = obj.optString("subcategory", "")
    val description = obj.optString("description", obj.optString("summary", ""))
    val year = obj.optString("publicationYear", obj.optString("year", ""))
    val bookType =
      if (obj.optString("bookType", "").contains("summary", true) || obj.optBoolean("isSummary", false)) {
        BookType.SUMMARY
      } else {
        BookType.FULL_BOOK
      }

    val chaptersByLang = linkedMapOf<String, List<EditableChapterDraft>>()
    val coverBytesByLang = linkedMapOf<String, ByteArray>()

    val editionsObj = obj.optJSONObject("editions")
    if (editionsObj != null) {
      val keys = editionsObj.keys()
      while (keys.hasNext()) {
        val lang = keys.next().lowercase().take(2)
        val ed = editionsObj.optJSONObject(lang) ?: continue
        val chs = extractChaptersFromJsonArray(ed.optJSONArray("chapters"))
        if (chs.isNotEmpty()) chaptersByLang[lang] = chs
        decodeBase64Cover(ed.optString("coverBase64", ""))?.let { coverBytesByLang[lang] = it }
      }
    }

    if (chaptersByLang.isEmpty()) {
      val lang = obj.optString("language", obj.optString("languageCode", "en")).lowercase().take(2)
      val directArr = obj.optJSONArray("chapters")
      if (directArr != null) {
        val chs = extractChaptersFromJsonArray(directArr)
        if (chs.isNotEmpty()) chaptersByLang[lang] = chs
      } else if (obj.has("content") || obj.has("text")) {
        val content = obj.optString("content", obj.optString("text", ""))
        chaptersByLang[lang] = parsePlainTextToDraft(title, content).chapters
      }
    }

    if (chaptersByLang.isEmpty()) {
      chaptersByLang["en"] = listOf(EditableChapterDraft("ch_1", "Chapter 1", description))
    }

    decodeBase64Cover(obj.optString("coverBase64", ""))?.let { bytes ->
      chaptersByLang.keys.forEach { lang -> coverBytesByLang.putIfAbsent(lang, bytes) }
    }

    val editions =
      chaptersByLang.keys.associateWith { lang ->
        BookEdition(
          languageCode = lang,
          languageLabel = LanguageCode.labelFor(lang),
          chapters = emptyList(),
          progress = ReadingProgress(),
        )
      }

    val book =
      Book(
        id = id,
        title = title,
        subtitle = subtitle,
        author = author,
        category = category,
        subcategory = subcategory,
        description = description,
        publicationYear = year,
        bookType = bookType,
        defaultLanguageCode = chaptersByLang.keys.first(),
        readingStatus = ReadingStatus.UNREAD,
        editions = editions,
      )
    return MigratedBookBundle(book, chaptersByLang, coverBytesByLang)
  }

  private fun extractChaptersFromJsonArray(arr: JSONArray?): List<EditableChapterDraft> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val item = arr.opt(i)
        when (item) {
          is JSONObject -> {
            val chTitle = item.optString("title", "Chapter ${i + 1}")
            val body = item.optString("bodyText", item.optString("content", item.optString("text", "")))
            add(EditableChapterDraft(item.optString("id", "ch_${i + 1}"), chTitle, body))
          }
          is String -> add(EditableChapterDraft("ch_${i + 1}", "Chapter ${i + 1}", item))
        }
      }
    }
  }

  private fun decodeBase64Cover(raw: String): ByteArray? {
    if (raw.isBlank()) return null
    val cleaned = raw.substringAfter("base64,", raw)
    return runCatching { android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT) }.getOrNull()
  }

  private fun extractHtmlHeading(html: String): String? {
    val m =
      Regex("""<h[1-3][^>]*>(.*?)</h[1-3]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
    return m?.groupValues?.get(1)?.let(::stripHtml)?.trim()?.takeIf { it.isNotEmpty() }
  }

  private fun stripHtml(html: String): String {
    val noScripts =
      html
        .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""</?(?:p|div|br|li|h[1-6]|tr|section|article)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""<[^>]+>"""), "")
    return noScripts
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }

  fun detectEthiopicOrEnglish(text: String): String {
    val ethiopicCount = text.count { it in '\u1200'..'\u137F' }
    return if (ethiopicCount >= 12) "am" else "en"
  }

  private fun resolveFileName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      if (it.moveToFirst()) {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) return it.getString(idx)
      }
    }
    return uri.lastPathSegment
  }
}
