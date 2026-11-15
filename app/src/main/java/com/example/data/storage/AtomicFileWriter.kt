package com.example.data.storage

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object AtomicFileWriter {

  @Throws(IOException::class)
  fun writeTextAtomically(target: File, content: String) {
    writeBytesAtomically(target, content.toByteArray(StandardCharsets.UTF_8))
  }

  @Throws(IOException::class)
  fun writeBytesAtomically(target: File, bytes: ByteArray) {
    val parent = target.parentFile
    if (parent != null && !parent.exists()) {
      parent.mkdirs()
    }
    val tempFile = File(parent, ".${target.name}.${System.nanoTime()}.tmp")
    try {
      FileOutputStream(tempFile).use { fos ->
        fos.write(bytes)
        fos.flush()
        try {
          fos.fd.sync()
        } catch (_: Throwable) {
          // Ignore sync failures on filesystems/robolectric that do not support fsync
        }
      }
      if (target.exists() && !target.delete()) {
        // Fallback overwrite if rename over existing fails on older filesystems
        FileOutputStream(target).use { fos ->
          fos.write(bytes)
          fos.flush()
        }
        tempFile.delete()
        return
      }
      if (!tempFile.renameTo(target)) {
        tempFile.copyTo(target, overwrite = true)
        tempFile.delete()
      }
    } finally {
      if (tempFile.exists()) {
        tempFile.delete()
      }
    }
  }
}
