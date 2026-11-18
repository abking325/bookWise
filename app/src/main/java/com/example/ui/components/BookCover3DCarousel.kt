package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.Book
import com.example.model.LanguageCode
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Editorial 3D Turntable Exhibition Carousel.
 * Standing 2:3 hardcovers orbit cleanly on pure white/charcoal gallery space
 * without decorative circles, boxes, or platform stages.
 */
@Composable
fun BookCover3DCarousel(
  books: List<Book>,
  showSpineThickness: Boolean,
  depthEnabled: Boolean,
  readButtonLabel: String,
  continueButtonLabel: String,
  detailsButtonLabel: String,
  onOpenBookDetails: (Book) -> Unit,
  onReadBook: (Book) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (books.isEmpty()) return

  val scope = rememberCoroutineScope()
  val virtualPosition = remember { Animatable(0f) }

  LaunchedEffect(books.size) {
    if (books.size == 1) {
      virtualPosition.snapTo(0f)
    }
  }

  val centerIndex by remember(books.size) {
    derivedStateOf {
      val n = books.size
      if (n == 0) 0 else ((virtualPosition.value.roundToInt() % n) + n) % n
    }
  }
  val selectedBook = books[centerIndex.coerceIn(books.indices)]

  fun stepBy(delta: Float) {
    scope.launch {
      val target = (virtualPosition.value.roundToInt() + delta).toFloat()
      virtualPosition.animateTo(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
      )
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Section C: Hero 3D Turntable Exhibition (264dp height, central cover 114–134dp width / 2:3 ratio)
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth().height(264.dp),
      contentAlignment = Alignment.Center,
    ) {
      val density = LocalDensity.current
      val maxWidthPx = with(density) { maxWidth.toPx() }
      val cardWidthDp = (maxWidth * 0.31f).coerceIn(112.dp, 134.dp)
      val cardWidthPx = with(density) { cardWidthDp.toPx() }
      val dragUnitPx = (cardWidthPx * 0.92f).coerceAtLeast(100f)
      val orbitRadiusPx = (maxWidthPx * 0.78f).coerceAtLeast(220f)

      val draggableState = rememberDraggableState { deltaPx ->
        scope.launch {
          virtualPosition.snapTo(virtualPosition.value - (deltaPx / dragUnitPx))
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(256.dp)
            .testTag("home_3d_carousel")
            .draggable(
              state = draggableState,
              orientation = Orientation.Horizontal,
              onDragStopped = { velocityPx ->
                scope.launch {
                  val decay = exponentialDecay<Float>(frictionMultiplier = 2.6f)
                  val velocityBooksPerSec = -(velocityPx / dragUnitPx)
                  val target =
                    if (abs(velocityBooksPerSec) > 0.35f) {
                      (virtualPosition.value + (velocityBooksPerSec * 0.24f)).roundToInt().toFloat()
                    } else {
                      virtualPosition.value.roundToInt().toFloat()
                    }
                  if (abs(velocityBooksPerSec) > 0.35f) {
                    virtualPosition.animateDecay(velocityBooksPerSec * 0.4f, decay)
                  }
                  val finalSnap = virtualPosition.value.roundToInt().toFloat()
                  val snappedTarget = if (abs(target - finalSnap) <= 1f) target else finalSnap
                  virtualPosition.animateTo(
                    targetValue = snappedTarget,
                    animationSpec =
                      spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMediumLow,
                      ),
                  )
                }
              },
            ),
        contentAlignment = Alignment.Center,
      ) {
        val n = books.size
        val slots =
          remember(books, virtualPosition.value) {
            val pos = virtualPosition.value
            val list =
              books.mapIndexed { idx, book ->
                var delta = (idx - pos) % n
                if (delta > n / 2f) delta -= n
                if (delta < -n / 2f) delta += n
                idx to (book to delta)
              }
            list.sortedByDescending { abs(it.second.second) }
          }

        slots.forEach { (_, pair) ->
          val (book, rawDelta) = pair
          val absDelta = abs(rawDelta)
          if (absDelta <= 2.75f) {
            val angleRad =
              sign(rawDelta) *
                (0.118f * (1f - exp(-absDelta / 0.48f)) + 0.095f * absDelta) *
                1.78f
            val tx = sin(angleRad) * orbitRadiusPx
            val depthCos = cos(angleRad).coerceIn(0.32f, 1f)
            val scale =
              if (depthEnabled) (0.70f + 0.30f * depthCos)
              else (0.86f + 0.14f * depthCos)
            val rotY = if (depthEnabled) -(rawDelta * 22f).coerceIn(-48f, 48f) else 0f
            val ty = (1f - depthCos) * 42f
            val alpha = (1f - (absDelta * 0.18f)).coerceIn(0.42f, 1f)
            val isFocused = absDelta < 0.35f
            val spineFraction = ((absDelta - 0.40f) / 0.95f).coerceIn(0f, 1f)

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier.width(cardWidthDp)
                  .zIndex(10f - absDelta)
                  .graphicsLayer {
                    translationX = tx
                    translationY = ty
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotY
                    this.alpha = alpha
                    cameraDistance = 15f * density.density
                  }
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                  ) {
                    if (isFocused) {
                      onOpenBookDetails(book)
                    } else {
                      scope.launch {
                        virtualPosition.animateTo(
                          targetValue = virtualPosition.value + rawDelta,
                          animationSpec =
                            spring(
                              dampingRatio = 0.86f,
                              stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                      }
                    }
                  },
            ) {
              BookCoverView(
                book = book,
                showThickness = showSpineThickness,
                elevation = if (isFocused) 14.dp else 4.dp,
                spineEdgeFraction = if (showSpineThickness) spineFraction else 0f,
                spineOnRightSide = rawDelta < 0f,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(modifier = Modifier.height(6.dp))
              // Soft natural contact shadow directly beneath the standing book
              Box(
                modifier =
                  Modifier.fillMaxWidth(if (isFocused) 0.84f else 0.72f)
                    .height(if (isFocused) 8.dp else 5.dp)
                    .background(
                      Brush.horizontalGradient(
                        listOf(
                          Color.Transparent,
                          Color.Black.copy(alpha = if (isFocused) 0.16f else 0.08f),
                          Color.Transparent,
                        )
                      ),
                      shape = CircleShape,
                    )
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Section D: Editorial Featured Book Presentation
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
      val langSummary =
        selectedBook.availableLanguages.joinToString(" / ") { code ->
          if (code == "am") "አማርኛ" else LanguageCode.fromCode(code).displayNameEn
        }
      val eyebrow =
        listOfNotNull(
            selectedBook.category.takeIf { it.isNotBlank() }?.uppercase(),
            langSummary.takeIf { it.isNotBlank() }?.uppercase(),
          )
          .joinToString("  ·  ")

      Text(
        text = eyebrow,
        fontFamily = ManropeFamily,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = { stepBy(-1f) },
          enabled = books.size > 1,
          modifier = Modifier.size(48.dp).testTag("carousel_prev_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = "Previous book",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Text(
          text = selectedBook.title,
          fontFamily = BodoniModaFamily,
          fontSize = 28.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Normal,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )

        IconButton(
          onClick = { stepBy(1f) },
          enabled = books.size > 1,
          modifier = Modifier.size(48.dp).testTag("carousel_next_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Next book",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      val authorText =
        listOfNotNull(
            selectedBook.author.takeIf { it.isNotBlank() },
            selectedBook.publicationYear.takeIf { it.isNotBlank() },
          )
          .joinToString(" · ")
      if (authorText.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = authorText,
          fontFamily = LiterataFamily,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      val hasProgress = selectedBook.primaryProgress > 0.01f
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = { onReadBook(selectedBook) },
          shape = RoundedCornerShape(50),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
          modifier = Modifier.height(48.dp).testTag("carousel_read_button"),
        ) {
          Text(
            text = if (hasProgress) continueButtonLabel else readButtonLabel,
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
          )
        }

        TextButton(
          onClick = { onOpenBookDetails(selectedBook) },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          modifier = Modifier.height(48.dp).testTag("carousel_details_button"),
        ) {
          Text(
            text = detailsButtonLabel,
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      if (books.size > 1) {
        Spacer(modifier = Modifier.height(16.dp))
        val curText = (centerIndex + 1).toString().padStart(2, '0')
        val totalText = books.size.toString().padStart(2, '0')
        val fraction = centerIndex.toFloat() / (books.size - 1).coerceAtLeast(1)
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = curText,
            fontFamily = ManropeFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Box(
            modifier = Modifier.weight(1f).height(8.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(0.75.dp)
                  .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
              if (fraction > 0f) {
                Spacer(modifier = Modifier.weight(fraction.coerceAtLeast(0.001f)))
              }
              Box(
                modifier =
                  Modifier.size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
              )
              if (1f - fraction > 0f) {
                Spacer(modifier = Modifier.weight((1f - fraction).coerceAtLeast(0.001f)))
              }
            }
          }
          Text(
            text = totalText,
            fontFamily = ManropeFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
