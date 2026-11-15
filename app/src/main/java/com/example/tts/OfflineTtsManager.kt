package com.example.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import com.example.data.storage.BookStorageRepository
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfflineVoiceInfo(
  val name: String,
  val localeTag: String,
  val displayLanguage: String,
  val genderHint: String,
  val qualityLabel: String,
)

data class TtsPlaybackState(
  val isEngineReady: Boolean = false,
  val isPlayerVisible: Boolean = false,
  val isPlaying: Boolean = false,
  val isPaused: Boolean = false,
  val isMinimized: Boolean = false,
  val bookId: String = "",
  val bookTitle: String = "",
  val languageCode: String = "en",
  val chapterIndex: Int = 0,
  val chapterTitle: String = "",
  val paragraphIndex: Int = 0,
  val totalParagraphs: Int = 0,
  val totalChapters: Int = 0,
  val speechRate: Float = 1.0f,
  val pitch: Float = 1.0f,
  val statusMessage: String? = null,
  val unavailableNotice: String? = null,
)

class OfflineTtsManager(
  private val context: Context,
  private val repository: BookStorageRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(TtsPlaybackState())
  val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

  private val _offlineVoices = MutableStateFlow<List<OfflineVoiceInfo>>(emptyList())
  val offlineVoices: StateFlow<List<OfflineVoiceInfo>> = _offlineVoices.asStateFlow()

  private var tts: TextToSpeech? = null
  private var activeParagraphs: List<String> = emptyList()
  private var currentUtteranceToken: String = ""
  private var isStopRequested = false

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var focusRequest: AudioFocusRequest? = null
  private var noisyReceiverRegistered = false

  private val noisyReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && _state.value.isPlaying) {
          pause()
        }
      }
    }

  private val audioFocusListener =
    AudioManager.OnAudioFocusChangeListener { change ->
      when (change) {
        AudioManager.AUDIOFOCUS_LOSS,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          if (_state.value.isPlaying) pause()
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          // Pause cleanly for clarity while ducking is requested
          if (_state.value.isPlaying) pause()
        }
      }
    }

  fun ensureInitialized() {
    if (tts != null) return
    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          setupListener()
          refreshOfflineVoices()
          val settings = repository.settings.value
          _state.value =
            _state.value.copy(
              isEngineReady = true,
              speechRate = settings.ttsSpeechRate,
              pitch = settings.ttsPitch,
            )
        } else {
          _state.value =
            _state.value.copy(
              isEngineReady = false,
              unavailableNotice =
                "Android Text-to-Speech engine could not initialize on this device. Check system TTS settings.",
            )
        }
      }
  }

  private fun setupListener() {
    tts?.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
          if (utteranceId != currentUtteranceToken || isStopRequested) return
          scope.launch { advanceAfterParagraphDone() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          if (utteranceId != currentUtteranceToken || isStopRequested) return
          scope.launch {
            _state.value =
              _state.value.copy(
                isPlaying = false,
                isPaused = true,
                statusMessage = "Offline voice synthesis could not read this passage.",
              )
            notifyServiceStateChanged()
          }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          if (utteranceId != currentUtteranceToken || isStopRequested) return
          scope.launch {
            _state.value =
              _state.value.copy(
                isPlaying = false,
                isPaused = true,
                statusMessage =
                  "Offline TTS unavailable for this language (code $errorCode). Install offline voice data in system settings.",
              )
            notifyServiceStateChanged()
          }
        }
      }
    )
  }

  fun refreshOfflineVoices() {
    val engine = tts ?: return
    val voices =
      runCatching { engine.voices?.toList().orEmpty() }
        .getOrDefault(emptyList())
        .filter { !it.isNetworkConnectionRequired }
        .sortedWith(compareBy({ it.locale.language }, { it.name }))

    _offlineVoices.value =
      voices.map { v ->
        val features = runCatching { v.features }.getOrNull().orEmpty()
        val lowerName = v.name.lowercase()
        val gender =
          when {
            lowerName.contains("female") || features.any { it.contains("female", true) } -> "Female"
            lowerName.contains("male") || features.any { it.contains("male", true) } -> "Male"
            lowerName.contains("-f") || lowerName.contains("_f_") -> "Female"
            lowerName.contains("-m") || lowerName.contains("_m_") -> "Male"
            else -> "Neutral"
          }
        val quality = if (v.quality >= Voice.QUALITY_HIGH) "High" else "Standard"
        OfflineVoiceInfo(
          name = v.name,
          localeTag = v.locale.toLanguageTag(),
          displayLanguage = v.locale.displayName,
          genderHint = gender,
          qualityLabel = quality,
        )
      }
  }

  fun hasOfflineVoiceForLanguage(languageCode: String): Boolean {
    val prefix = languageCode.lowercase().take(2)
    return _offlineVoices.value.any { it.localeTag.lowercase().startsWith(prefix) }
  }

  fun startReadingFromParagraph(
    bookId: String,
    languageCode: String,
    chapterIndex: Int,
    paragraphIndex: Int,
  ) {
    ensureInitialized()
    scope.launch {
      val book = repository.books.value.firstOrNull { it.id == bookId } ?: return@launch
      val edition = book.edition(languageCode) ?: return@launch
      val content =
        repository.loadChapterContent(bookId, edition.languageCode, chapterIndex) ?: return@launch
      activeParagraphs = content.paragraphs
      val safePara = paragraphIndex.coerceIn(0, (activeParagraphs.size - 1).coerceAtLeast(0))
      val settings = repository.settings.value

      _state.value =
        _state.value.copy(
          isPlayerVisible = true,
          isMinimized = false,
          bookId = book.id,
          bookTitle = book.title,
          languageCode = edition.languageCode,
          chapterIndex = chapterIndex,
          chapterTitle = content.meta.title,
          paragraphIndex = safePara,
          totalParagraphs = activeParagraphs.size,
          totalChapters = edition.chapters.size,
          speechRate = settings.ttsSpeechRate,
          pitch = settings.ttsPitch,
          statusMessage = null,
          unavailableNotice = null,
        )
      speakCurrentParagraph()
    }
  }

  private fun speakCurrentParagraph() {
    val engine = tts
    if (engine == null || !_state.value.isEngineReady) {
      ensureInitialized()
      _state.value =
        _state.value.copy(
          isPlaying = false,
          isPaused = true,
          unavailableNotice = "Initializing offline TTS engine… Tap Play again in a moment.",
        )
      return
    }

    val st = _state.value
    if (st.paragraphIndex !in activeParagraphs.indices) {
      stop()
      return
    }

    val voiceResult = configureVoiceForLanguage(engine, st.languageCode)
    if (!voiceResult.available) {
      _state.value =
        st.copy(
          isPlaying = false,
          isPaused = true,
          unavailableNotice = voiceResult.message,
        )
      return
    }

    if (!requestAudioFocus()) {
      _state.value =
        st.copy(
          isPlaying = false,
          isPaused = true,
          statusMessage = "Audio focus unavailable.",
        )
      return
    }
    registerNoisyReceiver()

    engine.setSpeechRate(st.speechRate)
    engine.setPitch(st.pitch)

    val text = activeParagraphs[st.paragraphIndex].trim()
    if (text.isEmpty()) {
      scope.launch { advanceAfterParagraphDone() }
      return
    }

    isStopRequested = false
    val token = UUID.randomUUID().toString()
    currentUtteranceToken = token

    _state.value =
      st.copy(
        isPlaying = true,
        isPaused = false,
        unavailableNotice = voiceResult.message,
        statusMessage = null,
      )

    val params = Bundle()
    val chunks = splitLongParagraphForTts(text)
    chunks.forEachIndexed { index, chunk ->
      val chunkId = if (index == chunks.lastIndex) token else "${token}_part_$index"
      val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      engine.speak(chunk, mode, params, chunkId)
    }

    startOrUpdateService()
  }

  private fun splitLongParagraphForTts(text: String): List<String> {
    val maxLen = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(3000)
    if (text.length <= maxLen) return listOf(text)
    val result = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
      val end = (start + maxLen).coerceAtMost(text.length)
      val splitAt =
        if (end < text.length) {
          text.lastIndexOf('.', end).takeIf { it > start }
            ?: text.lastIndexOf(' ', end).takeIf { it > start }
            ?: end
        } else end
      result.add(text.substring(start, splitAt).trim())
      start = splitAt
    }
    return result.filter { it.isNotEmpty() }
  }

  private data class VoiceCheckResult(val available: Boolean, val message: String? = null)

  private fun configureVoiceForLanguage(engine: TextToSpeech, languageCode: String): VoiceCheckResult {
    val preferredName = repository.settings.value.ttsVoiceName
    val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
    val offline = allVoices.filter { !it.isNetworkConnectionRequired }
    val targetPrefix = languageCode.lowercase().take(2)

    // 1. Exact user-selected offline voice matching language
    val selectedVoice =
      offline.firstOrNull {
        it.name == preferredName && it.locale.language.lowercase().startsWith(targetPrefix)
      }
    if (selectedVoice != null) {
      engine.voice = selectedVoice
      return VoiceCheckResult(true)
    }

    // 2. Any installed offline voice for target language
    val langOfflineVoice = offline.firstOrNull { it.locale.language.lowercase().startsWith(targetPrefix) }
    if (langOfflineVoice != null) {
      engine.voice = langOfflineVoice
      return VoiceCheckResult(true)
    }

    // 3. Fallback check via setLanguage + offline availability check
    val locale =
      if (targetPrefix == "am") Locale.Builder().setLanguage("am").setRegion("ET").build()
      else Locale.US
    val langSupport = engine.isLanguageAvailable(locale)
    if (langSupport >= TextToSpeech.LANG_AVAILABLE) {
      // Double check the resulting active voice is not network-required
      val activeVoice = runCatching { engine.voice }.getOrNull()
      if (activeVoice != null && activeVoice.isNetworkConnectionRequired) {
        return VoiceCheckResult(
          false,
          "Only an online voice is available for ${locale.displayLanguage}. Install offline voice data in Android Text-to-Speech settings.",
        )
      }
      engine.language = locale
      return VoiceCheckResult(true)
    }

    // 4. Graceful error notice for missing Amharic or English offline voice
    val notice =
      if (targetPrefix == "am") {
        "No offline Amharic (አማርኛ) voice is installed on this device. Install an offline Amharic TTS voice pack in Android Settings -> Text-to-Speech, or switch to the English edition."
      } else {
        "No offline English TTS voice was found on this device. Please install offline English voice data in Android TTS Settings."
      }
    return VoiceCheckResult(false, notice)
  }

  private suspend fun advanceAfterParagraphDone() {
    val st = _state.value
    if (!st.isPlaying) return
    val nextPara = st.paragraphIndex + 1
    if (nextPara < activeParagraphs.size) {
      _state.value = st.copy(paragraphIndex = nextPara)
      speakCurrentParagraph()
    } else {
      val nextChapter = st.chapterIndex + 1
      if (nextChapter < st.totalChapters) {
        val content =
          repository.loadChapterContent(st.bookId, st.languageCode, nextChapter) ?: run {
            stop()
            return
          }
        activeParagraphs = content.paragraphs
        _state.value =
          st.copy(
            chapterIndex = nextChapter,
            chapterTitle = content.meta.title,
            paragraphIndex = 0,
            totalParagraphs = activeParagraphs.size,
          )
        speakCurrentParagraph()
      } else {
        stop()
      }
    }
  }

  fun pause() {
    isStopRequested = true
    currentUtteranceToken = ""
    tts?.stop()
    _state.value = _state.value.copy(isPlaying = false, isPaused = true)
    notifyServiceStateChanged()
  }

  fun resume() {
    if (!_state.value.isPlayerVisible) return
    if (activeParagraphs.isEmpty()) {
      val st = _state.value
      startReadingFromParagraph(st.bookId, st.languageCode, st.chapterIndex, st.paragraphIndex)
      return
    }
    speakCurrentParagraph()
  }

  fun togglePlayPause() {
    if (_state.value.isPlaying) pause() else resume()
  }

  fun previousParagraph() {
    val st = _state.value
    if (!st.isPlayerVisible) return
    if (st.paragraphIndex > 0) {
      _state.value = st.copy(paragraphIndex = st.paragraphIndex - 1)
      speakCurrentParagraph()
    } else if (st.chapterIndex > 0) {
      scope.launch {
        val prevContent =
          repository.loadChapterContent(st.bookId, st.languageCode, st.chapterIndex - 1)
            ?: return@launch
        activeParagraphs = prevContent.paragraphs
        val lastPara = (activeParagraphs.size - 1).coerceAtLeast(0)
        _state.value =
          st.copy(
            chapterIndex = st.chapterIndex - 1,
            chapterTitle = prevContent.meta.title,
            paragraphIndex = lastPara,
            totalParagraphs = activeParagraphs.size,
          )
        speakCurrentParagraph()
      }
    }
  }

  fun nextParagraph() {
    val st = _state.value
    if (!st.isPlayerVisible) return
    if (st.paragraphIndex + 1 < activeParagraphs.size) {
      _state.value = st.copy(paragraphIndex = st.paragraphIndex + 1)
      speakCurrentParagraph()
    } else if (st.chapterIndex + 1 < st.totalChapters) {
      scope.launch {
        val nextContent =
          repository.loadChapterContent(st.bookId, st.languageCode, st.chapterIndex + 1)
            ?: return@launch
        activeParagraphs = nextContent.paragraphs
        _state.value =
          st.copy(
            chapterIndex = st.chapterIndex + 1,
            chapterTitle = nextContent.meta.title,
            paragraphIndex = 0,
            totalParagraphs = activeParagraphs.size,
          )
        speakCurrentParagraph()
      }
    }
  }

  fun updateSpeechRate(rate: Float) {
    val clamped = rate.coerceIn(0.5f, 2.5f)
    scope.launch { repository.updateSettings { it.copy(ttsSpeechRate = clamped) } }
    val wasPlaying = _state.value.isPlaying
    _state.value = _state.value.copy(speechRate = clamped)
    if (wasPlaying) speakCurrentParagraph()
  }

  fun updatePitch(pitch: Float) {
    val clamped = pitch.coerceIn(0.6f, 1.6f)
    scope.launch { repository.updateSettings { it.copy(ttsPitch = clamped) } }
    val wasPlaying = _state.value.isPlaying
    _state.value = _state.value.copy(pitch = clamped)
    if (wasPlaying) speakCurrentParagraph()
  }

  fun setMinimized(minimized: Boolean) {
    _state.value = _state.value.copy(isMinimized = minimized)
  }

  fun stop() {
    isStopRequested = true
    currentUtteranceToken = ""
    tts?.stop()
    abandonAudioFocus()
    unregisterNoisyReceiver()
    _state.value =
      _state.value.copy(
        isPlayerVisible = false,
        isPlaying = false,
        isPaused = false,
        isMinimized = false,
        statusMessage = null,
        unavailableNotice = null,
      )
    stopService()
  }

  fun previewVoice(voiceName: String, languageCode: String) {
    ensureInitialized()
    val engine = tts ?: return
    val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
    val offlineMatch = allVoices.firstOrNull { !it.isNetworkConnectionRequired && it.name == voiceName }
    if (offlineMatch != null) {
      engine.voice = offlineMatch
    } else {
      val check = configureVoiceForLanguage(engine, languageCode)
      if (!check.available) {
        _state.value = _state.value.copy(unavailableNotice = check.message)
        return
      }
    }
    engine.setSpeechRate(repository.settings.value.ttsSpeechRate)
    engine.setPitch(repository.settings.value.ttsPitch)
    val sample =
      if (languageCode.startsWith("am")) "ሰላም፣ ይህ የBookWise ከመስመር ውጭ ንባብ ድምጽ ሙከራ ነው።"
      else "Welcome to BookWise Offline. This is a preview of your selected offline reading voice."
    engine.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "preview_${System.currentTimeMillis()}")
  }

  fun openInstallTtsDataIntent() {
    val intent =
      Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }.onFailure {
      val fallback =
        Intent("com.android.settings.TTS_SETTINGS").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      runCatching { context.startActivity(fallback) }
    }
  }

  private fun startOrUpdateService() {
    if (!repository.settings.value.ttsBackgroundPlaybackEnabled) return
    val intent = Intent(context, TtsPlaybackService::class.java).apply {
      action = TtsPlaybackService.ACTION_SYNC
    }
    runCatching { ContextCompat.startForegroundService(context, intent) }
  }

  private fun notifyServiceStateChanged() {
    if (!repository.settings.value.ttsBackgroundPlaybackEnabled) return
    if (_state.value.isPlayerVisible) {
      val intent = Intent(context, TtsPlaybackService::class.java).apply {
        action = TtsPlaybackService.ACTION_SYNC
      }
      runCatching { context.startService(intent) }
    }
  }

  private fun stopService() {
    runCatching { context.stopService(Intent(context, TtsPlaybackService::class.java)) }
  }

  private fun requestAudioFocus(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setOnAudioFocusChangeListener(audioFocusListener)
          .build()
      focusRequest = req
      audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        audioFocusListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN,
      ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
      focusRequest = null
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(audioFocusListener)
    }
  }

  private fun registerNoisyReceiver() {
    if (noisyReceiverRegistered) return
    val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    ContextCompat.registerReceiver(
      context,
      noisyReceiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    noisyReceiverRegistered = true
  }

  private fun unregisterNoisyReceiver() {
    if (!noisyReceiverRegistered) return
    runCatching { context.unregisterReceiver(noisyReceiver) }
    noisyReceiverRegistered = false
  }
}
