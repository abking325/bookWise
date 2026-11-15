package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.tts.TtsPlaybackState
import kotlin.math.roundToInt

/**
 * Compact, movable floating offline TTS player.
 * Remains completely hidden during normal reading; appears only when TTS starts.
 */
@Composable
fun FloatingTtsPlayer(
  state: TtsPlaybackState,
  onTogglePlayPause: () -> Unit,
  onPreviousParagraph: () -> Unit,
  onNextParagraph: () -> Unit,
  onStop: () -> Unit,
  onCycleSpeed: () -> Unit,
  onToggleMinimize: () -> Unit,
  onOpenSystemVoiceSetup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = state.isPlayerVisible,
    enter = fadeIn() + scaleIn(initialScale = 0.92f),
    exit = fadeOut() + scaleOut(targetScale = 0.92f),
    modifier = modifier,
  ) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
      modifier =
        Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              offsetX = (offsetX + dragAmount.x).coerceIn(-300f, 300f)
              offsetY = (offsetY + dragAmount.y).coerceIn(-900f, 200f)
            }
          }
          .testTag("floating_tts_player")
    ) {
      if (state.isMinimized) {
        Surface(
          shape = RoundedCornerShape(50),
          tonalElevation = 6.dp,
          shadowElevation = 10.dp,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.GraphicEq,
              contentDescription = "Audio active",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
            IconButton(
              onClick = onTogglePlayPause,
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
              )
            }
            IconButton(
              onClick = onToggleMinimize,
              modifier = Modifier.size(48.dp),
            ) {
              Icon(imageVector = Icons.Filled.ExpandLess, contentDescription = "Expand player")
            }
            IconButton(
              onClick = onStop,
              modifier = Modifier.size(48.dp),
            ) {
              Icon(imageVector = Icons.Filled.Close, contentDescription = "Stop audio")
            }
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(20.dp),
          tonalElevation = 8.dp,
          shadowElevation = 12.dp,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.widthIn(min = 280.dp, max = 380.dp),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = state.chapterTitle.ifBlank { state.bookTitle },
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    "Paragraph ${state.paragraphIndex + 1} of ${state.totalParagraphs.coerceAtLeast(1)} • ${state.languageCode.uppercase()}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row {
                IconButton(onClick = onToggleMinimize, modifier = Modifier.size(40.dp)) {
                  Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Minimize player",
                  )
                }
                IconButton(
                  onClick = onStop,
                  modifier = Modifier.size(40.dp).testTag("tts_stop_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop reading",
                  )
                }
              }
            }

            if (state.unavailableNotice != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = state.unavailableNotice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
              TextButton(onClick = onOpenSystemVoiceSetup) {
                Text("Open Offline TTS Voice Setup")
              }
            } else if (state.statusMessage != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            val fraction =
              if (state.totalParagraphs > 0) {
                (state.paragraphIndex + 1).toFloat() / state.totalParagraphs.toFloat()
              } else 0f
            LinearProgressIndicator(
              progress = { fraction.coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(3.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Surface(
                onClick = onCycleSpeed,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.height(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                  Text(
                    text = "${String.format("%.1f", state.speechRate)}x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onPreviousParagraph,
                  modifier = Modifier.size(48.dp).testTag("tts_prev_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous paragraph",
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilledIconButton(
                  onClick = onTogglePlayPause,
                  shape = CircleShape,
                  colors =
                    IconButtonDefaults.filledIconButtonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  modifier = Modifier.size(48.dp).testTag("tts_play_pause_button"),
                ) {
                  Icon(
                    imageVector =
                      if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Resume",
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                  onClick = onNextParagraph,
                  modifier = Modifier.size(48.dp).testTag("tts_next_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next paragraph",
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
