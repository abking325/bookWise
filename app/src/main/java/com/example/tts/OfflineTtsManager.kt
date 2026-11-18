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
import com.example.model.AudioProgress
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
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
  val sentenceIndex: Int = 0,
  val activeCharStart: Int = -1,
  val activeCharEnd: Int = -1,
  val totalParagraphs: Int = 0,
  val totalChapters: Int = 0,
  val speechRate: Float = 1.0f,
  val pitch: Float = 1.0f,
  val statusMessage: String? = null,
  val unavailableNotice: String? = null,
)

data class SentenceSpan(
  val index: Int,
  val text: String,
  val startChar: Int,
  val endChar: Int,
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
  private var activeChapterId: String = ""
  private val playbackGeneration = AtomicLong(0L)
  @Volatile private var currentFinalUtteranceId: String = ""
  @Volatile private var currentGenPrefix: String = ""
  private var chunkOffsets = mutableMapOf<String, Int>()
  @Volatile private var isStopRequested = false

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
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
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

  private fun isTokenActive(utteranceId: String?): Boolean {
    if (utteranceId.isNullOrEmpty() || isStopRequested) return false
    val prefix = currentGenPrefix
    return prefix.isNotEmpty() && utteranceId.startsWith(prefix)
  }

  private fun setupListener() {
    tts?.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          if (!isTokenActive(utteranceId)) return
          val baseOffset = chunkOffsets[utteranceId] ?: 0
          scope.launch {
            if (!isTokenActive(utteranceId)) return@launch
            persistAudioCheckpoint(baseOffset)
          }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
          if (!isTokenActive(utteranceId)) return
          val baseOffset = chunkOffsets[utteranceId] ?: 0
          val absStart = baseOffset + start
          val absEnd = baseOffset + end
          scope.launch {
            if (!isTokenActive(utteranceId)) return@launch
            val paraText = activeParagraphs.getOrNull(_state.value.paragraphIndex).orEmpty()
            val spans = splitParagraphIntoSentenceSpans(paraText)
            val sentenceIdx =
              spans.firstOrNull { absStart in it.startChar..it.endChar }?.index
                ?: _state.value.sentenceIndex
            _state.value =
              _state.value.copy(
                sentenceIndex = sentenceIdx,
                activeCharStart = absStart,
                activeCharEnd = absEnd,
              )
          }
        }

        override fun onDone(utteranceId: String?) {
          if (utteranceId != currentFinalUtteranceId || isStopRequested) return
          val capturedGen = playbackGeneration.get()
          scope.launch {
            if (playbackGeneration.get() != capturedGen || isStopRequested) return@launch
            advanceAfterParagraphDone()
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          if (!isTokenActive(utteranceId)) return
          scope.launch {
            if (!isTokenActive(utteranceId)) return@launch
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
          if (!isTokenActive(utteranceId)) return
          scope.launch {
            if (!isTokenActive(utteranceId)) return@launch
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
    startSentenceIndex: Int = 0,
  ) {
    ensureInitialized()
    val gen = playbackGeneration.incrementAndGet()
    // Immediately cancel any previous speech before async load finishes
    isStopRequested = true
    currentFinalUtteranceId = ""
    currentGenPrefix = ""
    tts?.stop()

    scope.launch {
      if (playbackGeneration.get() != gen) return@launch
      val book = repository.books.value.firstOrNull { it.id == bookId } ?: return@launch
      val edition = book.edition(languageCode) ?: return@launch
      val content =
        repository.loadChapterContent(bookId, edition.languageCode, chapterIndex) ?: return@launch
      if (playbackGeneration.get() != gen) return@launch

      activeParagraphs = content.paragraphs
      activeChapterId = content.meta.id
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
          sentenceIndex = startSentenceIndex.coerceAtLeast(0),
          activeCharStart = -1,
          activeCharEnd = -1,
          totalParagraphs = activeParagraphs.size,
          totalChapters = edition.chapters.size,
          speechRate = settings.ttsSpeechRate,
          pitch = settings.ttsPitch,
          statusMessage = null,
          unavailableNotice = null,
        )
      speakCurrentParagraph(startSentenceIndex)
    }
  }

  private fun speakCurrentParagraph(startSentenceIndex: Int = 0) {
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

    val fullText = activeParagraphs[st.paragraphIndex].trim()
    if (fullText.isEmpty()) {
      scope.launch { advanceAfterParagraphDone() }
      return
    }

    val sentenceSpans = splitParagraphIntoSentenceSpans(fullText)
    val startSpan = sentenceSpans.getOrNull(startSentenceIndex) ?: sentenceSpans.firstOrNull()
    val startChar = startSpan?.startChar ?: 0
    val textToSpeak = fullText.substring(startChar).trim()
    if (textToSpeak.isEmpty()) {
      scope.launch { advanceAfterParagraphDone() }
      return
    }

    val gen = playbackGeneration.incrementAndGet()
    val prefix = "bw_${gen}_${UUID.randomUUID().toString().take(8)}"
    currentGenPrefix = prefix
    isStopRequested = false
    chunkOffsets.clear()

    val chunks = splitLongParagraphForTtsWithOffsets(textToSpeak, startChar)
    val finalId = "${prefix}_part_${chunks.lastIndex}"
    currentFinalUtteranceId = finalId

    _state.value =
      st.copy(
        isPlaying = true,
        isPaused = false,
        sentenceIndex = startSpan?.index ?: 0,
        activeCharStart = startChar,
        activeCharEnd = startSpan?.endChar ?: -1,
        unavailableNotice = voiceResult.message,
        statusMessage = null,
      )

    persistAudioCheckpoint(startChar)

    val params = Bundle()
    chunks.forEachIndexed { index, (chunkText, offset) ->
      val chunkId = "${prefix}_part_$index"
      chunkOffsets[chunkId] = offset
      val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      engine.speak(chunkText, mode, params, chunkId)
    }

    startOrUpdateService()
  }

  private fun persistAudioCheckpoint(charOffset: Int) {
    val st = _state.value
    if (st.bookId.isBlank()) return
    scope.launch {
      repository.updateAudioProgress(
        bookId = st.bookId,
        languageCode = st.languageCode,
        audioProgress =
          AudioProgress(
            chapterIndex = st.chapterIndex,
            chapterId = activeChapterId,
            paragraphIndex = st.paragraphIndex,
            sentenceIndex = st.sentenceIndex,
            charOffset = charOffset,
            lastUpdatedAt = System.currentTimeMillis(),
          ),
      )
    }
  }

  private fun splitLongParagraphForTtsWithOffsets(
    text: String,
    baseOffset: Int,
  ): List<Pair<String, Int>> {
    val maxLen = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(3000)
    if (text.length <= maxLen) return listOf(text to baseOffset)
    val result = mutableListOf<Pair<String, Int>>()
    var start = 0
    while (start < text.length) {
      val end = (start + maxLen).coerceAtMost(text.length)
      val splitAt =
        if (end < text.length) {
          text.lastIndexOf('.', end).takeIf { it > start }
            ?: text.lastIndexOf('።', end).takeIf { it > start }
            ?: text.lastIndexOf(' ', end).takeIf { it > start }
            ?: end
        } else end
      val piece = text.substring(start, splitAt).trim()
      if (piece.isNotEmpty()) {
        result.add(piece to (baseOffset + start))
      }
      start = splitAt
    }
    return result.ifEmpty { listOf(text to baseOffset) }
  }

  private data class VoiceCheckResult(val available: Boolean, val message: String? = null)

  private fun configureVoiceForLanguage(engine: TextToSpeech, languageCode: String): VoiceCheckResult {
    val preferredName = repository.settings.value.ttsVoiceName
    val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
    val offline = allVoices.filter { !it.isNetworkConnectionRequired }
    val targetPrefix = languageCode.lowercase().take(2)

    val selectedVoice =
      offline.firstOrNull {
        it.name == preferredName && it.locale.language.lowercase().startsWith(targetPrefix)
      }
    if (selectedVoice != null) {
      engine.voice = selectedVoice
      return VoiceCheckResult(true)
    }

    val langOfflineVoice = offline.firstOrNull { it.locale.language.lowercase().startsWith(targetPrefix) }
    if (langOfflineVoice != null) {
      engine.voice = langOfflineVoice
      return VoiceCheckResult(true)
    }

    val locale =
      if (targetPrefix == "am") Locale.Builder().setLanguage("am").setRegion("ET").build()
      else Locale.US
    val langSupport = engine.isLanguageAvailable(locale)
    if (langSupport >= TextToSpeech.LANG_AVAILABLE) {
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
      _state.value =
        st.copy(
          paragraphIndex = nextPara,
          sentenceIndex = 0,
          activeCharStart = -1,
          activeCharEnd = -1,
        )
      speakCurrentParagraph(0)
    } else if (repository.settings.value.ttsAutoAdvanceChapter && st.chapterIndex + 1 < st.totalChapters) {
      val nextChapter = st.chapterIndex + 1
      val content =
        repository.loadChapterContent(st.bookId, st.languageCode, nextChapter) ?: run {
          stop()
          return
        }
      activeParagraphs = content.paragraphs
      activeChapterId = content.meta.id
      _state.value =
        st.copy(
          chapterIndex = nextChapter,
          chapterTitle = content.meta.title,
          paragraphIndex = 0,
          sentenceIndex = 0,
          activeCharStart = -1,
          activeCharEnd = -1,
          totalParagraphs = activeParagraphs.size,
        )
      speakCurrentParagraph(0)
    } else {
      pause()
    }
  }

  fun pause() {
    playbackGeneration.incrementAndGet()
    isStopRequested = true
    currentFinalUtteranceId = ""
    currentGenPrefix = ""
    tts?.stop()
    _state.value = _state.value.copy(isPlaying = false, isPaused = true)
    notifyServiceStateChanged()
  }

  fun resume() {
    if (!_state.value.isPlayerVisible) return
    if (activeParagraphs.isEmpty()) {
      val st = _state.value
      startReadingFromParagraph(st.bookId, st.languageCode, st.chapterIndex, st.paragraphIndex, st.sentenceIndex)
      return
    }
    speakCurrentParagraph(_state.value.sentenceIndex)
  }

  fun togglePlayPause() {
    if (_state.value.isPlaying) pause() else resume()
  }

  fun previousParagraph() {
    val st = _state.value
    if (!st.isPlayerVisible) return
    if (st.paragraphIndex > 0) {
      _state.value =
        st.copy(
          paragraphIndex = st.paragraphIndex - 1,
          sentenceIndex = 0,
          activeCharStart = -1,
          activeCharEnd = -1,
        )
      speakCurrentParagraph(0)
    } else if (st.chapterIndex > 0) {
      val gen = playbackGeneration.incrementAndGet()
      isStopRequested = true
      tts?.stop()
      scope.launch {
        if (playbackGeneration.get() != gen) return@launch
        val prevContent =
          repository.loadChapterContent(st.bookId, st.languageCode, st.chapterIndex - 1)
            ?: return@launch
        if (playbackGeneration.get() != gen) return@launch
        activeParagraphs = prevContent.paragraphs
        activeChapterId = prevContent.meta.id
        val lastPara = (activeParagraphs.size - 1).coerceAtLeast(0)
        _state.value =
          st.copy(
            chapterIndex = st.chapterIndex - 1,
            chapterTitle = prevContent.meta.title,
            paragraphIndex = lastPara,
            sentenceIndex = 0,
            activeCharStart = -1,
            activeCharEnd = -1,
            totalParagraphs = activeParagraphs.size,
          )
        speakCurrentParagraph(0)
      }
    }
  }

  fun nextParagraph() {
    val st = _state.value
    if (!st.isPlayerVisible) return
    if (st.paragraphIndex + 1 < activeParagraphs.size) {
      _state.value =
        st.copy(
          paragraphIndex = st.paragraphIndex + 1,
          sentenceIndex = 0,
          activeCharStart = -1,
          activeCharEnd = -1,
        )
      speakCurrentParagraph(0)
    } else if (st.chapterIndex + 1 < st.totalChapters) {
      val gen = playbackGeneration.incrementAndGet()
      isStopRequested = true
      tts?.stop()
      scope.launch {
        if (playbackGeneration.get() != gen) return@launch
        val nextContent =
          repository.loadChapterContent(st.bookId, st.languageCode, st.chapterIndex + 1)
            ?: return@launch
        if (playbackGeneration.get() != gen) return@launch
        activeParagraphs = nextContent.paragraphs
        activeChapterId = nextContent.meta.id
        _state.value =
          st.copy(
            chapterIndex = st.chapterIndex + 1,
            chapterTitle = nextContent.meta.title,
            paragraphIndex = 0,
            sentenceIndex = 0,
            activeCharStart = -1,
            activeCharEnd = -1,
            totalParagraphs = activeParagraphs.size,
          )
        speakCurrentParagraph(0)
      }
    }
  }

  fun updateSpeechRate(rate: Float) {
    val clamped = rate.coerceIn(0.5f, 2.5f)
    scope.launch { repository.updateSettings { it.copy(ttsSpeechRate = clamped) } }
    val wasPlaying = _state.value.isPlaying
    _state.value = _state.value.copy(speechRate = clamped)
    if (wasPlaying) speakCurrentParagraph(_state.value.sentenceIndex)
  }

  fun updatePitch(pitch: Float) {
    val clamped = pitch.coerceIn(0.6f, 1.6f)
    scope.launch { repository.updateSettings { it.copy(ttsPitch = clamped) } }
    val wasPlaying = _state.value.isPlaying
    _state.value = _state.value.copy(pitch = clamped)
    if (wasPlaying) speakCurrentParagraph(_state.value.sentenceIndex)
  }

  fun setMinimized(minimized: Boolean) {
    _state.value = _state.value.copy(isMinimized = minimized)
  }

  fun stop() {
    playbackGeneration.incrementAndGet()
    isStopRequested = true
    currentFinalUtteranceId = ""
    currentGenPrefix = ""
    chunkOffsets.clear()
    tts?.stop()
    abandonAudioFocus()
    unregisterNoisyReceiver()
    _state.value =
      _state.value.copy(
        isPlayerVisible = false,
        isPlaying = false,
        isPaused = false,
        isMinimized = false,
        activeCharStart = -1,
        activeCharEnd = -1,
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

  companion object {
    fun splitParagraphIntoSentenceSpans(paragraph: String): List<SentenceSpan> {
      if (paragraph.isBlank()) return emptyList()
      val spans = mutableListOf<SentenceSpan>()
      val boundaryRegex = Regex("""(?<=[.!?።][\"”’\)\]]?)\s+""")
      var cursor = 0
      val parts = paragraph.split(boundaryRegex)
      parts.forEachIndexed { idx, part ->
        val trimmed = part.trim()
        if (trimmed.isNotEmpty()) {
          val foundAt = paragraph.indexOf(trimmed, cursor).coerceAtLeast(cursor)
          val endAt = (foundAt + trimmed.length).coerceAtMost(paragraph.length)
          spans.add(SentenceSpan(idx, trimmed, foundAt, endAt))
          cursor = endAt
        }
      }
      if (spans.isEmpty()) {
        spans.add(SentenceSpan(0, paragraph.trim(), 0, paragraph.length))
      }
      return spans
    }
  }
}
