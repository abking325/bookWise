package com.example

import android.app.Application
import com.example.data.storage.BookStorageRepository
import com.example.tts.OfflineTtsManager

class BookWiseApplication : Application() {

  lateinit var repository: BookStorageRepository
    private set

  lateinit var ttsManager: OfflineTtsManager
    private set

  override fun onCreate() {
    super.onCreate()
    repository = BookStorageRepository(this)
    ttsManager = OfflineTtsManager(this, repository)
  }
}
