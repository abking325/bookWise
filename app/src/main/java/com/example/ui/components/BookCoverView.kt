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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Book
import com.example.model.BookEdition
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.NotoSerifEthiopicFamily
import java.io.File

const val BOOK_COVER_ASPECT_RATIO = 2f / 3f

@Composable
fun BookCoverView(
  book: Book,
  edition: BookEdition? = book.edition(book.defaultLanguageCode),
  modifier: Modifier = Modifier,
  showThickness: Boolean = true,
  elevation: Dp = 8.dp,
  cornerRadius: Dp = 4.dp,
  spineEdgeFraction: Float = 0f,
  spineOnRightSide: Boolean = false,
) {
  val shape =
    RoundedCornerShape(
      topStart = 2.dp,
      bottomStart = 2.dp,
      topEnd = cornerRadius,
      bottomEnd = cornerRadius,
    )
  val pageEdgeWidth = if (showThickness) 5.dp else 0.dp
  val baseAccent =
    remember(edition?.accentColorHex, book.id) {
      parseAccentColor(edition?.accentColorHex, book.id)
    }
  val spineInk =
    remember(baseAccent) {
      if (baseAccent.luminance() > 0.42f) Color(0xFF191919) else Color(0xFFF6F2EB)
    }

  Box(
    modifier = modifier.aspectRatio(BOOK_COVER_ASPECT_RATIO),
  ) {
    // Realistic cream paper page block edge peeking behind front cover
    if (showThickness) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(top = 2.dp, bottom = 2.dp, start = 3.dp)
            .clip(RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFFDCD6CA), Color(0xFFF6F3EC), Color(0xFFE7E2D6))
              )
            )
            .border(
              0.5.dp,
              Color(0xFFC5BFB2),
              RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius),
            )
      )
    }

    // Front cover surface
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(end = pageEdgeWidth)
          .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.22f),
            spotColor = Color.Black.copy(alpha = 0.26f),
          )
          .clip(shape)
          .background(baseAccent)
          .border(0.5.dp, Color.Black.copy(alpha = 0.08f), shape)
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
          EditorialTypographicCover(book = book, bgColor = baseAccent, inkColor = spineInk)
        }
      }

      // Delicate physical spine crease along the left binding edge
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .width(8.dp)
            .background(
              Brush.horizontalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.20f),
                  Color.White.copy(alpha = 0.08f),
                  Color.Transparent,
                )
              )
            )
      )

      // Outer-book angled spine ribbon on turntable rotation
      if (spineEdgeFraction > 0.06f) {
        val spineAlign = if (spineOnRightSide) Alignment.CenterEnd else Alignment.CenterStart
        val ribbonWidth = (24f * spineEdgeFraction.coerceIn(0f, 1f)).dp.coerceAtLeast(14.dp)
        Box(
          modifier =
            Modifier.align(spineAlign)
              .fillMaxHeight()
              .width(ribbonWidth)
              .background(
                Brush.horizontalGradient(
                  listOf(
                    baseAccent.copy(alpha = 0.96f),
                    baseAccent.copy(alpha = 0.88f),
                  )
                )
              )
              .border(0.4.dp, spineInk.copy(alpha = 0.20f)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = book.title,
            fontFamily =
              if (edition?.languageCode == "am") NotoSerifEthiopicFamily else LiterataFamily,
            color = spineInk,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(120.dp).rotate(-90f),
          )
        }
      }
    }
  }
}

@Composable
private fun EditorialTypographicCover(book: Book, bgColor: Color, inkColor: Color) {
  val isAm = book.defaultLanguageCode == "am"
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(bgColor)
        .padding(11.dp)
        .border(0.75.dp, Color(0xFFDF8154).copy(alpha = 0.55f), RoundedCornerShape(2.dp))
        .padding(9.dp),
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
          color = Color(0xFFDF8154),
          fontSize = 7.5.sp,
          letterSpacing = 1.1.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column {
        Text(
          text = book.title,
          fontFamily = if (isAm) NotoSerifEthiopicFamily else BodoniModaFamily,
          color = inkColor,
          fontSize = 14.sp,
          lineHeight = 18.sp,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
        if (book.subtitle.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = book.subtitle,
            fontFamily = if (isAm) NotoSerifEthiopicFamily else LiterataFamily,
            color = inkColor.copy(alpha = 0.76f),
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text = book.author,
        style = MaterialTheme.typography.labelSmall,
        color = inkColor.copy(alpha = 0.85f),
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun parseAccentColor(hex: String?, bookId: String): Color {
  if (!hex.isNullOrBlank()) {
    val parsed = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    if (parsed != null) return parsed
  }
  val palette =
    listOf(
      Color(0xFF1A2433),
      Color(0xFF2B2420),
      Color(0xFF1E2D28),
      Color(0xFF332124),
      Color(0xFF232634),
    )
  return palette[kotlin.math.abs(bookId.hashCode()) % palette.size]
}

private fun resolveBundledDrawable(key: String?): Int? =
  when (key) {
    "img_cover_meditations" -> R.drawable.img_cover_meditations
    "img_cover_art_of_war" -> R.drawable.img_cover_art_of_war
    "img_cover_wisdom_bilingual" -> R.drawable.img_cover_wisdom_bilingual
    else -> null
  }
