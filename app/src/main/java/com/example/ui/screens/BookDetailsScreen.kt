package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.BookNote
import com.example.model.Bookmark
import com.example.model.Highlight
import com.example.model.LanguageCode
import com.example.model.ReadingStatus
import com.example.ui.components.BookCoverView
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.PlayfairDisplayFamily

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
  var showDeleteConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(book.id, activeLang) {
    onLoadAnnotations(book.id, activeLang)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(book.bookType.labelEn, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onToggleFavorite(book.id) }) {
            Icon(
              imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = "Favorite",
              tint =
                if (book.isFavorite) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(onClick = { onEditEdition(book.id, activeLang) }) {
            Icon(Icons.Filled.Edit, contentDescription = strings.editBook)
          }
          IconButton(onClick = { onExportBookZip(book) }) {
            Icon(Icons.Filled.Archive, contentDescription = strings.exportBookZip)
          }
          IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = strings.deleteBook,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        },
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top,
        ) {
          BookCoverView(
            book = book,
            edition = edition,
            showThickness = true,
            elevation = 10.dp,
            modifier = Modifier.width(128.dp),
          )
          Spacer(modifier = Modifier.width(18.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = book.title,
              fontFamily = PlayfairDisplayFamily,
              style = MaterialTheme.typography.headlineSmall,
            )
            if (book.subtitle.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = book.subtitle,
                fontFamily = LoraFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = book.author,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val metaLine =
              listOfNotNull(
                  book.category.takeIf { it.isNotBlank() },
                  book.subcategory.takeIf { it.isNotBlank() },
                  book.publicationYear.takeIf { it.isNotBlank() },
                )
                .joinToString(" • ")
            Text(
              text = metaLine,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Language Editions Switcher (English & Amharic)
        Text(
          text = "Language Editions",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          book.editions.keys.forEach { code ->
            FilterChip(
              selected = activeLang == code,
              onClick = { selectedLang = code },
              label = { Text(LanguageCode.labelFor(code)) },
            )
          }
          val missingOther =
            when {
              !book.editions.containsKey("am") -> "am"
              !book.editions.containsKey("en") -> "en"
              else -> null
            }
          if (missingOther != null) {
            OutlinedButton(
              onClick = { onAddMissingEdition(book.id, missingOther) },
              shape = RoundedCornerShape(50),
              modifier = Modifier.height(38.dp),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add ${LanguageCode.labelFor(missingOther)}")
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        val progress = edition?.progress?.overallProgress ?: 0f
        if (progress > 0.01f) {
          LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${(progress * 100).toInt()}% completed in ${LanguageCode.labelFor(activeLang)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
          onClick = {
            val savedProg = edition?.progress
            onOpenReader(
              book.id,
              activeLang,
              savedProg?.chapterIndex ?: 0,
              savedProg?.paragraphIndex ?: 0,
            )
          },
          shape = RoundedCornerShape(50),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("details_read_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (progress > 0.01f) strings.continueReading else strings.readNow,
            style = MaterialTheme.typography.titleSmall,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        // Reading status selector
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReadingStatus.entries.forEach { st ->
            FilterChip(
              selected = book.readingStatus == st,
              onClick = { onSetStatus(book.id, st) },
              label = { Text(st.labelEn) },
            )
          }
        }

        if (book.description.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = book.description,
            fontFamily = LoraFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        if (book.personalNotes.isNotBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Personal Book Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = book.personalNotes, style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
        val tabs =
          listOf(
            "${strings.tableOfContents} (${edition?.chapters?.size ?: 0})",
            "${strings.bookmarksLabel} (${bookmarks.count { it.languageCode == activeLang }})",
            "${strings.highlightsLabel} (${highlights.count { it.languageCode == activeLang }})",
            "${strings.notesLabel} (${notes.count { it.languageCode == activeLang }})",
          )
        TabRow(selectedTabIndex = selectedTab) {
          tabs.forEachIndexed { idx, label ->
            Tab(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              text = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      when (selectedTab) {
        0 -> {
          val chapters = edition?.chapters.orEmpty()
          items(chapters, key = { it.id }) { ch ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onOpenReader(book.id, activeLang, ch.index, 0) }
                  .padding(vertical = 12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = ch.title,
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${ch.estimatedMinutes} min",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
          }
        }
        1 -> {
          val langBookmarks = bookmarks.filter { it.languageCode == activeLang }
          if (langBookmarks.isEmpty()) {
            item { EmptyTabMessage("No bookmarks yet. Tap the bookmark icon while reading.") }
          } else {
            items(langBookmarks, key = { it.id }) { bm ->
              AnnotationRow(
                header = bm.chapterTitle,
                body = bm.excerpt,
                note = bm.note,
                onClick = { onOpenReader(book.id, activeLang, bm.chapterIndex, bm.paragraphIndex) },
              )
            }
          }
        }
        2 -> {
          val langHighlights = highlights.filter { it.languageCode == activeLang }
          if (langHighlights.isEmpty()) {
            item { EmptyTabMessage("No highlights yet. Long-press any paragraph in the Reader.") }
          } else {
            items(langHighlights, key = { it.id }) { hl ->
              AnnotationRow(
                header = "Chapter ${hl.chapterIndex + 1} • Para ${hl.paragraphIndex + 1}",
                body = hl.text,
                note = hl.note,
                onClick = { onOpenReader(book.id, activeLang, hl.chapterIndex, hl.paragraphIndex) },
              )
            }
          }
        }
        3 -> {
          val langNotes = notes.filter { it.languageCode == activeLang }
          if (langNotes.isEmpty()) {
            item { EmptyTabMessage("No reading notes yet. Long-press a paragraph while reading to add a note.") }
          } else {
            items(langNotes, key = { it.id }) { note ->
              AnnotationRow(
                header = note.title,
                body = note.content,
                note = "Chapter ${note.chapterIndex + 1} • Para ${note.paragraphIndex + 1}",
                onClick = { onOpenReader(book.id, activeLang, note.chapterIndex, note.paragraphIndex) },
              )
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

@Composable
private fun EmptyTabMessage(message: String) {
  Text(
    text = message,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(vertical = 20.dp),
  )
}

@Composable
private fun AnnotationRow(
  header: String,
  body: String,
  note: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = header,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = body,
        fontFamily = LoraFamily,
        style = MaterialTheme.typography.bodyMedium,
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
}
