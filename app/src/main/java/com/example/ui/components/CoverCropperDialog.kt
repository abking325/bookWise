package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

/**
 * Interactive offline cover cropper enforcing BookWise's consistent 2:3 book cover aspect ratio.
 * Supports pinch-zoom, slider zoom, drag repositioning, and high-quality JPEG export.
 */
@Composable
fun CoverCropperDialog(
  sourceFile: File,
  outputDir: File,
  onCropped: (File) -> Unit,
  onDismiss: () -> Unit,
) {
  val bitmap = remember(sourceFile.absolutePath) { BitmapFactory.decodeFile(sourceFile.absolutePath) }
  if (bitmap == null) {
    onDismiss()
    return
  }

  var zoom by remember { mutableFloatStateOf(1f) }
  var panX by remember { mutableFloatStateOf(0f) }
  var panY by remember { mutableFloatStateOf(0f) }
  var viewportSize by remember { mutableFloatStateOf(0f) }
  var boxIntSize by remember { mutableFloatStateOf(0f) }
  var frameDims by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Crop & Position Book Cover",
          style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Pinch or drag to frame inside the 2:3 book cover guide.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth(0.76f)
              .aspectRatio(BOOK_COVER_ASPECT_RATIO)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF14171C))
              .onSizeChanged {
                frameDims = it
                viewportSize = it.width.toFloat()
                boxIntSize = it.height.toFloat()
              }
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                  val newZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                  zoom = newZoom
                  panX = (panX + pan.x).coerceIn(-600f, 600f)
                  panY = (panY + pan.y).coerceIn(-800f, 800f)
                }
              },
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Cover crop preview",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = panX
                translationY = panY
              },
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
              color = Color.White.copy(alpha = 0.85f),
              topLeft = Offset.Zero,
              size = Size(size.width, size.height),
              style = Stroke(width = 3f),
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Zoom", style = MaterialTheme.typography.labelMedium)
          Slider(
            value = zoom,
            onValueChange = { zoom = it },
            valueRange = 1f..4f,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
          )
          Text("${String.format("%.1f", zoom)}x", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text("Cancel")
          }
          Button(
            onClick = {
              val croppedFile =
                renderCroppedCover(
                  source = bitmap,
                  frameW = frameDims.width.coerceAtLeast(300),
                  frameH = frameDims.height.coerceAtLeast(450),
                  zoom = zoom,
                  panX = panX,
                  panY = panY,
                  outputDir = outputDir,
                )
              onCropped(croppedFile)
            },
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text("Apply Cover")
          }
        }
      }
    }
  }
}

private fun renderCroppedCover(
  source: Bitmap,
  frameW: Int,
  frameH: Int,
  zoom: Float,
  panX: Float,
  panY: Float,
  outputDir: File,
): File {
  val targetW = 800
  val targetH = 1200
  val outBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
  val canvas = android.graphics.Canvas(outBitmap)

  val srcRatio = source.width.toFloat() / source.height.toFloat()
  val dstRatio = targetW.toFloat() / targetH.toFloat()
  val baseScale =
    if (srcRatio > dstRatio) {
      targetH.toFloat() / source.height.toFloat()
    } else {
      targetW.toFloat() / source.width.toFloat()
    }

  val scaleToTarget = targetW.toFloat() / frameW.toFloat()
  val matrix = android.graphics.Matrix()
  matrix.postTranslate(-source.width / 2f, -source.height / 2f)
  matrix.postScale(baseScale * zoom, baseScale * zoom)
  matrix.postTranslate(
    targetW / 2f + panX * scaleToTarget,
    targetH / 2f + panY * scaleToTarget,
  )

  val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
  canvas.drawColor(android.graphics.Color.BLACK)
  canvas.drawBitmap(source, matrix, paint)

  outputDir.mkdirs()
  val outFile = File(outputDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
  FileOutputStream(outFile).use { fos ->
    outBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
  }
  outBitmap.recycle()
  return outFile
}
