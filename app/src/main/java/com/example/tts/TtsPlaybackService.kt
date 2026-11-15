package com.example.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.BookWiseApplication
import com.example.MainActivity

class TtsPlaybackService : Service() {

  companion object {
    const val CHANNEL_ID = "bookwise_tts_playback"
    const val NOTIFICATION_ID = 4102
    const val ACTION_SYNC = "com.example.tts.SYNC"
    const val ACTION_PLAY_PAUSE = "com.example.tts.PLAY_PAUSE"
    const val ACTION_PREV = "com.example.tts.PREV"
    const val ACTION_NEXT = "com.example.tts.NEXT"
    const val ACTION_STOP = "com.example.tts.STOP"
  }

  private var mediaSession: MediaSession? = null
  private var wakeLock: PowerManager.WakeLock? = null

  private val ttsManager: OfflineTtsManager
    get() = (application as BookWiseApplication).ttsManager

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    mediaSession =
      MediaSession(this, "BookWiseTtsSession").apply {
        setCallback(
          object : MediaSession.Callback() {
            override fun onPlay() {
              ttsManager.resume()
            }

            override fun onPause() {
              ttsManager.pause()
            }

            override fun onSkipToNext() {
              ttsManager.nextParagraph()
            }

            override fun onSkipToPrevious() {
              ttsManager.previousParagraph()
            }

            override fun onStop() {
              ttsManager.stop()
            }
          }
        )
        isActive = true
      }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_PLAY_PAUSE -> ttsManager.togglePlayPause()
      ACTION_PREV -> ttsManager.previousParagraph()
      ACTION_NEXT -> ttsManager.nextParagraph()
      ACTION_STOP -> {
        ttsManager.stop()
        stopSelf()
        return START_NOT_STICKY
      }
    }

    val state = ttsManager.state.value
    if (!state.isPlayerVisible) {
      releaseWakeLock()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    updateMediaSession(state)
    if (state.isPlaying) acquireWakeLock() else releaseWakeLock()
    startForeground(NOTIFICATION_ID, buildNotification(state))
    return START_NOT_STICKY
  }

  private fun updateMediaSession(state: TtsPlaybackState) {
    val session = mediaSession ?: return
    val playbackState =
      PlaybackState.Builder()
        .setActions(
          PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_STOP
        )
        .setState(
          if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
          state.paragraphIndex.toLong(),
          state.speechRate,
        )
        .build()
    session.setPlaybackState(playbackState)
    session.setMetadata(
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, state.bookTitle.ifBlank { "BookWise" })
        .putString(MediaMetadata.METADATA_KEY_ARTIST, state.chapterTitle)
        .build()
    )
  }

  private fun buildNotification(state: TtsPlaybackState): Notification {
    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    fun serviceAction(action: String, reqCode: Int): PendingIntent =
      PendingIntent.getService(
        this,
        reqCode,
        Intent(this, TtsPlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val prevAction =
      Notification.Action.Builder(
          android.R.drawable.ic_media_previous,
          "Previous",
          serviceAction(ACTION_PREV, 1),
        )
        .build()

    val playPauseAction =
      Notification.Action.Builder(
          if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
          if (state.isPlaying) "Pause" else "Play",
          serviceAction(ACTION_PLAY_PAUSE, 2),
        )
        .build()

    val nextAction =
      Notification.Action.Builder(
          android.R.drawable.ic_media_next,
          "Next",
          serviceAction(ACTION_NEXT, 3),
        )
        .build()

    val stopAction =
      Notification.Action.Builder(
          android.R.drawable.ic_menu_close_clear_cancel,
          "Stop",
          serviceAction(ACTION_STOP, 4),
        )
        .build()

    val builder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, CHANNEL_ID)
      } else {
        @Suppress("DEPRECATION")
        Notification.Builder(this)
      }

    return builder
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle(state.bookTitle.ifBlank { "BookWise Offline" })
      .setContentText(
        "${state.chapterTitle} • Para ${state.paragraphIndex + 1}/${state.totalParagraphs.coerceAtLeast(1)}"
      )
      .setContentIntent(openAppIntent)
      .setOngoing(state.isPlaying)
      .addAction(prevAction)
      .addAction(playPauseAction)
      .addAction(nextAction)
      .addAction(stopAction)
      .setStyle(
        Notification.MediaStyle()
          .setMediaSession(mediaSession?.sessionToken)
          .setShowActionsInCompactView(0, 1, 2)
      )
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            "BookWise Audio Reading",
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply {
            description = "Controls for offline Text-to-Speech book playback"
            setShowBadge(false)
          }
      manager.createNotificationChannel(channel)
    }
  }

  private fun acquireWakeLock() {
    if (wakeLock?.isHeld == true) return
    val pm = getSystemService(POWER_SERVICE) as PowerManager
    wakeLock =
      pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BookWise:TtsPlayback").apply {
        setReferenceCounted(false)
        acquire(60 * 60 * 1000L)
      }
  }

  private fun releaseWakeLock() {
    if (wakeLock?.isHeld == true) {
      runCatching { wakeLock?.release() }
    }
  }

  override fun onDestroy() {
    releaseWakeLock()
    mediaSession?.isActive = false
    mediaSession?.release()
    mediaSession = null
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
