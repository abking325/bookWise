package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.UiStrings
import com.example.model.AppSettings
import com.example.model.Book
import com.example.model.CategoryItem
import com.example.model.ReadingStatus
import com.example.ui.components.BookCover3DCarousel
import com.example.ui.components.BookCoverView
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.PlayfairDisplayFamily

@Composable
fun HomeScreen(
  books: List<Book>,
  categories: List<CategoryItem>,
  settings: AppSettings,
  strings: UiStrings,
  onOpenBookDetails: (Book) -> Unit,
  onReadBook: (Book) -> Unit,
  onOpenAddBook: () -> Unit,
  onOpenCategory: (String) -> Unit,
  onImportBackup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val continueReading =
    remember(books) {
      books
        .filter { it.readingStatus == ReadingStatus.READING || it.primaryProgress > 0.01f }
        .sortedByDescending { it.lastReadAt }
    }
  val recentlyAdded = remember(books) { books.sortedByDescending { it.createdAt }.take(10) }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("home_screen"),
    contentPadding = PaddingValues(bottom = 96.dp),
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = strings.appTitle,
              fontFamily = PlayfairDisplayFamily,
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = strings.offlineBadge,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
          OutlinedButton(
            onClick = onOpenAddBook,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp).testTag("home_add_book_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = strings.addBook,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.addBook)
          }
        }
      }
    }

    if (books.isEmpty()) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoStories,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = strings.noBooksTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = strings.noBooksHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenAddBook, modifier = Modifier.height(48.dp)) {
              Text(strings.addBook)
            }
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.height(48.dp)) {
              Text("Import BookWise")
            }
          }
        }
      }
    } else {
      item {
        BookCover3DCarousel(
          books = books.take(12),
          showSpineThickness = settings.carouselShowSpineThickness,
          depthEnabled = settings.carouselDepthEnabled,
          readButtonLabel = strings.readNow,
          continueButtonLabel = strings.continueReading,
          onOpenBookDetails = onOpenBookDetails,
          onReadBook = onReadBook,
          modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
        )
      }
    }

    if (continueReading.isNotEmpty()) {
      item {
        SectionDividerHeader(title = strings.continueReading)
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          items(continueReading, key = { it.id }) { book ->
            ContinueReadingCard(
              book = book,
              onClick = { onReadBook(book) },
              onLongInspect = { onOpenBookDetails(book) },
            )
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    if (recentlyAdded.isNotEmpty()) {
      item {
        SectionDividerHeader(title = strings.recentlyAdded)
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          items(recentlyAdded, key = { it.id }) { book ->
            ShelfBookItem(
              book = book,
              onClick = { onOpenBookDetails(book) },
            )
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    if (categories.isNotEmpty()) {
      item {
        SectionDividerHeader(title = strings.organizedCategories)
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(categories, key = { it.id }) { cat ->
            val count = books.count { it.category.equals(cat.name, ignoreCase = true) }
            Surface(
              onClick = { onOpenCategory(cat.name) },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border =
                androidx.compose.foundation.BorderStroke(
                  1.dp,
                  MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier = Modifier.height(48.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.BookmarkBorder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = cat.name,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "$count",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionDividerHeader(title: String) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = title,
      fontFamily = PlayfairDisplayFamily,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
private fun ContinueReadingCard(
  book: Book,
  onClick: () -> Unit,
  onLongInspect: () -> Unit,
) {
  val edition = book.edition(book.defaultLanguageCode)
  val progress = edition?.progress?.overallProgress ?: 0f
  val pct = (progress * 100).toInt().coerceIn(0, 100)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border =
      androidx.compose.foundation.BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.width(268.dp).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCoverView(
        book = book,
        showThickness = true,
        elevation = 4.dp,
        modifier = Modifier.width(64.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
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
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { progress.coerceIn(0.02f, 1f) },
          modifier = Modifier.fillMaxWidth().height(4.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "$pct% read",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Details",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable(onClick = onLongInspect).padding(4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ShelfBookItem(
  book: Book,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier.width(114.dp).clickable(onClick = onClick),
  ) {
    BookCoverView(
      book = book,
      showThickness = true,
      elevation = 6.dp,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = book.title,
      style = MaterialTheme.typography.titleSmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = book.author,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
