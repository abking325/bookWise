package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BookEditorDraft
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.EditableChapterDraft
import com.example.model.HeadingConfidence
import com.example.model.LanguageCode
import com.example.model.SectionRole
import com.example.ui.components.BOOK_COVER_ASPECT_RATIO
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily
import java.io.File
import java.util.UUID

@Composable
internal fun EditorStepBar(
  currentStep: Int,
  onSelectStep: (Int) -> Unit,
) {
  val labels = listOf("01 Info", "02 Content", "03 Structure", "04 Review")
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    labels.forEachIndexed { idx, label ->
      val selected = idx == currentStep
      val completed = idx < currentStep
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.weight(1f)
            .clickable { onSelectStep(idx) }
            .padding(top = 10.dp)
            .testTag("editor_step_$idx"),
      ) {
        Text(
          text = label,
          fontFamily = ManropeFamily,
          fontSize = 11.5.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          color =
            when {
              selected -> MaterialTheme.colorScheme.onBackground
              completed -> MaterialTheme.colorScheme.secondary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 6.dp)
              .height(2.dp)
              .background(
                when {
                  selected -> MaterialTheme.colorScheme.onBackground
                  completed -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                  else -> Color.Transparent
                }
              )
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorInfoStep(
  draft: BookEditorDraft,
  categories: List<CategoryItem>,
  onUpdateDraft: (BookEditorDraft) -> Unit,
  onPickCover: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text(
      text = "Book Information & Cover",
      fontFamily = BodoniModaFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onBackground,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier =
            Modifier.width(104.dp)
              .aspectRatio(BOOK_COVER_ASPECT_RATIO)
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
              .clickable(onClick = onPickCover),
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
              Icon(
                Icons.Filled.Image,
                contentDescription = "Add cover",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "2:3 Cover",
                fontFamily = ManropeFamily,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onPickCover,
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.height(40.dp).testTag("editor_pick_cover_button"),
        ) {
          Text(
            text = if (draft.coverImagePath.isNullOrBlank()) "Add Cover" else "Change",
            fontSize = 11.5.sp,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = draft.title,
          onValueChange = { onUpdateDraft(draft.copy(title = it)) },
          label = { Text("Title *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("editor_title_input"),
        )
        OutlinedTextField(
          value = draft.subtitle,
          onValueChange = { onUpdateDraft(draft.copy(subtitle = it)) },
          label = { Text("Subtitle") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = draft.author,
          onValueChange = { onUpdateDraft(draft.copy(author = it)) },
          label = { Text("Author") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("editor_author_input"),
        )
      }
    }

    Text(
      text = "LANGUAGE & EDITION TYPE",
      fontFamily = ManropeFamily,
      fontSize = 10.5.sp,
      letterSpacing = 1.2.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      EditorChoiceTag("English", draft.languageCode == "en") {
        onUpdateDraft(draft.copy(languageCode = "en"))
      }
      EditorChoiceTag("አማርኛ (Amharic)", draft.languageCode == "am") {
        onUpdateDraft(draft.copy(languageCode = "am"))
      }
      EditorChoiceTag("Full Book", draft.bookType == BookType.FULL_BOOK) {
        onUpdateDraft(draft.copy(bookType = BookType.FULL_BOOK))
      }
      EditorChoiceTag("Book Summary", draft.bookType == BookType.SUMMARY) {
        onUpdateDraft(draft.copy(bookType = BookType.SUMMARY))
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = draft.category,
        onValueChange = { onUpdateDraft(draft.copy(category = it)) },
        label = { Text("Category") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = draft.subcategory,
        onValueChange = { onUpdateDraft(draft.copy(subcategory = it)) },
        label = { Text("Subcategory") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
    }

    if (categories.isNotEmpty()) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
          EditorChoiceTag(
            label = cat.name,
            selected = draft.category.equals(cat.name, ignoreCase = true),
            onClick = { onUpdateDraft(draft.copy(category = cat.name)) },
          )
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = draft.publicationYear,
        onValueChange = { onUpdateDraft(draft.copy(publicationYear = it)) },
        label = { Text("Year") },
        singleLine = true,
        modifier = Modifier.weight(0.4f),
      )
      OutlinedTextField(
        value = draft.tagsText,
        onValueChange = { onUpdateDraft(draft.copy(tagsText = it)) },
        label = { Text("Tags (comma separated)") },
        singleLine = true,
        modifier = Modifier.weight(0.6f),
      )
    }

    OutlinedTextField(
      value = draft.description,
      onValueChange = { onUpdateDraft(draft.copy(description = it)) },
      label = { Text("Description") },
      minLines = 2,
      maxLines = 4,
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
      value = draft.personalNotes,
      onValueChange = { onUpdateDraft(draft.copy(personalNotes = it)) },
      label = { Text("Personal Notes") },
      minLines = 2,
      maxLines = 4,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorContentStep(
  draft: BookEditorDraft,
  onImportDocument: () -> Unit,
  onOpenPasteDialog: () -> Unit,
  onStartManualWriting: () -> Unit,
  onRedetectFromCurrent: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text(
      text = "Import or Write Book",
      fontFamily = BodoniModaFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text =
        "Parse TXT, EPUB, PDF, BookWise archives, or pasted manuscripts offline—preserving every chapter without truncation or invented splits.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    EditorialMethodOption(
      icon = Icons.Filled.ContentPaste,
      title = "Paste Complete Book",
      subtitle = "Paste text and auto-detect English or Amharic sections.",
      isPrimary = true,
      testTag = "editor_paste_text_button",
      onClick = onOpenPasteDialog,
    )

    EditorialMethodOption(
      icon = Icons.Filled.FileOpen,
      title = "Import File (TXT / EPUB / PDF / JSON)",
      subtitle = "Select a document stored offline on this device.",
      isPrimary = false,
      testTag = "editor_import_file_button",
      onClick = onImportDocument,
    )

    EditorialMethodOption(
      icon = Icons.Filled.Edit,
      title = "Add Chapters Manually",
      subtitle = "Compose introduction, chapters, and sections directly.",
      isPrimary = false,
      testTag = "editor_manual_chapters_button",
      onClick = onStartManualWriting,
    )

    if (draft.detectionSummary.isNotBlank() || draft.chapters.any { it.bodyText.isNotBlank() }) {
      val totalWords =
        draft.chapters.sumOf {
          it.bodyText.trim().split(Regex("\\s+")).count { w -> w.isNotBlank() }
        }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "CURRENT MANUSCRIPT STATUS",
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.2.sp,
          color = MaterialTheme.colorScheme.secondary,
        )
        Text(
          text =
            draft.detectionSummary.ifBlank {
              "${draft.chapters.size} section(s) · ~$totalWords words ready."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(
          onClick = onRedetectFromCurrent,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
          Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Re-run Chapter Detection")
        }
      }
    }
  }
}

@Composable
private fun EditorialMethodOption(
  icon: ImageVector,
  title: String,
  subtitle: String,
  isPrimary: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.background,
    border =
      BorderStroke(
        width = if (isPrimary) 1.dp else 0.75.dp,
        color =
          if (isPrimary) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isPrimary) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontFamily = LiterataFamily,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorStructureOptionsHeader(
  draft: BookEditorDraft,
  onUpdateDraft: (BookEditorDraft) -> Unit,
  onAddChapter: () -> Unit,
  onAddIntroduction: () -> Unit,
  onToggleTableOfContents: () -> Unit,
  onSplitManualChapter: (EditableChapterDraft) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = "Structure & Sections (${draft.chapters.size})",
      fontFamily = BodoniModaFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onBackground,
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Include Title Page", style = MaterialTheme.typography.bodyMedium)
      Switch(
        checked = draft.showTitlePage,
        onCheckedChange = { onUpdateDraft(draft.copy(showTitlePage = it)) },
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Include Table of Contents", style = MaterialTheme.typography.bodyMedium)
      Switch(
        checked = draft.showTableOfContents,
        onCheckedChange = { onUpdateDraft(draft.copy(showTableOfContents = it)) },
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

    val singleManual =
      draft.chapters.size == 1 && draft.chapters.first().confidence == HeadingConfidence.MANUAL
    if (singleManual && draft.chapters.first().bodyText.length > 200) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "No headings detected — full text preserved untouched.",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Text(
          text = "BookWise never invents arbitrary splits. You can split manually or keep one continuous text.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = { onSplitManualChapter(draft.chapters.first()) },
          shape = RoundedCornerShape(50),
        ) {
          Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Split Chapter at Midpoint / Divider")
        }
      }
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = onAddChapter,
        shape = RoundedCornerShape(50),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = Modifier.height(44.dp).testTag("editor_add_chapter_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Chapter", fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = onAddIntroduction,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(44.dp).testTag("editor_add_intro_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Introduction", fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = onToggleTableOfContents,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(44.dp).testTag("editor_add_toc_button"),
      ) {
        Text(
          text = if (draft.showTableOfContents) "TOC Included ✓" else "+ Table of Contents",
          fontSize = 12.sp,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorChapterCard(
  idx: Int,
  totalCount: Int,
  ch: EditableChapterDraft,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onUpdate: (EditableChapterDraft) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onMergeWithNext: () -> Unit,
  onDuplicate: () -> Unit,
  onSplitMidpoint: () -> Unit,
  onDelete: () -> Unit,
) {
  var showRoleMenu by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = if (ch.needsReview) 1.dp else 0.75.dp,
          color =
            if (ch.needsReview) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(10.dp),
        )
        .padding(14.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      val indexLabel =
        if (ch.role == SectionRole.CHAPTER) "${(idx + 1).toString().padStart(2, '0')} —"
        else ch.role.labelEn.uppercase()
      Text(
        text = indexLabel,
        fontFamily = ManropeFamily,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.secondary,
      )
      Row {
        IconButton(onClick = onMoveUp, enabled = idx > 0) {
          Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onMoveDown, enabled = idx < totalCount - 1) {
          Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
        }
      }
    }

    OutlinedTextField(
      value = ch.title,
      onValueChange = { onUpdate(ch.copy(title = it)) },
      label = { Text("Section Title") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box {
        EditorChoiceTag("Role: ${ch.role.labelEn}", false) { showRoleMenu = true }
        DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
          SectionRole.entries.forEach { role ->
            DropdownMenuItem(
              text = { Text(role.labelEn) },
              onClick = {
                onUpdate(ch.copy(role = role))
                showRoleMenu = false
              },
            )
          }
        }
      }

      EditorChoiceTag(ch.confidence.labelEn, ch.needsReview) {}

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Include", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
          checked = ch.isIncluded,
          onCheckedChange = { onUpdate(ch.copy(isIncluded = it)) },
        )
      }
    }

    if (ch.detectionNote.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = ch.detectionNote,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      TextButton(onClick = onToggleExpand) {
        Text(if (isExpanded) "Collapse Text" else "Edit Text (${ch.bodyText.length} chars)")
      }
      if (idx < totalCount - 1) {
        TextButton(onClick = onMergeWithNext) {
          Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Merge Next")
        }
      }
      if (ch.bodyText.length > 160) {
        TextButton(onClick = onSplitMidpoint) {
          Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Split")
        }
      }
      TextButton(onClick = onDuplicate) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Duplicate")
      }
      if (totalCount > 1) {
        IconButton(onClick = onDelete) {
          Icon(
            Icons.Filled.DeleteOutline,
            contentDescription = "Delete chapter",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    if (isExpanded) {
      OutlinedTextField(
        value = ch.bodyText,
        onValueChange = { onUpdate(ch.copy(bodyText = it)) },
        label = { Text("Section Text") },
        minLines = 5,
        maxLines = 14,
        modifier = Modifier.fillMaxWidth().testTag("editor_chapter_body_$idx"),
      )
    }
  }
}

@Composable
internal fun EditorReviewStep(
  draft: BookEditorDraft,
  onSaveClick: () -> Unit,
) {
  val includedChapters = draft.chapters.filter { it.isIncluded }
  val totalWords =
    includedChapters.sumOf {
      it.bodyText.trim().split(Regex("\\s+")).count { w -> w.isNotBlank() }
    }
  val estMinutes = (totalWords / 200).coerceAtLeast(1)

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Print Edition Preview",
      fontFamily = BodoniModaFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onBackground,
    )

    if (draft.showTitlePage) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (!draft.coverImagePath.isNullOrBlank() && File(draft.coverImagePath).exists()) {
          AsyncImage(
            model = File(draft.coverImagePath),
            contentDescription = "Cover preview",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.width(90.dp)
                .aspectRatio(BOOK_COVER_ASPECT_RATIO)
                .clip(RoundedCornerShape(4.dp)),
          )
          Spacer(modifier = Modifier.height(14.dp))
        }
        Text(
          text = "${draft.bookType.labelEn.uppercase()} · ${LanguageCode.labelFor(draft.languageCode).uppercase()}",
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.4.sp,
          color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = draft.title.ifBlank { "Untitled Book" },
          fontFamily = BodoniModaFamily,
          fontSize = 24.sp,
          lineHeight = 30.sp,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onBackground,
        )
        if (draft.subtitle.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = draft.subtitle,
            fontFamily = LiterataFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = draft.author.ifBlank { "Unknown Author" },
          fontFamily = LiterataFamily,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "${includedChapters.size} section(s) · ~$totalWords words · ~$estMinutes min read",
          fontFamily = ManropeFamily,
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }

    if (draft.showTableOfContents) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "TABLE OF CONTENTS",
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.4.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
        var bodyIndex = 0
        includedChapters.forEach { ch ->
          val label =
            if (ch.role == SectionRole.CHAPTER) {
              bodyIndex++
              "${bodyIndex.toString().padStart(2, '0')} — ${ch.title}"
            } else {
              ch.title
            }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = label,
              fontFamily = LiterataFamily,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = ch.role.labelEn,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
        }
      }
    }

    Button(
      onClick = onSaveClick,
      shape = RoundedCornerShape(50),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("editor_finish_save_button"),
    ) {
      Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Save Book Offline", style = MaterialTheme.typography.titleSmall)
    }
  }
}

@Composable
private fun EditorChoiceTag(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
    border =
      BorderStroke(
        width = 0.75.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.height(36.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
      Text(
        text = label,
        fontFamily = ManropeFamily,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

internal fun splitDraftChapter(ch: EditableChapterDraft): Pair<EditableChapterDraft, EditableChapterDraft> {
  val body = ch.bodyText.trim()
  val markerIdx = body.indexOf("\n---\n")
  val splitPoint =
    when {
      markerIdx > 0 -> markerIdx
      else -> {
        val mid = body.length / 2
        val paraBreak = body.indexOf("\n\n", mid).takeIf { it > 0 }
          ?: body.lastIndexOf("\n\n", mid).takeIf { it > 0 }
          ?: body.indexOf('\n', mid).takeIf { it > 0 }
          ?: mid
        paraBreak
      }
    }
  val firstPart = body.substring(0, splitPoint).trim()
  val secondPart = body.substring(splitPoint).removePrefix("---").trim()
  val first = ch.copy(bodyText = firstPart, confidence = HeadingConfidence.MANUAL, needsReview = false)
  val second =
    EditableChapterDraft(
      id = "ch_${UUID.randomUUID().toString().take(6)}",
      title = "${ch.title} (Part 2)",
      bodyText = secondPart,
      role = ch.role,
      confidence = HeadingConfidence.MANUAL,
      needsReview = false,
    )
  return first to second
}
