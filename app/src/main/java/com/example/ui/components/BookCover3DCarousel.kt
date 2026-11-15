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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.PlayfairDisplayFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * 3D rotating round-table book carousel.
 * Books travel smoothly along a 3D circular arc with perspective rotationY, depth scale,
 * real page-block thickness, and spring-snapped momentum without jumps or flickering.
 */
@Composable
fun BookCover3DCarousel(
  books: List<Book>,
  showSpineThickness: Boolean,
  depthEnabled: Boolean,
  readButtonLabel: String,
  continueButtonLabel: String,
  onOpenBookDetails: (Book) -> Unit,
  onReadBook: (Book) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (books.isEmpty()) return

  val scope = rememberCoroutineScope()
  val virtualPosition = remember { Animatable(0f) }

  // Keep continuous position stable when list shrinks
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

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth().height(236.dp),
      contentAlignment = Alignment.Center,
    ) {
      val density = LocalDensity.current
      val maxWidthPx = with(density) { maxWidth.toPx() }
      val cardWidthDp = (maxWidth * 0.37f).coerceIn(118.dp, 164.dp)
      val cardWidthPx = with(density) { cardWidthDp.toPx() }
      val dragUnitPx = (cardWidthPx * 0.92f).coerceAtLeast(110f)
      val tableRadiusX = (maxWidthPx * 0.35f).coerceAtLeast(110f)

      val draggableState = rememberDraggableState { deltaPx ->
        scope.launch {
          virtualPosition.snapTo(virtualPosition.value - (deltaPx / dragUnitPx))
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(228.dp)
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
                      val projected =
                        virtualPosition.value + (velocityBooksPerSec * 0.24f)
                      projected.roundToInt().toFloat()
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
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow,
                      ),
                  )
                }
              },
            ),
        contentAlignment = Alignment.Center,
      ) {
        val n = books.size
        // Build shortest-arc circular offsets for seamless wraparound rotation
        val slots =
          remember(books, virtualPosition.value) {
            val pos = virtualPosition.value
            val list =
              books.mapIndexed { idx, book ->
                var delta = (idx - pos) % n
                if (delta > n / 2f) delta -= n
                if (delta < -n / 2f) delta += n
                // Handle duplicate wrap when n == 2 or 3 cleanly
                idx to (book to delta)
              }
            list.sortedByDescending { abs(it.second.second) }
          }

        slots.forEach { (idx, pair) ->
          val (book, rawDelta) = pair
          val absDelta = abs(rawDelta)
          if (absDelta <= 2.4f) {
            val normalizedAngle = (rawDelta / 2.2f).coerceIn(-1f, 1f) * (PI.toFloat() * 0.44f)
            val tx = sin(normalizedAngle) * tableRadiusX
            val depthFactor = cos(normalizedAngle).coerceIn(0.35f, 1f)
            val scale = if (depthEnabled) 0.68f + 0.32f * depthFactor else 0.86f + 0.14f * depthFactor
            val rotY = if (depthEnabled) -(rawDelta * 26f).coerceIn(-54f, 54f) else 0f
            val ty = (1f - depthFactor) * 28f
            val alpha = (1f - (absDelta * 0.22f)).coerceIn(0.32f, 1f)
            val isFocused = absDelta < 0.35f

            Box(
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
                    cameraDistance = 14f * density.density
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
                              dampingRatio = 0.82f,
                              stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                      }
                    }
                  },
              contentAlignment = Alignment.Center,
            ) {
              BookCoverView(
                book = book,
                showThickness = showSpineThickness,
                elevation = if (isFocused) 14.dp else 5.dp,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
    }

    // Subtle page indicator dots when > 1 book
    if (books.size > 1) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp),
      ) {
        books.forEachIndexed { idx, _ ->
          val active = idx == centerIndex
          Box(
            modifier =
              Modifier.size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                .clip(CircleShape)
                .background(
                  if (active) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outline
                )
          )
        }
      }
    }

    // Selected book info + primary reading action directly below carousel
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) {
      Text(
        text = selectedBook.title,
        fontFamily = PlayfairDisplayFamily,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = selectedBook.author,
        fontFamily = LoraFamily,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(8.dp))

      // Language editions & category badge row
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Text(
            text = selectedBook.category,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        selectedBook.availableLanguages.forEach { code ->
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = if (code == "am") "አማርኛ" else LanguageCode.fromCode(code).displayNameEn,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      val hasProgress = selectedBook.primaryProgress > 0.01f
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { onReadBook(selectedBook) },
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.height(48.dp).testTag("carousel_read_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (hasProgress) continueButtonLabel else readButtonLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}
