package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.importer.BookImporter
import com.example.data.importer.ImportResult
import com.example.model.Book
import com.example.model.BookEditorDraft
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.EditableChapterDraft
import com.example.model.LanguageCode
import com.example.ui.components.BOOK_COVER_ASPECT_RATIO
import com.example.ui.components.CoverCropperDialog
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookEditorScreen(
  initialBookId: String?,
  initialLanguageCode: String?,
  initialBookType: BookType?,
  categories: List<CategoryItem>,
  onLoadDraft: suspend (String?, String?) -> BookEditorDraft,
  onAutoSaveDraft: (BookEditorDraft) -> Unit,
  onCopyPickedCover: suspend (android.net.Uri) -> File?,
  onImportFile: suspend (android.net.Uri) -> ImportResult,
  onSaveBook: suspend (BookEditorDraft) -> Book,
  onClearDraft: () -> Unit,
  onShowMessage: (String) -> Unit,
  onSavedDone: (Book, addOtherLanguageCode: String?) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var draft by remember { mutableStateOf(BookEditorDraft()) }
  var isLoaded by remember { mutableStateOf(false) }
  var pendingCropSourceFile by remember { mutableStateOf<File?>(null) }
  var showPasteTextDialog by remember { mutableStateOf(false) }
  var scannedPdfNotice by remember { mutableStateOf<ImportResult.ScannedPdfDetected?>(null) }
  var postSaveOtherLangPrompt by remember { mutableStateOf<Pair<Book, String>?>(null) }
  var expandedChapterId by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(initialBookId, initialLanguageCode, initialBookType) {
    val loaded = onLoadDraft(initialBookId, initialLanguageCode)
    draft =
      if (initialBookId == null && initialBookType != null) {
        loaded.copy(bookType = initialBookType)
      } else {
        loaded
      }
    expandedChapterId = draft.chapters.firstOrNull()?.id
    isLoaded = true
  }

  LaunchedEffect(draft, isLoaded) {
    if (isLoaded && initialBookId == null) {
      onAutoSaveDraft(draft)
    }
  }

  BackHandler { onBack() }

  // Zero-permission Android Photo Picker for cover image
  val coverPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val file = onCopyPickedCover(uri)
          if (file != null) {
            pendingCropSourceFile = file
          } else {
            onShowMessage("Could not load selected cover image.")
          }
        }
      }
    }

  // Storage Access Framework document picker for TXT, EPUB, PDF, JSON, HTML, JS
  val documentPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          when (val result = onImportFile(uri)) {
            is ImportResult.DraftReady -> {
              val imp = result.draft
              draft =
                draft.copy(
                  title = imp.title.ifBlank { draft.title },
                  author = imp.author.ifBlank { draft.author },
                  description = imp.description.ifBlank { draft.description },
                  languageCode = imp.languageCode,
                  coverImagePath = imp.coverImagePath ?: draft.coverImagePath,
                  chapters = imp.chapters,
                )
              expandedChapterId = imp.chapters.firstOrNull()?.id
              result.notice?.let(onShowMessage)
            }
            is ImportResult.ScannedPdfDetected -> {
              draft = draft.copy(title = result.suggestedTitle.ifBlank { draft.title })
              scannedPdfNotice = result
            }
            is ImportResult.MultiBookMigration -> {
              val first = result.books.firstOrNull()
              if (first != null) {
                val lang = first.chaptersByLang.keys.first()
                draft =
                  draft.copy(
                    title = first.book.title,
                    subtitle = first.book.subtitle,
                    author = first.book.author,
                    category = first.book.category,
                    description = first.book.description,
                    languageCode = lang,
                    chapters = first.chaptersByLang[lang].orEmpty(),
                  )
                onShowMessage("Loaded '${first.book.title}' from BookWise migration file.")
              }
            }
            is ImportResult.Error -> onShowMessage(result.message)
          }
        }
      }
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            if (draft.editingBookId == null) "Add Book" else "Edit Book (${draft.languageCode.uppercase()})",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (draft.editingBookId == null) {
            TextButton(
              onClick = {
                onClearDraft()
                draft = BookEditorDraft(chapters = listOf(EditableChapterDraft("ch_1", "Chapter 1", "")))
                onShowMessage("Draft cleared.")
              }
            ) {
              Text("Reset")
            }
          }
          Button(
            onClick = {
              scope.launch {
                val saved = onSaveBook(draft)
                val otherLang =
                  when {
                    !saved.editions.containsKey("am") -> "am"
                    !saved.editions.containsKey("en") -> "en"
                    else -> null
                  }
                if (draft.editingBookId == null && otherLang != null) {
                  postSaveOtherLangPrompt = saved to otherLang
                } else {
                  onSavedDone(saved, null)
                }
              }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(end = 8.dp).testTag("editor_save_button"),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save")
          }
        },
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Quick Import & Paste Bar
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Import or Paste Content",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Automatically detects chapters & metadata locally from TXT, EPUB, PDF, pasted text, or BookWise JSON/HTML.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = {
                  documentPicker.launch(
                    arrayOf(
                      "text/plain",
                      "application/epub+zip",
                      "application/pdf",
                      "application/json",
                      "text/html",
                      "*/*",
                    )
                  )
                },
                modifier = Modifier.height(44.dp).testTag("editor_import_file_button"),
              ) {
                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import TXT / EPUB / PDF")
              }
              OutlinedButton(
                onClick = { showPasteTextDialog = true },
                modifier = Modifier.height(44.dp).testTag("editor_paste_text_button"),
              ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Complete Book")
              }
            }
          }
        }
      }

      // Cover Management + Core Info
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier.width(108.dp)
                  .aspectRatio(BOOK_COVER_ASPECT_RATIO)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainer),
              contentAlignment = Alignment.Center,
            ) {
              val coverPath = draft.coverImagePath
              if (!coverPath.isNullOrBlank() && File(coverPath).exists()) {
                AsyncImage(
                  model = File(coverPath),
                  contentDescription = "Book cover preview",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(
                  Icons.Filled.Image,
                  contentDescription = "No cover",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(32.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = {
                coverPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.height(40.dp).testTag("editor_pick_cover_button"),
            ) {
              Text("Cover", style = MaterialTheme.typography.labelMedium)
            }
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = draft.title,
              onValueChange = { draft = draft.copy(title = it) },
              label = { Text("Title *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("editor_title_input"),
            )
            OutlinedTextField(
              value = draft.subtitle,
              onValueChange = { draft = draft.copy(subtitle = it) },
              label = { Text("Subtitle") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = draft.author,
              onValueChange = { draft = draft.copy(author = it) },
              label = { Text("Author") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("editor_author_input"),
            )
          }
        }
      }

      // Language, Type, Category, Metadata
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Edition Language & Type", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = draft.languageCode == "en",
              onClick = { draft = draft.copy(languageCode = "en") },
              label = { Text("English") },
            )
            FilterChip(
              selected = draft.languageCode == "am",
              onClick = { draft = draft.copy(languageCode = "am") },
              label = { Text("አማርኛ (Amharic)") },
            )
            FilterChip(
              selected = draft.bookType == BookType.FULL_BOOK,
              onClick = { draft = draft.copy(bookType = BookType.FULL_BOOK) },
              label = { Text("Full Book") },
            )
            FilterChip(
              selected = draft.bookType == BookType.SUMMARY,
              onClick = { draft = draft.copy(bookType = BookType.SUMMARY) },
              label = { Text("Book Summary") },
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = draft.category,
              onValueChange = { draft = draft.copy(category = it) },
              label = { Text("Category") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = draft.subcategory,
              onValueChange = { draft = draft.copy(subcategory = it) },
              label = { Text("Subcategory") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          if (categories.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              categories.forEach { cat ->
                FilterChip(
                  selected = draft.category.equals(cat.name, ignoreCase = true),
                  onClick = { draft = draft.copy(category = cat.name) },
                  label = { Text(cat.name) },
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = draft.publicationYear,
              onValueChange = { draft = draft.copy(publicationYear = it) },
              label = { Text("Year") },
              singleLine = true,
              modifier = Modifier.weight(0.4f),
            )
            OutlinedTextField(
              value = draft.tagsText,
              onValueChange = { draft = draft.copy(tagsText = it) },
              label = { Text("Tags (comma separated)") },
              singleLine = true,
              modifier = Modifier.weight(0.6f),
            )
          }

          OutlinedTextField(
            value = draft.description,
            onValueChange = { draft = draft.copy(description = it) },
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = draft.personalNotes,
            onValueChange = { draft = draft.copy(personalNotes = it) },
            label = { Text("Personal Notes") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      // Editable Chapters Preview Header
      item {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Chapters Preview (${draft.chapters.size})",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Reorder, rename, insert, duplicate, or edit before saving.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = {
              val newCh =
                EditableChapterDraft(
                  id = "ch_${UUID.randomUUID().toString().take(6)}",
                  title =
                    if (draft.languageCode == "am") "ምዕራፍ ${draft.chapters.size + 1}"
                    else "Chapter ${draft.chapters.size + 1}",
                  bodyText = "",
                )
              draft = draft.copy(chapters = draft.chapters + newCh)
              expandedChapterId = newCh.id
            },
            modifier = Modifier.height(44.dp).testTag("editor_add_chapter_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Chapter")
          }
        }
      }

      // Chapter Cards
      itemsIndexed(draft.chapters, key = { _, ch -> ch.id }) { idx, ch ->
        val isExpanded = expandedChapterId == ch.id || draft.chapters.size == 1
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = ch.title,
                onValueChange = { newTitle ->
                  val updated = draft.chapters.toMutableList()
                  updated[idx] = ch.copy(title = newTitle)
                  draft = draft.copy(chapters = updated)
                },
                label = { Text("Chapter ${idx + 1} Title") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = {
                  if (idx > 0) {
                    val list = draft.chapters.toMutableList()
                    val item = list.removeAt(idx)
                    list.add(idx - 1, item)
                    draft = draft.copy(chapters = list)
                  }
                },
                enabled = idx > 0,
              ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
              }
              IconButton(
                onClick = {
                  if (idx < draft.chapters.lastIndex) {
                    val list = draft.chapters.toMutableList()
                    val item = list.removeAt(idx)
                    list.add(idx + 1, item)
                    draft = draft.copy(chapters = list)
                  }
                },
                enabled = idx < draft.chapters.lastIndex,
              ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TextButton(
                onClick = { expandedChapterId = if (isExpanded) null else ch.id }
              ) {
                Text(if (isExpanded) "Collapse Text" else "Edit Text (${ch.bodyText.length} chars)")
              }
              Row {
                TextButton(
                  onClick = {
                    val copy =
                      ch.copy(
                        id = "ch_${UUID.randomUUID().toString().take(6)}",
                        title = "${ch.title} (Copy)",
                      )
                    val list = draft.chapters.toMutableList().apply { add(idx + 1, copy) }
                    draft = draft.copy(chapters = list)
                    expandedChapterId = copy.id
                  }
                ) {
                  Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Duplicate")
                }
                if (draft.chapters.size > 1) {
                  IconButton(
                    onClick = {
                      val list = draft.chapters.toMutableList().apply { removeAt(idx) }
                      draft = draft.copy(chapters = list)
                    }
                  ) {
                    Icon(
                      Icons.Filled.DeleteOutline,
                      contentDescription = "Delete chapter",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }

            if (isExpanded) {
              OutlinedTextField(
                value = ch.bodyText,
                onValueChange = { newBody ->
                  val updated = draft.chapters.toMutableList()
                  updated[idx] = ch.copy(bodyText = newBody)
                  draft = draft.copy(chapters = updated)
                },
                label = { Text("Chapter Content") },
                minLines = 5,
                maxLines = 14,
                modifier = Modifier.fillMaxWidth().testTag("editor_chapter_body_$idx"),
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(72.dp)) }
    }
  }

  // Cover Cropper Modal
  pendingCropSourceFile?.let { srcFile ->
    CoverCropperDialog(
      sourceFile = srcFile,
      outputDir = File(context.filesDir, "bookwise_library/temp_covers"),
      onCropped = { cropped ->
        draft = draft.copy(coverImagePath = cropped.absolutePath)
        pendingCropSourceFile = null
      },
      onDismiss = { pendingCropSourceFile = null },
    )
  }

  // Paste Complete Book Dialog
  if (showPasteTextDialog) {
    var pastedText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showPasteTextDialog = false },
      title = { Text("Paste Complete Book Text") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "BookWise automatically detects chapter headings, numbering, and Ethiopic/English language locally.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = pastedText,
            onValueChange = { pastedText = it },
            placeholder = { Text("Paste complete book or chapters here…") },
            minLines = 6,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth().testTag("paste_book_dialog_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (pastedText.isNotBlank()) {
              val parsed = BookImporter.parsePlainTextToDraft(draft.title.ifBlank { "Imported Book" }, pastedText)
              draft =
                draft.copy(
                  title = draft.title.ifBlank { parsed.title },
                  author = draft.author.ifBlank { parsed.author },
                  languageCode = parsed.languageCode,
                  chapters = parsed.chapters,
                )
              expandedChapterId = parsed.chapters.firstOrNull()?.id
              onShowMessage("Detected ${parsed.chapters.size} chapter(s) from pasted text.")
            }
            showPasteTextDialog = false
          }
        ) {
          Text("Detect Chapters")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPasteTextDialog = false }) { Text("Cancel") }
      },
    )
  }

  // Scanned PDF Explanation Dialog
  scannedPdfNotice?.let { info ->
    AlertDialog(
      onDismissRequest = { scannedPdfNotice = null },
      title = { Text("Scanned PDF Detected") },
      text = { Text(info.explanation) },
      confirmButton = {
        Button(
          onClick = {
            scannedPdfNotice = null
            showPasteTextDialog = true
          }
        ) {
          Text("Paste or Enter Manually")
        }
      },
      dismissButton = {
        TextButton(onClick = { scannedPdfNotice = null }) { Text("Close") }
      },
    )
  }

  // Optional Other Language Edition Prompt After Creating New Book
  postSaveOtherLangPrompt?.let { (savedBook, otherLang) ->
    AlertDialog(
      onDismissRequest = {
        postSaveOtherLangPrompt = null
        onSavedDone(savedBook, null)
      },
      title = { Text("Add ${LanguageCode.labelFor(otherLang)} Edition?") },
      text = {
        Text(
          "'${savedBook.title}' is saved. Would you like to link a ${LanguageCode.labelFor(otherLang)} edition under the same book now?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            postSaveOtherLangPrompt = null
            onSavedDone(savedBook, otherLang)
          }
        ) {
          Text("Add ${LanguageCode.labelFor(otherLang)}")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            postSaveOtherLangPrompt = null
            onSavedDone(savedBook, null)
          }
        ) {
          Text("Done")
        }
      },
    )
  }
}
