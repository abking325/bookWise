package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Book
import com.example.model.BookEdition
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.PlayfairDisplayFamily
import java.io.File

const val BOOK_COVER_ASPECT_RATIO = 2f / 3f

@Composable
fun BookCoverView(
  book: Book,
  edition: BookEdition? = book.edition(book.defaultLanguageCode),
  modifier: Modifier = Modifier,
  showThickness: Boolean = true,
  elevation: Dp = 8.dp,
  cornerRadius: Dp = 6.dp,
) {
  val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = cornerRadius, bottomEnd = cornerRadius)
  val pageEdgeWidth = if (showThickness) 6.dp else 0.dp

  Box(
    modifier = modifier.aspectRatio(BOOK_COVER_ASPECT_RATIO),
  ) {
    // Realistic paper page block edge peeking behind the front cover
    if (showThickness) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(top = 3.dp, bottom = 3.dp, start = 4.dp)
            .clip(RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFFDAD4C8), Color(0xFFF3EFE6), Color(0xFFE4DED1))
              )
            )
            .border(
              0.5.dp,
              Color(0xFFBFB8AA),
              RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius),
            )
      )
    }

    // Front cover surface
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(end = pageEdgeWidth)
          .shadow(elevation = elevation, shape = shape, clip = false)
          .clip(shape)
          .background(parseAccentColor(edition?.accentColorHex))
    ) {
      val coverFilePath = edition?.coverImagePath
      val bundledKey = edition?.bundledCoverKey
      val bundledResId = remember(bundledKey) { resolveBundledDrawable(bundledKey) }

      when {
        !coverFilePath.isNullOrBlank() && File(coverFilePath).exists() -> {
          AsyncImage(
            model =
              ImageRequest.Builder(LocalContext.current)
                .data(File(coverFilePath))
                .crossfade(true)
                .build(),
            contentDescription = "${book.title} cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        bundledResId != null -> {
          Image(
            painter = painterResource(id = bundledResId),
            contentDescription = "${book.title} cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        else -> {
          EditorialTypographicCover(book = book)
        }
      }

      // Subtle physical spine crease highlight along the left binding edge
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .width(10.dp)
            .background(
              Brush.horizontalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.24f),
                  Color.White.copy(alpha = 0.10f),
                  Color.Transparent,
                )
              )
            )
      )
    }
  }
}

@Composable
private fun EditorialTypographicCover(book: Book) {
  val bgColor = remember(book.id) {
    val palette =
      listOf(
        Color(0xFF1A2433),
        Color(0xFF2B2420),
        Color(0xFF1E2D28),
        Color(0xFF332124),
        Color(0xFF232634),
      )
    palette[kotlin.math.abs(book.id.hashCode()) % palette.size]
  }
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(bgColor)
        .padding(12.dp)
        .border(0.75.dp, Color(0xFFC9A25E).copy(alpha = 0.55f), RoundedCornerShape(3.dp))
        .padding(10.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = book.category.uppercase(),
          color = Color(0xFFC9A25E),
          fontSize = 8.sp,
          letterSpacing = 1.1.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column {
        Text(
          text = book.title,
          fontFamily = PlayfairDisplayFamily,
          color = Color(0xFFFAF7F2),
          fontSize = 15.sp,
          lineHeight = 19.sp,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
        if (book.subtitle.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = book.subtitle,
            fontFamily = LoraFamily,
            color = Color(0xFFD6CFC3),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text = book.author,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE5DFD3),
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun parseAccentColor(hex: String?): Color {
  if (hex.isNullOrBlank()) return Color(0xFF1C2534)
  return runCatching { Color(android.graphics.Color.parseColor(hex)) }
    .getOrDefault(Color(0xFF1C2534))
}

private fun resolveBundledDrawable(key: String?): Int? =
  when (key) {
    "img_cover_meditations" -> R.drawable.img_cover_meditations
    "img_cover_art_of_war" -> R.drawable.img_cover_art_of_war
    "img_cover_wisdom_bilingual" -> R.drawable.img_cover_wisdom_bilingual
    else -> null
  }
