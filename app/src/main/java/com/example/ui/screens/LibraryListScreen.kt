package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.LibraryFilter
import com.example.model.ReadingStatus
import com.example.model.SortOption
import com.example.ui.components.BookCoverView
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily

@Composable
fun LibraryListScreen(
  screenTitle: String,
  bookType: BookType,
  books: List<Book>,
  categories: List<CategoryItem>,
  filter: LibraryFilter,
  strings: UiStrings,
  onFilterChange: (LibraryFilter) -> Unit,
  onOpenBookDetails: (Book) -> Unit,
  onReadBook: (Book) -> Unit,
  onEditBook: (Book) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onExportBookZip: (Book) -> Unit,
  onDeleteBook: (Book) -> Unit,
  onAddBook: (BookType) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }
  var isGridView by remember { mutableStateOf(true) }
  var deletingBook by remember { mutableStateOf<Book?>(null) }

  Column(modifier = modifier.fillMaxSize()) {
    // Editorial Header + View/Sort/Add Controls
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = screenTitle,
          fontFamily = BodoniModaFamily,
          fontSize = 28.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Normal,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.weight(1f),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { isGridView = !isGridView },
            modifier = Modifier.size(48.dp).testTag("library_view_toggle"),
          ) {
            Icon(
              imageVector =
                if (isGridView) Icons.AutoMirrored.Outlined.ViewList
                else Icons.Outlined.GridView,
              contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }

          Box {
            IconButton(
              onClick = { showSortMenu = true },
              modifier = Modifier.size(48.dp).testTag("library_sort_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = strings.sortBy,
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
              SortOption.entries.forEach { opt ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = opt.labelEn,
                      fontWeight = if (opt == filter.sortOption) FontWeight.SemiBold else FontWeight.Normal,
                    )
                  },
                  onClick = {
                    onFilterChange(filter.copy(sortOption = opt))
                    showSortMenu = false
                  },
                )
              }
            }
          }

          Surface(
            onClick = { onAddBook(bookType) },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp).height(40.dp).testTag("library_add_fab"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp),
            ) {
              Icon(
                Icons.Filled.Add,
                contentDescription = strings.addBook,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Add",
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = filter.searchQuery,
        onValueChange = { onFilterChange(filter.copy(searchQuery = it)) },
        placeholder = {
          Text(
            text = strings.searchBooksPlaceholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        leadingIcon = {
          Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        },
        trailingIcon = {
          if (filter.searchQuery.isNotEmpty()) {
            IconButton(onClick = { onFilterChange(filter.copy(searchQuery = "")) }) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp).testTag("library_search_input"),
      )
    }

    // Minimal Hairline Filter Strip
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (filter.favoritesOnly ||
        filter.languageCode != null ||
        filter.readingStatus != null ||
        filter.category != null
      ) {
        item {
          EditorialFilterTag(
            label = "Reset",
            selected = false,
            onClick = { onFilterChange(LibraryFilter(searchQuery = filter.searchQuery, sortOption = filter.sortOption)) },
          )
        }
      }
      item {
        EditorialFilterTag(
          label = strings.favoritesOnly,
          selected = filter.favoritesOnly,
          onClick = { onFilterChange(filter.copy(favoritesOnly = !filter.favoritesOnly)) },
        )
      }
      item {
        EditorialFilterTag(
          label = "English",
          selected = filter.languageCode == "en",
          onClick = {
            onFilterChange(filter.copy(languageCode = if (filter.languageCode == "en") null else "en"))
          },
        )
      }
      item {
        EditorialFilterTag(
          label = "አማርኛ",
          selected = filter.languageCode == "am",
          onClick = {
            onFilterChange(filter.copy(languageCode = if (filter.languageCode == "am") null else "am"))
          },
        )
      }
      item {
        EditorialFilterTag(
          label = "Reading",
          selected = filter.readingStatus == ReadingStatus.READING,
          onClick = {
            val next =
              if (filter.readingStatus == ReadingStatus.READING) null else ReadingStatus.READING
            onFilterChange(filter.copy(readingStatus = next))
          },
        )
      }
      item {
        EditorialFilterTag(
          label = "Completed",
          selected = filter.readingStatus == ReadingStatus.COMPLETED,
          onClick = {
            val next =
              if (filter.readingStatus == ReadingStatus.COMPLETED) null else ReadingStatus.COMPLETED
            onFilterChange(filter.copy(readingStatus = next))
          },
        )
      }
      items(categories, key = { it.id }) { cat ->
        val selected = filter.category.equals(cat.name, ignoreCase = true)
        EditorialFilterTag(
          label = cat.name,
          selected = selected,
          onClick = { onFilterChange(filter.copy(category = if (selected) null else cat.name)) },
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

    if (books.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = strings.noBooksTitle,
            fontFamily = BodoniModaFamily,
            style = MaterialTheme.typography.headlineSmall,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = strings.noBooksHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else if (isGridView) {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize().testTag("library_grid_view"),
      ) {
        items(books, key = { it.id }) { book ->
          LibraryBookGridItem(
            book = book,
            strings = strings,
            onOpenDetails = { onOpenBookDetails(book) },
            onRead = { onReadBook(book) },
            onEdit = { onEditBook(book) },
            onToggleFavorite = { onToggleFavorite(book.id) },
            onExportZip = { onExportBookZip(book) },
            onRequestDelete = { deletingBook = book },
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 104.dp),
        modifier = Modifier.fillMaxSize().testTag("library_list_view"),
      ) {
        items(books, key = { it.id }) { book ->
          LibraryBookFlatRow(
            book = book,
            strings = strings,
            onOpenDetails = { onOpenBookDetails(book) },
            onRead = { onReadBook(book) },
            onEdit = { onEditBook(book) },
            onToggleFavorite = { onToggleFavorite(book.id) },
            onExportZip = { onExportBookZip(book) },
            onRequestDelete = { deletingBook = book },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
        }
      }
    }
  }

  deletingBook?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingBook = null },
      title = { Text("Delete '${target.title}'?") },
      text = {
        Text("This permanently deletes the book, chapters, cover, bookmarks, highlights, and notes from device storage.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteBook(target)
            deletingBook = null
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingBook = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun EditorialFilterTag(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color =
      if (selected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.background,
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

@Composable
private fun LibraryBookGridItem(
  book: Book,
  strings: UiStrings,
  onOpenDetails: () -> Unit,
  onRead: () -> Unit,
  onEdit: () -> Unit,
  onToggleFavorite: () -> Unit,
  onExportZip: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  val edition = book.edition(book.defaultLanguageCode)
  val progress = edition?.progress?.overallProgress ?: 0f
  var showOverflow by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetails),
  ) {
    BookCoverView(
      book = book,
      showThickness = true,
      elevation = 7.dp,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = book.category.uppercase(),
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.0.sp,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = book.title,
          fontFamily = LiterataFamily,
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = book.author,
          fontFamily = ManropeFamily,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box {
        IconButton(
          onClick = { showOverflow = true },
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            Icons.Filled.MoreVert,
            contentDescription = "Book actions",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
        BookActionsDropdown(
          expanded = showOverflow,
          book = book,
          strings = strings,
          onDismiss = { showOverflow = false },
          onOpenDetails = onOpenDetails,
          onRead = onRead,
          onEdit = onEdit,
          onToggleFavorite = onToggleFavorite,
          onExportZip = onExportZip,
          onDelete = onRequestDelete,
        )
      }
    }

    if (progress > 0.01f) {
      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.fillMaxWidth().height(2.dp),
      )
    }
  }
}

@Composable
private fun LibraryBookFlatRow(
  book: Book,
  strings: UiStrings,
  onOpenDetails: () -> Unit,
  onRead: () -> Unit,
  onEdit: () -> Unit,
  onToggleFavorite: () -> Unit,
  onExportZip: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  val edition = book.edition(book.defaultLanguageCode)
  val progress = edition?.progress?.overallProgress ?: 0f
  var showOverflow by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetails).padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BookCoverView(
      book = book,
      showThickness = true,
      elevation = 5.dp,
      modifier = Modifier.width(66.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      val langLabel = book.availableLanguages.joinToString(" / ") { if (it == "am") "አማርኛ" else it.uppercase() }
      Text(
        text = "${book.category.uppercase()} · $langLabel",
        fontFamily = ManropeFamily,
        fontSize = 10.sp,
        letterSpacing = 1.0.sp,
        color = MaterialTheme.colorScheme.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = book.title,
        fontFamily = LiterataFamily,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = book.author,
        fontFamily = ManropeFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(6.dp))
      if (progress > 0.01f) {
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.outlineVariant,
          modifier = Modifier.fillMaxWidth().height(2.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
      }
      val chCount = edition?.chapters?.size ?: 0
      val progressLabel = if (progress > 0.01f) "${(progress * 100).toInt()}% read · " else ""
      Text(
        text = "$progressLabel$chCount sections",
        fontFamily = ManropeFamily,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Box {
      IconButton(
        onClick = { showOverflow = true },
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          Icons.Filled.MoreVert,
          contentDescription = "Book actions",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      BookActionsDropdown(
        expanded = showOverflow,
        book = book,
        strings = strings,
        onDismiss = { showOverflow = false },
        onOpenDetails = onOpenDetails,
        onRead = onRead,
        onEdit = onEdit,
        onToggleFavorite = onToggleFavorite,
        onExportZip = onExportZip,
        onDelete = onRequestDelete,
      )
    }
  }
}

@Composable
private fun BookActionsDropdown(
  expanded: Boolean,
  book: Book,
  strings: UiStrings,
  onDismiss: () -> Unit,
  onOpenDetails: () -> Unit,
  onRead: () -> Unit,
  onEdit: () -> Unit,
  onToggleFavorite: () -> Unit,
  onExportZip: () -> Unit,
  onDelete: () -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    DropdownMenuItem(
      text = { Text("Open") },
      onClick = {
        onDismiss()
        onRead()
      },
    )
    DropdownMenuItem(
      text = { Text(strings.discoverDetails) },
      onClick = {
        onDismiss()
        onOpenDetails()
      },
    )
    DropdownMenuItem(
      text = { Text(strings.editBook) },
      onClick = {
        onDismiss()
        onEdit()
      },
    )
    DropdownMenuItem(
      text = { Text(if (book.isFavorite) "Remove Favorite" else "Add to Favorites") },
      onClick = {
        onDismiss()
        onToggleFavorite()
      },
    )
    DropdownMenuItem(
      text = { Text(strings.exportBookZip) },
      onClick = {
        onDismiss()
        onExportZip()
      },
    )
    DropdownMenuItem(
      text = { Text(strings.deleteBook, color = MaterialTheme.colorScheme.error) },
      onClick = {
        onDismiss()
        onDelete()
      },
    )
  }
}
