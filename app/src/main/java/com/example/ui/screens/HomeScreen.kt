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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.UiStrings
import com.example.model.AppSettings
import com.example.model.Book
import com.example.model.CategoryItem
import com.example.model.ReadingStatus
import com.example.ui.components.BookCover3DCarousel
import com.example.ui.components.BookCoverView
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily

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
  onViewAllBooks: () -> Unit,
  onViewAllCategories: () -> Unit,
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
    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
  ) {
    if (books.isEmpty()) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 56.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoStories,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(44.dp),
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = strings.noBooksTitle,
            fontFamily = BodoniModaFamily,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = strings.noBooksHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(22.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
              onClick = onOpenAddBook,
              shape = RoundedCornerShape(50),
              modifier = Modifier.height(48.dp),
            ) {
              Text(strings.addBook)
            }
            OutlinedButton(
              onClick = onImportBackup,
              shape = RoundedCornerShape(50),
              modifier = Modifier.height(48.dp),
            ) {
              Text("Import BookWise")
            }
          }
        }
      }
    } else {
      // Section C & D: Hero 3D Turntable Carousel + Featured Book Info
      item(key = "hero_carousel") {
        BookCover3DCarousel(
          books = books.take(12),
          showSpineThickness = settings.carouselShowSpineThickness,
          depthEnabled = settings.carouselDepthEnabled,
          readButtonLabel = strings.readNow,
          continueButtonLabel = strings.continueReading,
          detailsButtonLabel = strings.discoverDetails,
          onOpenBookDetails = onOpenBookDetails,
          onReadBook = onReadBook,
          modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
        )
      }
    }

    // Section E: Continue Reading — clean flat borderless editorial shelf
    if (continueReading.isNotEmpty()) {
      item(key = "continue_reading_section") {
        EditorialSectionHeader(
          title = strings.continueReading,
          actionLabel = strings.viewAll,
          onActionClick = onViewAllBooks,
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
          items(continueReading, key = { it.id }) { book ->
            EditorialContinueReadingItem(
              book = book,
              onReadClick = { onReadBook(book) },
              onDetailsClick = { onOpenBookDetails(book) },
            )
          }
        }
        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    // Section F: Recently Added — pure gallery cover row without boxed containers
    if (recentlyAdded.isNotEmpty()) {
      item(key = "recently_added_section") {
        EditorialSectionHeader(
          title = strings.recentlyAdded,
          actionLabel = strings.viewAll,
          onActionClick = onViewAllBooks,
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          items(recentlyAdded, key = { it.id }) { book ->
            EditorialGalleryBookItem(
              book = book,
              onClick = { onOpenBookDetails(book) },
            )
          }
        }
        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    // Section G: Categories — clean luxury bookstore index rows with hairline dividers
    if (categories.isNotEmpty()) {
      item(key = "categories_header") {
        EditorialSectionHeader(
          title = strings.organizedCategories,
          actionLabel = strings.viewAll,
          onActionClick = onViewAllCategories,
        )
      }
      items(categories, key = { it.id }) { cat ->
        val count = books.count { it.category.equals(cat.name, ignoreCase = true) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onOpenCategory(cat.name) }
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = cat.name,
              fontFamily = BodoniModaFamily,
              fontSize = 18.sp,
              fontWeight = FontWeight.Normal,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (count == 1) "1 title" else "$count titles",
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
        }
      }
    }
  }
}

@Composable
private fun EditorialSectionHeader(
  title: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
    Spacer(modifier = Modifier.height(18.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        fontFamily = BodoniModaFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (actionLabel != null && onActionClick != null) {
        TextButton(
          onClick = onActionClick,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
          Text(
            text = actionLabel,
            fontFamily = ManropeFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(14.dp))
  }
}

@Composable
private fun EditorialContinueReadingItem(
  book: Book,
  onReadClick: () -> Unit,
  onDetailsClick: () -> Unit,
) {
  val edition = book.edition(book.defaultLanguageCode)
  val progress = edition?.progress?.overallProgress ?: 0f
  val pct = (progress * 100).toInt().coerceIn(0, 100)
  val chIdx = (edition?.progress?.chapterIndex ?: 0) + 1

  Row(
    modifier = Modifier.width(248.dp).clickable(onClick = onReadClick).padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BookCoverView(
      book = book,
      showThickness = true,
      elevation = 5.dp,
      modifier = Modifier.width(66.dp).clickable(onClick = onDetailsClick),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
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
      Spacer(modifier = Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { progress.coerceIn(0.02f, 1f) },
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.fillMaxWidth().height(2.dp),
      )
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = "Ch $chIdx · $pct% read",
        fontFamily = ManropeFamily,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EditorialGalleryBookItem(
  book: Book,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier.width(108.dp).clickable(onClick = onClick),
  ) {
    BookCoverView(
      book = book,
      showThickness = true,
      elevation = 6.dp,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = book.title,
      fontFamily = LiterataFamily,
      fontSize = 14.sp,
      lineHeight = 19.sp,
      fontWeight = FontWeight.Normal,
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
}
