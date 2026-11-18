package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.BookNote
import com.example.model.Bookmark
import com.example.model.Highlight
import com.example.model.LanguageCode
import com.example.model.ReadingStatus
import com.example.model.SectionRole
import com.example.ui.components.BookCoverView
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailsScreen(
  book: Book,
  bookmarks: List<Bookmark>,
  highlights: List<Highlight>,
  notes: List<BookNote>,
  strings: UiStrings,
  onBack: () -> Unit,
  onLoadAnnotations: (String, String) -> Unit,
  onOpenReader: (bookId: String, languageCode: String, chapterIndex: Int, paragraphIndex: Int) -> Unit,
  onResetProgress: (bookId: String, languageCode: String) -> Unit,
  onStartAudio: (bookId: String, languageCode: String, chapterIndex: Int, paragraphIndex: Int, sentenceIndex: Int) -> Unit,
  onEditEdition: (bookId: String, languageCode: String) -> Unit,
  onAddMissingEdition: (bookId: String, targetLang: String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onSetStatus: (String, ReadingStatus) -> Unit,
  onExportBookZip: (Book) -> Unit,
  onDeleteBook: (String) -> Unit,
) {
  var selectedLang by remember(book.id) { mutableStateOf(book.defaultLanguageCode) }
  val edition = book.edition(selectedLang) ?: book.editions.values.firstOrNull()
  val activeLang = edition?.languageCode ?: "en"
  var selectedTab by remember { mutableIntStateOf(0) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(book.id, activeLang) {
    onLoadAnnotations(book.id, activeLang)
  }

  val chapters = edition?.chapters.orEmpty()
  val introChapter =
    remember(chapters) {
      chapters.firstOrNull {
        it.role == SectionRole.INTRODUCTION ||
          it.role == SectionRole.PREFACE ||
          it.role == SectionRole.FOREWORD
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
              text = book.bookType.labelEn.uppercase(),
              fontFamily = ManropeFamily,
              fontSize = 11.sp,
              letterSpacing = 1.4.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            IconButton(
              onClick = { onToggleFavorite(book.id) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint =
                  if (book.isFavorite) MaterialTheme.colorScheme.secondary
                  else MaterialTheme.colorScheme.onBackground,
              )
            }
            IconButton(
              onClick = { onEditEdition(book.id, activeLang) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = strings.editBook)
            }
            Box {
              IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
              }
              DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
              ) {
                DropdownMenuItem(
                  text = { Text(strings.exportBookZip) },
                  onClick = {
                    showOverflowMenu = false
                    onExportBookZip(book)
                  },
                )
                DropdownMenuItem(
                  text = { Text("Restart Book") },
                  onClick = {
                    showOverflowMenu = false
                    onResetProgress(book.id, activeLang)
                  },
                )
                DropdownMenuItem(
                  text = { Text(strings.deleteBook, color = MaterialTheme.colorScheme.error) },
                  onClick = {
                    showOverflowMenu = false
                    showDeleteConfirm = true
                  },
                )
              }
            }
          },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
      }
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
    ) {
      // Book Jacket Hero Row
      item(key = "details_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top,
        ) {
          BookCoverView(
            book = book,
            edition = edition,
            showThickness = true,
            elevation = 10.dp,
            modifier = Modifier.width(108.dp),
          )
          Spacer(modifier = Modifier.width(18.dp))
          Column(modifier = Modifier.weight(1f)) {
            val eyebrow =
              listOfNotNull(
                  book.category.takeIf { it.isNotBlank() }?.uppercase(),
                  book.subcategory.takeIf { it.isNotBlank() }?.uppercase(),
                )
                .joinToString(" · ")
            if (eyebrow.isNotBlank()) {
              Text(
                text = eyebrow,
                fontFamily = ManropeFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
              text = book.title,
              fontFamily = BodoniModaFamily,
              fontSize = 26.sp,
              lineHeight = 31.sp,
              fontWeight = FontWeight.Normal,
              color = MaterialTheme.colorScheme.onBackground,
            )
            if (book.subtitle.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = book.subtitle,
                fontFamily = LiterataFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = book.author,
              fontFamily = LiterataFamily,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val estMins = chapters.sumOf { it.estimatedMinutes }.coerceAtLeast(1)
            val metaParts =
              listOfNotNull(
                  book.publicationYear.takeIf { it.isNotBlank() },
                  "${chapters.size} sections",
                  "~$estMins min",
                )
                .joinToString(" · ")
            Text(
              text = metaParts,
              fontFamily = ManropeFamily,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Editions & Reading Progress
      item(key = "details_editions") {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          book.editions.keys.forEach { code ->
            val selected = activeLang == code
            Surface(
              onClick = { selectedLang = code },
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
                  text = LanguageCode.labelFor(code),
                  fontFamily = ManropeFamily,
                  fontSize = 12.sp,
                  fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground,
                )
              }
            }
          }
          val missingOther =
            when {
              !book.editions.containsKey("am") -> "am"
              !book.editions.containsKey("en") -> "en"
              else -> null
            }
          if (missingOther != null) {
            Surface(
              onClick = { onAddMissingEdition(book.id, missingOther) },
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.background,
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.height(36.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Add ${LanguageCode.labelFor(missingOther)}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        val savedProg = edition?.progress
        val progress = savedProg?.overallProgress ?: 0f
        if (progress > 0.01f) {
          Spacer(modifier = Modifier.height(14.dp))
          LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth().height(2.5.dp),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              "${(progress * 100).toInt()}% completed · Ch ${(savedProg?.chapterIndex ?: 0) + 1}, Para ${(savedProg?.paragraphIndex ?: 0) + 1}",
            fontFamily = ManropeFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Primary Read & Listen Actions
      item(key = "details_actions") {
        val savedProg = edition?.progress
        val progress = savedProg?.overallProgress ?: 0f

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              onOpenReader(
                book.id,
                activeLang,
                savedProg?.chapterIndex ?: 0,
                savedProg?.paragraphIndex ?: 0,
              )
            },
            shape = RoundedCornerShape(50),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("details_read_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (progress > 0.01f) strings.continueReading else strings.readNow,
              fontFamily = ManropeFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
            )
          }

          val audioProg = edition?.audioProgress
          val hasAudioProg = audioProg != null && audioProg.lastUpdatedAt > 0L
          OutlinedButton(
            onClick = {
              if (hasAudioProg) {
                onStartAudio(
                  book.id,
                  activeLang,
                  audioProg!!.chapterIndex,
                  audioProg.paragraphIndex,
                  audioProg.sentenceIndex,
                )
              } else {
                onStartAudio(
                  book.id,
                  activeLang,
                  savedProg?.chapterIndex ?: 0,
                  savedProg?.paragraphIndex ?: 0,
                  0,
                )
              }
            },
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.height(50.dp).testTag("details_listen_button"),
          ) {
            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (hasAudioProg) "Resume Audio" else "Listen",
              fontSize = 13.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ReadingStatus.entries.forEach { st ->
            val selected = book.readingStatus == st
            Text(
              text = st.labelEn,
              fontFamily = ManropeFamily,
              fontSize = 12.sp,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              color =
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.clickable { onSetStatus(book.id, st) }.padding(vertical = 4.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Description, Introduction Preview, Personal Notes & Section Underline Tabs
      item(key = "details_editorial_body") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

        if (book.description.isNotBlank()) {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "SYNOPSIS",
            fontFamily = ManropeFamily,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = book.description,
            fontFamily = LiterataFamily,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        if (introChapter != null) {
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onOpenReader(book.id, activeLang, introChapter.index, 0) }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = introChapter.role.labelEn.uppercase(),
                fontFamily = ManropeFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = introChapter.title,
                fontFamily = LiterataFamily,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
            Text(
              text = "Read →",
              fontFamily = ManropeFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
        }

        if (book.personalNotes.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
              text = "PERSONAL NOTES",
              fontFamily = ManropeFamily,
              fontSize = 10.sp,
              letterSpacing = 1.2.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = book.personalNotes,
              fontFamily = ManropeFamily,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val tabs =
          listOf(
            "Contents (${chapters.size})",
            "Bookmarks (${bookmarks.count { it.languageCode == activeLang }})",
            "Highlights (${highlights.count { it.languageCode == activeLang }})",
            "Notes (${notes.count { it.languageCode == activeLang }})",
          )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          tabs.forEachIndexed { idx, label ->
            val selected = selectedTab == idx
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable { selectedTab = idx }.padding(top = 6.dp),
            ) {
              Text(
                text = label,
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                  if (selected) MaterialTheme.colorScheme.onBackground
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier =
                  Modifier.width(36.dp)
                    .height(2.dp)
                    .background(
                      if (selected) MaterialTheme.colorScheme.onBackground
                      else Color.Transparent
                    )
              )
            }
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
      }

      when (selectedTab) {
        0 -> {
          val numberedMap = buildBodyChapterNumbers(chapters)
          items(chapters, key = { it.id }) { ch ->
            val displayNumber = numberedMap[ch.id]
            val titleLine =
              if (displayNumber != null) {
                "${displayNumber.toString().padStart(2, '0')} — ${ch.title}"
              } else {
                ch.title
              }
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onOpenReader(book.id, activeLang, ch.index, 0) }
                  .padding(vertical = 14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  if (ch.role != SectionRole.CHAPTER) {
                    Text(
                      text = ch.role.labelEn.uppercase(),
                      fontFamily = ManropeFamily,
                      fontSize = 9.5.sp,
                      letterSpacing = 1.2.sp,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                  }
                  Text(
                    text = titleLine,
                    fontFamily = LiterataFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "${ch.estimatedMinutes} min",
                  fontFamily = ManropeFamily,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
          }
        }
        1 -> {
          val langBookmarks = bookmarks.filter { it.languageCode == activeLang }
          if (langBookmarks.isEmpty()) {
            item { EmptyTabMessage("No bookmarks yet. Tap the bookmark icon while reading.") }
          } else {
            items(langBookmarks, key = { it.id }) { bm ->
              AnnotationFlatRow(
                header = bm.chapterTitle,
                body = bm.excerpt,
                note = bm.note,
                onClick = { onOpenReader(book.id, activeLang, bm.chapterIndex, bm.paragraphIndex) },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
            }
          }
        }
        2 -> {
          val langHighlights = highlights.filter { it.languageCode == activeLang }
          if (langHighlights.isEmpty()) {
            item { EmptyTabMessage("No highlights yet. Long-press any paragraph in the Reader.") }
          } else {
            items(langHighlights, key = { it.id }) { hl ->
              AnnotationFlatRow(
                header = "Chapter ${hl.chapterIndex + 1} · Para ${hl.paragraphIndex + 1}",
                body = hl.text,
                note = hl.note,
                onClick = { onOpenReader(book.id, activeLang, hl.chapterIndex, hl.paragraphIndex) },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
            }
          }
        }
        3 -> {
          val langNotes = notes.filter { it.languageCode == activeLang }
          if (langNotes.isEmpty()) {
            item { EmptyTabMessage("No reading notes yet. Long-press a paragraph while reading to add a note.") }
          } else {
            items(langNotes, key = { it.id }) { note ->
              AnnotationFlatRow(
                header = note.title,
                body = note.content,
                note = "Chapter ${note.chapterIndex + 1} · Para ${note.paragraphIndex + 1}",
                onClick = { onOpenReader(book.id, activeLang, note.chapterIndex, note.paragraphIndex) },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(64.dp)) }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete '${book.title}'?") },
      text = {
        Text("This permanently deletes the book, chapters, cover, bookmarks, highlights, and notes from device storage.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDeleteBook(book.id)
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
      },
    )
  }
}

internal fun buildBodyChapterNumbers(chapters: List<com.example.model.ChapterMeta>): Map<String, Int> {
  var bodyNum = 0
  val map = mutableMapOf<String, Int>()
  chapters.forEach { ch ->
    if (ch.role == SectionRole.CHAPTER) {
      bodyNum++
      map[ch.id] = bodyNum
    }
  }
  return map
}

@Composable
private fun EmptyTabMessage(message: String) {
  Text(
    text = message,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(vertical = 24.dp),
  )
}

@Composable
private fun AnnotationFlatRow(
  header: String,
  body: String,
  note: String,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
  ) {
    Text(
      text = header,
      fontFamily = ManropeFamily,
      fontSize = 11.sp,
      letterSpacing = 0.6.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = body,
      fontFamily = LiterataFamily,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (note.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = note,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
