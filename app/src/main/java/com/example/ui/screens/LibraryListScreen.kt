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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.localization.UiStrings
import com.example.model.Book
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.LibraryFilter
import com.example.model.ReadingStatus
import com.example.model.SortOption
import com.example.ui.components.BookCoverView
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.PlayfairDisplayFamily

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
  onToggleFavorite: (String) -> Unit,
  onAddBook: (BookType) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header & Search
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = screenTitle,
            fontFamily = PlayfairDisplayFamily,
            style = MaterialTheme.typography.headlineMedium,
          )
          Box {
            IconButton(
              onClick = { showSortMenu = true },
              modifier = Modifier.size(48.dp).testTag("library_sort_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = strings.sortBy)
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
              SortOption.entries.forEach { opt ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = opt.labelEn,
                      fontWeight = if (opt == filter.sortOption) FontWeight.Bold else FontWeight.Normal,
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = filter.searchQuery,
          onValueChange = { onFilterChange(filter.copy(searchQuery = it)) },
          placeholder = { Text(strings.searchBooksPlaceholder) },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (filter.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onFilterChange(filter.copy(searchQuery = "")) }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
        )
      }

      // Filter strip (Favorites, Language, Status, Category)
      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          FilterChip(
            selected = filter.favoritesOnly,
            onClick = { onFilterChange(filter.copy(favoritesOnly = !filter.favoritesOnly)) },
            label = { Text(strings.favoritesOnly) },
            leadingIcon = {
              Icon(
                imageVector = if (filter.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
          )
        }
        item {
          FilterChip(
            selected = filter.languageCode == "en",
            onClick = {
              onFilterChange(filter.copy(languageCode = if (filter.languageCode == "en") null else "en"))
            },
            label = { Text("English") },
          )
        }
        item {
          FilterChip(
            selected = filter.languageCode == "am",
            onClick = {
              onFilterChange(filter.copy(languageCode = if (filter.languageCode == "am") null else "am"))
            },
            label = { Text("አማርኛ") },
          )
        }
        item {
          FilterChip(
            selected = filter.readingStatus == ReadingStatus.READING,
            onClick = {
              val next =
                if (filter.readingStatus == ReadingStatus.READING) null else ReadingStatus.READING
              onFilterChange(filter.copy(readingStatus = next))
            },
            label = { Text("In Progress") },
          )
        }
        item {
          FilterChip(
            selected = filter.readingStatus == ReadingStatus.COMPLETED,
            onClick = {
              val next =
                if (filter.readingStatus == ReadingStatus.COMPLETED) null
                else ReadingStatus.COMPLETED
              onFilterChange(filter.copy(readingStatus = next))
            },
            label = { Text("Completed") },
          )
        }
        items(categories, key = { it.id }) { cat ->
          val selected = filter.category.equals(cat.name, ignoreCase = true)
          FilterChip(
            selected = selected,
            onClick = {
              onFilterChange(filter.copy(category = if (selected) null else cat.name))
            },
            label = { Text(cat.name) },
          )
        }
      }

      if (books.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = strings.noBooksTitle,
              style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.noBooksHint,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(books, key = { it.id }) { book ->
            LibraryBookRowCard(
              book = book,
              onOpenDetails = { onOpenBookDetails(book) },
              onRead = { onReadBook(book) },
              onToggleFavorite = { onToggleFavorite(book.id) },
            )
          }
        }
      }
    }

    FloatingActionButton(
      onClick = { onAddBook(bookType) },
      modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).testTag("library_add_fab"),
    ) {
      Icon(Icons.Filled.Add, contentDescription = strings.addBook)
    }
  }
}

@Composable
private fun LibraryBookRowCard(
  book: Book,
  onOpenDetails: () -> Unit,
  onRead: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  val edition = book.edition(book.defaultLanguageCode)
  val progress = edition?.progress?.overallProgress ?: 0f

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetails),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCoverView(
        book = book,
        showThickness = true,
        elevation = 5.dp,
        modifier = Modifier.width(74.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = book.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = book.author,
              fontFamily = LoraFamily,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = "Favorite",
              tint =
                if (book.isFavorite) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Text(
              text = book.category,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          book.availableLanguages.forEach { lang ->
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = if (lang == "am") "አማርኛ" else lang.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          val chCount = edition?.chapters?.size ?: 0
          Text(
            text = "$chCount ch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (progress > 0.01f) {
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(3.dp),
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (progress > 0.01f) "Continue Reading ->" else "Read ->",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable(onClick = onRead).padding(vertical = 4.dp),
        )
      }
    }
  }
}
