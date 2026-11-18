package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.importer.BookImporter
import com.example.data.importer.ImportResult
import com.example.model.Book
import com.example.model.BookEditorDraft
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.EditableChapterDraft
import com.example.model.HeadingConfidence
import com.example.model.LanguageCode
import com.example.model.SectionRole
import com.example.ui.components.CoverCropperDialog
import com.example.ui.theme.BodoniModaFamily
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
  var currentStep by remember { mutableIntStateOf(0) }
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

  fun performSave() {
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
  }

  BackHandler {
    if (currentStep > 0) currentStep-- else onBack()
  }

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
                  publicationYear = imp.publicationYear.ifBlank { draft.publicationYear },
                  description = imp.description.ifBlank { draft.description },
                  languageCode = imp.languageCode,
                  coverImagePath = imp.coverImagePath ?: draft.coverImagePath,
                  chapters = imp.chapters,
                  tocEntries = imp.tocEntries,
                  detectionSummary = imp.detectionSummary,
                )
              expandedChapterId = imp.chapters.firstOrNull()?.id
              currentStep = 2
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
                currentStep = 2
                onShowMessage("Loaded '${first.book.title}' from BookWise migration file.")
              }
            }
            is ImportResult.Error -> onShowMessage(result.message)
          }
        }
      }
    }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Column {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
          title = {
            Text(
              text =
                if (draft.editingBookId == null) "Add Book"
                else "Edit Edition (${draft.languageCode.uppercase()})",
              fontFamily = BodoniModaFamily,
              style = MaterialTheme.typography.headlineSmall,
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onBack,
              modifier = Modifier.size(48.dp).testTag("editor_back_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            if (draft.editingBookId == null) {
              TextButton(
                onClick = {
                  onClearDraft()
                  draft = BookEditorDraft(chapters = listOf(EditableChapterDraft("ch_1", "Chapter 1", "")))
                  currentStep = 0
                  onShowMessage("Draft cleared.")
                }
              ) {
                Text("Reset")
              }
            }
            Button(
              onClick = { performSave() },
              shape = RoundedCornerShape(50),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
              modifier = Modifier.padding(end = 8.dp).testTag("editor_save_button"),
            ) {
              Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save", fontWeight = FontWeight.Medium)
            }
          },
        )
        EditorStepBar(currentStep = currentStep, onSelectStep = { currentStep = it })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
      ) {
        Column {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            OutlinedButton(
              onClick = { if (currentStep > 0) currentStep-- else onBack() },
              shape = RoundedCornerShape(50),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (currentStep > 0) "Back" else "Cancel")
            }

            if (currentStep < 3) {
              Button(
                onClick = { currentStep++ },
                shape = RoundedCornerShape(50),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.height(48.dp).testTag("editor_next_step_button"),
              ) {
                Text("Next Step")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            } else {
              Button(
                onClick = { performSave() },
                shape = RoundedCornerShape(50),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.height(48.dp),
              ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Book")
              }
            }
          }
        }
      }
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      when (currentStep) {
        0 -> {
          item {
            EditorInfoStep(
              draft = draft,
              categories = categories,
              onUpdateDraft = { draft = it },
              onPickCover = {
                coverPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
            )
          }
        }
        1 -> {
          item {
            EditorContentStep(
              draft = draft,
              onImportDocument = {
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
              onOpenPasteDialog = { showPasteTextDialog = true },
              onStartManualWriting = {
                if (draft.chapters.isEmpty()) {
                  val ch = EditableChapterDraft("ch_1", "Chapter 1", "")
                  draft = draft.copy(chapters = listOf(ch))
                  expandedChapterId = ch.id
                }
                currentStep = 2
              },
              onRedetectFromCurrent = {
                val combined = draft.chapters.joinToString("\n\n") { "${it.title}\n\n${it.bodyText}" }
                val parsed = BookImporter.parsePlainTextToDraft(draft.title.ifBlank { "Book" }, combined)
                draft =
                  draft.copy(
                    chapters = parsed.chapters,
                    tocEntries = parsed.tocEntries,
                    detectionSummary = parsed.detectionSummary,
                  )
                expandedChapterId = parsed.chapters.firstOrNull()?.id
                onShowMessage(parsed.detectionSummary)
                currentStep = 2
              },
            )
          }
        }
        2 -> {
          item {
            EditorStructureOptionsHeader(
              draft = draft,
              onUpdateDraft = { draft = it },
              onAddChapter = {
                val bodyCount = draft.chapters.count { it.role == SectionRole.CHAPTER }
                val newCh =
                  EditableChapterDraft(
                    id = "ch_${UUID.randomUUID().toString().take(6)}",
                    title =
                      if (draft.languageCode == "am") "ምዕራፍ ${bodyCount + 1}"
                      else "Chapter ${bodyCount + 1}",
                    bodyText = "",
                    role = SectionRole.CHAPTER,
                    confidence = HeadingConfidence.MANUAL,
                  )
                draft = draft.copy(chapters = draft.chapters + newCh)
                expandedChapterId = newCh.id
              },
              onAddIntroduction = {
                val existing = draft.chapters.firstOrNull { it.role == SectionRole.INTRODUCTION }
                if (existing != null) {
                  expandedChapterId = existing.id
                } else {
                  val intro =
                    EditableChapterDraft(
                      id = "intro_${UUID.randomUUID().toString().take(6)}",
                      title = if (draft.languageCode == "am") "መግቢያ" else "Introduction",
                      bodyText = "",
                      role = SectionRole.INTRODUCTION,
                      confidence = HeadingConfidence.MANUAL,
                    )
                  draft = draft.copy(chapters = listOf(intro) + draft.chapters)
                  expandedChapterId = intro.id
                }
              },
              onToggleTableOfContents = {
                val next = !draft.showTableOfContents
                draft = draft.copy(showTableOfContents = next)
                onShowMessage(
                  if (next) "Table of Contents enabled for this edition."
                  else "Table of Contents hidden."
                )
              },
              onSplitManualChapter = { ch ->
                val idx = draft.chapters.indexOfFirst { it.id == ch.id }.coerceAtLeast(0)
                val (first, second) = splitDraftChapter(ch)
                val list = draft.chapters.toMutableList()
                list[idx] = first
                list.add(idx + 1, second)
                draft = draft.copy(chapters = list)
                expandedChapterId = second.id
              },
            )
          }

          itemsIndexed(draft.chapters, key = { _, ch -> ch.id }) { idx, ch ->
            val isExpanded = expandedChapterId == ch.id || draft.chapters.size == 1
            EditorChapterCard(
              idx = idx,
              totalCount = draft.chapters.size,
              ch = ch,
              isExpanded = isExpanded,
              onToggleExpand = { expandedChapterId = if (isExpanded) null else ch.id },
              onUpdate = { updated ->
                val list = draft.chapters.toMutableList()
                list[idx] = updated
                draft = draft.copy(chapters = list)
              },
              onMoveUp = {
                if (idx > 0) {
                  val list = draft.chapters.toMutableList()
                  val item = list.removeAt(idx)
                  list.add(idx - 1, item)
                  draft = draft.copy(chapters = list)
                }
              },
              onMoveDown = {
                if (idx < draft.chapters.lastIndex) {
                  val list = draft.chapters.toMutableList()
                  val item = list.removeAt(idx)
                  list.add(idx + 1, item)
                  draft = draft.copy(chapters = list)
                }
              },
              onMergeWithNext = {
                if (idx < draft.chapters.lastIndex) {
                  val list = draft.chapters.toMutableList()
                  val next = list.removeAt(idx + 1)
                  list[idx] =
                    ch.copy(
                      bodyText = "${ch.bodyText.trim()}\n\n${next.title}\n\n${next.bodyText.trim()}".trim()
                    )
                  draft = draft.copy(chapters = list)
                }
              },
              onDuplicate = {
                val copy =
                  ch.copy(
                    id = "ch_${UUID.randomUUID().toString().take(6)}",
                    title = "${ch.title} (Copy)",
                  )
                val list = draft.chapters.toMutableList().apply { add(idx + 1, copy) }
                draft = draft.copy(chapters = list)
                expandedChapterId = copy.id
              },
              onSplitMidpoint = {
                val (first, second) = splitDraftChapter(ch)
                val list = draft.chapters.toMutableList()
                list[idx] = first
                list.add(idx + 1, second)
                draft = draft.copy(chapters = list)
                expandedChapterId = second.id
              },
              onDelete = {
                val list = draft.chapters.toMutableList().apply { removeAt(idx) }
                draft = draft.copy(chapters = list)
              },
            )
          }
        }
        3 -> {
          item {
            EditorReviewStep(
              draft = draft,
              onSaveClick = { performSave() },
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(32.dp)) }
    }
  }

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

  if (showPasteTextDialog) {
    var pastedText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showPasteTextDialog = false },
      title = { Text("Paste Complete Book Text") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "BookWise automatically detects chapters, front/back matter, and Ethiopic/English language offline without truncating text.",
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
                  publicationYear = draft.publicationYear.ifBlank { parsed.publicationYear },
                  languageCode = parsed.languageCode,
                  chapters = parsed.chapters,
                  tocEntries = parsed.tocEntries,
                  detectionSummary = parsed.detectionSummary,
                )
              expandedChapterId = parsed.chapters.firstOrNull()?.id
              currentStep = 2
              onShowMessage(parsed.detectionSummary)
            }
            showPasteTextDialog = false
          },
          modifier = Modifier.testTag("paste_book_detect_button"),
        ) {
          Text("Detect Chapters")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPasteTextDialog = false }) { Text("Cancel") }
      },
    )
  }

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
          },
          modifier = Modifier.testTag("editor_post_save_done_button"),
        ) {
          Text("Done")
        }
      },
    )
  }
}
