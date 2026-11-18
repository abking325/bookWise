package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tts.TtsPlaybackState
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.ManropeFamily
import com.example.ui.theme.PureWhite
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Compact, draggable floating offline TTS player.
 * Remains completely hidden until TTS playback begins.
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
  val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
  val barSurfaceColor = if (isLightTheme) PureWhite else DarkSurface

  AnimatedVisibility(
    visible = state.isPlayerVisible,
    enter = fadeIn() + scaleIn(initialScale = 0.94f),
    exit = fadeOut() + scaleOut(targetScale = 0.94f),
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
          tonalElevation = 2.dp,
          shadowElevation = 8.dp,
          color = barSurfaceColor,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
          shape = RoundedCornerShape(16.dp),
          tonalElevation = 2.dp,
          shadowElevation = 10.dp,
          color = barSurfaceColor,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.widthIn(min = 280.dp, max = 384.dp),
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = state.chapterTitle.ifBlank { state.bookTitle },
                  fontFamily = LiterataFamily,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text =
                    "Para ${state.paragraphIndex + 1}/${state.totalParagraphs.coerceAtLeast(1)} · Sent ${state.sentenceIndex + 1} · ${state.languageCode.uppercase()}",
                  fontFamily = ManropeFamily,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row {
                IconButton(onClick = onToggleMinimize, modifier = Modifier.size(42.dp)) {
                  Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Minimize player",
                  )
                }
                IconButton(
                  onClick = onStop,
                  modifier = Modifier.size(42.dp).testTag("tts_stop_button"),
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

            Spacer(modifier = Modifier.height(8.dp))
            val fraction =
              if (state.totalParagraphs > 0) {
                (state.paragraphIndex + 1).toFloat() / state.totalParagraphs.toFloat()
              } else 0f
            LinearProgressIndicator(
              progress = { fraction.coerceIn(0f, 1f) },
              color = MaterialTheme.colorScheme.secondary,
              trackColor = MaterialTheme.colorScheme.outlineVariant,
              modifier = Modifier.fillMaxWidth().height(2.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Surface(
                onClick = onCycleSpeed,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.height(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                  Text(
                    text = "${String.format(Locale.US, "%.2f", state.speechRate).trimEnd('0').trimEnd('.')}x",
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
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
