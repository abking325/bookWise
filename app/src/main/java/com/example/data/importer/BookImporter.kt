package com.example.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.Book
import com.example.model.BookEdition
import com.example.model.BookEditorDraft
import com.example.model.BookType
import com.example.model.EditableChapterDraft
import com.example.model.LanguageCode
import com.example.model.ReadingProgress
import com.example.model.ReadingStatus
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
          ImportResult.DraftReady(draft, "Detected ${draft.chapters.size} chapter(s) from TXT.")
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
            ImportResult.DraftReady(draft)
          }
        }
      }
    }

  fun parsePlainTextToDraft(titleHint: String, rawText: String): BookEditorDraft {
    val clean = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
    val detectedLanguage = detectEthiopicOrEnglish(clean)
    val lines = clean.lines()

    // Try extracting metadata from initial header lines if present
    var extractedTitle = titleHint.replace('_', ' ').replace('-', ' ').trim().ifBlank { "Untitled Book" }
    var extractedAuthor = ""
    var bodyStartIndex = 0
    lines.take(15).forEachIndexed { idx, line ->
      val trimmed = line.trim()
      if (trimmed.startsWith("Title:", ignoreCase = true)) {
        extractedTitle = trimmed.substringAfter(":").trim().ifBlank { extractedTitle }
        bodyStartIndex = idx + 1
      } else if (trimmed.startsWith("Author:", ignoreCase = true) || trimmed.startsWith("By ", ignoreCase = false)) {
        extractedAuthor =
          if (trimmed.contains(":")) trimmed.substringAfter(":").trim()
          else trimmed.removePrefix("By ").trim()
        bodyStartIndex = idx + 1
      }
    }

    val headingRegex =
      Regex(
        """^(chapter\s+([0-9]+|[ivxlcdm]+)\b.*|part\s+([0-9]+|[ivxlcdm]+)\b.*|book\s+([0-9]+|[ivxlcdm]+)\b.*|section\s+[0-9]+\b.*|ምዕራፍ\s+[0-9፩-፼]+\b.*|#{1,3}\s+.+)$""",
        RegexOption.IGNORE_CASE,
      )

    val chapters = mutableListOf<EditableChapterDraft>()
    var currentTitle: String? = null
    val currentBody = StringBuilder()

    for (line in lines.drop(bodyStartIndex)) {
      val trimmed = line.trim()
      if (headingRegex.matches(trimmed)) {
        if (currentTitle != null || currentBody.isNotBlank()) {
          chapters.add(
            EditableChapterDraft(
              id = "ch_${chapters.size + 1}",
              title = currentTitle ?: "Introduction",
              bodyText = currentBody.toString().trim(),
            )
          )
          currentBody.clear()
        }
        currentTitle = trimmed.removePrefix("###").removePrefix("##").removePrefix("#").trim()
      } else {
        currentBody.appendLine(line)
      }
    }

    if (currentTitle != null || currentBody.isNotBlank()) {
      chapters.add(
        EditableChapterDraft(
          id = "ch_${chapters.size + 1}",
          title = currentTitle ?: "Chapter ${chapters.size + 1}",
          bodyText = currentBody.toString().trim(),
        )
      )
    }

    // If single massive chapter without headings (>15,000 chars), split at paragraph boundaries
    val finalChapters =
      if (chapters.size == 1 && chapters[0].bodyText.length > 15_000) {
        splitLargeTextIntoSections(chapters[0].bodyText)
      } else if (chapters.isEmpty()) {
        listOf(EditableChapterDraft("ch_1", "Chapter 1", clean))
      } else {
        chapters
      }

    return BookEditorDraft(
      title = extractedTitle,
      author = extractedAuthor,
      languageCode = detectedLanguage,
      chapters = finalChapters,
    )
  }

  private fun splitLargeTextIntoSections(text: String): List<EditableChapterDraft> {
    val paragraphs = text.split(Regex("\n\\s*\n+")).map { it.trim() }.filter { it.isNotEmpty() }
    val result = mutableListOf<EditableChapterDraft>()
    val chunk = StringBuilder()
    var part = 1
    for (p in paragraphs) {
      if (chunk.length + p.length > 7500 && chunk.isNotEmpty()) {
        result.add(EditableChapterDraft("ch_$part", "Section $part", chunk.toString().trim()))
        chunk.clear()
        part++
      }
      chunk.append(p).append("\n\n")
    }
    if (chunk.isNotEmpty()) {
      result.add(EditableChapterDraft("ch_$part", "Section $part", chunk.toString().trim()))
    }
    return result
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

    val chapters = mutableListOf<EditableChapterDraft>()
    spineIds.forEachIndexed { idx, idref ->
      val (href, _) = manifest[idref] ?: return@forEachIndexed
      val normalizedHref = (opfDir + href).replace("./", "")
      val htmlBytes = entries[normalizedHref] ?: entries[href] ?: return@forEachIndexed
      val htmlText = htmlBytes.toString(StandardCharsets.UTF_8)
      val chapterTitle = extractHtmlHeading(htmlText) ?: "Chapter ${chapters.size + 1}"
      val plainBody = stripHtml(htmlText).trim()
      if (plainBody.length > 20) {
        chapters.add(
          EditableChapterDraft(
            id = "epub_ch_${idx + 1}",
            title = chapterTitle,
            bodyText = plainBody,
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

    return ImportResult.DraftReady(
      BookEditorDraft(
        title = title,
        author = author,
        description = description,
        languageCode = finalLang,
        coverImagePath = savedCoverPath,
        chapters = chapters,
      ),
      notice = "Extracted ${chapters.size} chapter(s) from EPUB.",
    )
  }

  private fun parsePdf(fallbackTitle: String, bytes: ByteArray): ImportResult {
    // Offline pure-Kotlin PDF text & metadata extraction (supports FlateDecode streams & UTF-16BE/PDFDoc strings)
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

  private fun decodePdfLiteral(raw: String): String =
    raw
      .replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\\t", "\t")
      .replace("\\(", "(")
      .replace("\\)", ")")
      .replace("\\\\", "\\")

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
        ),
        notice = "Loaded BookWise JSON book '${single.book.title}'.",
      )
    }
    return ImportResult.MultiBookMigration(bundles, fileName)
  }

  private fun parseTrebEditBookWise(fileName: String, raw: String): ImportResult {
    // 1) Look for embedded JSON array or object in <script> or JS assignment (e.g. const books = [...])
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

    // 2) Fallback: Parse HTML headings (<h1..h3> / <section> / <article>) without altering content
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
          if (body.isNotEmpty()) EditableChapterDraft("html_ch_$idx", chTitle, body) else null
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

    // Format A: editions map { "en": { chapters: [...] }, "am": { chapters: [...] } }
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

    // Format B: direct chapters array or text field
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
