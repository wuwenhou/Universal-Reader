package com.universalreader.app.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.universalreader.app.MainActivity
import com.universalreader.app.R
import com.universalreader.app.data.LibraryStore
import java.util.Locale

/**
 * Foreground TTS playback.
 *
 * Design decision: speak directly with Android TTS and expose a platform
 * MediaSession. Synthesizing to files would add latency and disk churn without
 * improving offline reliability for the MVP.
 */
class TtsPlaybackService : Service(), TextToSpeech.OnInitListener {

    private val handler = Handler(Looper.getMainLooper())
    private val store by lazy { LibraryStore(applicationContext) }

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var pendingPlay = false

    private var playing = false
    private var paused = false
    private var resumeOnFocusGain = false
    private var foregroundStarted = false
    private var receiverRegistered = false

    private var currentQueue: PlaybackQueue? = null
    private var currentUtteranceId: String? = null
    private var utteranceCounter = 0
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val shouldResume = playing
                // Keep the focus request so AUDIOFOCUS_GAIN can arrive later.
                pause(abandonFocus = false)
                resumeOnFocusGain = shouldResume
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSession(this, "UniversalReaderTts").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onStop() = stopPlayback()
                override fun onSkipToNext() = skipNext()
                override fun onSkipToPrevious() = skipPrevious()
            })
            isActive = true
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(noisyReceiver, filter)
        }
        receiverRegistered = true

        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        val action = intent?.action
        // Every command except STOP needs a queue handed over by the UI. This
        // also prevents a stale notification action from keeping an idle
        // foreground service alive.
        if (PlaybackSession.queue.isEmpty() && action != ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }
        when (action) {
            // The UI explicitly starts a new session; this resets the cursor
            // even when an older book is still playing.
            ACTION_START -> beginPlayback()
            // Notification/media-button play only resumes an existing session.
            ACTION_PLAY -> when {
                playing -> Unit
                paused -> resume()
                else -> beginPlayback()
            }
            ACTION_PAUSE -> if (playing) pause() else if (!paused) stopPlayback()
            ACTION_STOP -> stopPlayback()
            ACTION_NEXT -> skipNext()
            ACTION_PREVIOUS -> skipPrevious()
            ACTION_UPDATE -> if (playing) speakCurrent() else updateNotification()
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            fail("Không khởi tạo được bộ đọc TTS trên thiết bị.")
            return
        }
        initialized = true
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                // Serialize queue mutations on the main thread. Ignore
                // callbacks from an utterance that was flushed by a newer one.
                handler.post {
                    if (utteranceId != null && utteranceId == currentUtteranceId) {
                        handleUtteranceDone()
                    }
                }
            }

            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                handler.post {
                    if (utteranceId == null || utteranceId == currentUtteranceId) {
                        fail("Không phát được câu hiện tại.")
                    }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handler.post {
                    if (utteranceId == null || utteranceId == currentUtteranceId) {
                        fail("Lỗi TTS ($errorCode).")
                    }
                }
            }
        })
        configureVoice()
        if (pendingPlay) {
            pendingPlay = false
            beginPlayback()
        }
    }

    private fun configureVoice() {
        val engine = tts ?: return
        val locale = Locale.forLanguageTag("vi-VN")
        val languageResult = engine.setLanguage(locale)

        val voices = runCatching {
            engine.voices?.filter { it.locale.language == "vi" }.orEmpty()
        }.getOrDefault(emptyList())
        val offline = voices.filter { !it.isNetworkConnectionRequired }
            .maxByOrNull { it.quality }
        val chosen = offline ?: voices.maxByOrNull { it.quality }

        if (chosen != null) {
            engine.voice = chosen
        }
        PlaybackSession.update {
            it.copy(
                voiceName = chosen?.name,
                voiceOffline = chosen?.isNetworkConnectionRequired?.not()
            )
        }

        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            PlaybackSession.update {
                it.copy(
                    error = "Thiết bị chưa có giọng đọc tiếng Việt. " +
                        "Hãy cài voice vi-VN trong Cài đặt Android."
                )
            }
        }
    }

    private fun beginPlayback() {
        if (PlaybackSession.queue.isEmpty()) {
            stopPlayback()
            return
        }
        // A new ACTION_START always starts the queue handed over by the UI;
        // resume() keeps the existing cursor when the user paused.
        currentQueue = PlaybackQueue(PlaybackSession.queue, PlaybackSession.startIndex)
        val queue = currentQueue ?: return
        playing = true
        paused = false
        resumeOnFocusGain = false
        PlaybackSession.update {
            it.copy(
                isPlaying = true,
                isPreparing = true,
                currentIndex = queue.index,
                total = queue.size,
                error = null,
                completed = false
            )
        }
        requestAudioFocus()
        acquireWakeLock()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        updateNotification()
        speakCurrent()
    }

    private fun speakCurrent() {
        val queue = currentQueue ?: return
        val text = queue.current
        if (text.isNullOrBlank()) {
            finishQueue()
            return
        }
        val engine = tts
        if (engine == null || !initialized) {
            pendingPlay = true
            return
        }

        engine.setSpeechRate(PlaybackSession.rate)
        engine.setPitch(PlaybackSession.pitch)
        val utteranceId = "ur-${++utteranceCounter}"
        currentUtteranceId = utteranceId
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            fail("Không phát được câu ${queue.index + 1}.")
            return
        }

        PlaybackSession.update {
            it.copy(
                isPlaying = true,
                isPreparing = false,
                currentIndex = queue.index,
                total = queue.size
            )
        }
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        updateNotification()
    }

    private fun handleUtteranceDone() {
        if (!playing) return
        val queue = currentQueue ?: return
        if (!queue.advance()) {
            finishQueue()
            return
        }
        store.saveProgress(PlaybackSession.bookUrl, queue.index, queue.size)
        PlaybackSession.update {
            it.copy(currentIndex = queue.index, total = queue.size)
        }
        updateNotification()
        speakCurrent()
    }

    private fun pause(abandonFocus: Boolean = true) {
        if (!playing) return
        playing = false
        paused = true
        tts?.stop()
        currentQueue?.let { queue ->
            store.saveProgress(PlaybackSession.bookUrl, queue.index, queue.size)
        }
        PlaybackSession.update { it.copy(isPlaying = false, isPreparing = false) }
        releaseWakeLock()
        if (abandonFocus) {
            resumeOnFocusGain = false
            abandonAudioFocus()
        }
        updatePlaybackState(PlaybackState.STATE_PAUSED)
        updateNotification()
    }

    private fun resume() {
        if (PlaybackSession.queue.isEmpty()) {
            stopPlayback()
            return
        }
        if (currentQueue == null) {
            currentQueue = PlaybackQueue(PlaybackSession.queue, PlaybackSession.startIndex)
        }
        playing = true
        paused = false
        requestAudioFocus()
        acquireWakeLock()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
        speakCurrent()
    }

    private fun skipNext() {
        val queue = ensureQueue() ?: return
        if (!queue.advance()) {
            finishQueue()
            return
        }
        PlaybackSession.update { it.copy(currentIndex = queue.index, total = queue.size) }
        if (playing) speakCurrent() else updateNotification()
    }

    private fun skipPrevious() {
        val queue = ensureQueue() ?: return
        if (!queue.previous()) return
        PlaybackSession.update { it.copy(currentIndex = queue.index, total = queue.size) }
        if (playing) speakCurrent() else updateNotification()
    }

    private fun ensureQueue(): PlaybackQueue? {
        if (PlaybackSession.queue.isEmpty()) return null
        return currentQueue ?: PlaybackQueue(
            PlaybackSession.queue,
            PlaybackSession.startIndex
        ).also { currentQueue = it }
    }

    private fun finishQueue() {
        playing = false
        paused = false
        currentUtteranceId = null
        val size = currentQueue?.size ?: PlaybackSession.queue.size
        store.saveProgress(PlaybackSession.bookUrl, size, size)
        PlaybackSession.update {
            it.copy(
                isPlaying = false,
                isPreparing = false,
                currentIndex = (it.total - 1).coerceAtLeast(0),
                completed = true
            )
        }
        releaseWakeLock()
        abandonAudioFocus()
        updatePlaybackState(PlaybackState.STATE_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    private fun stopPlayback() {
        playing = false
        paused = false
        currentUtteranceId = null
        tts?.stop()
        currentQueue?.let { queue ->
            store.saveProgress(PlaybackSession.bookUrl, queue.index, queue.size)
        }
        PlaybackSession.update { it.copy(isPlaying = false, isPreparing = false) }
        releaseWakeLock()
        abandonAudioFocus()
        updatePlaybackState(PlaybackState.STATE_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    private fun fail(message: String) {
        playing = false
        paused = false
        currentUtteranceId = null
        PlaybackSession.update {
            it.copy(isPlaying = false, isPreparing = false, error = message)
        }
        releaseWakeLock()
        abandonAudioFocus()
        updatePlaybackState(PlaybackState.STATE_ERROR)
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun buildNotification(): Notification {
        val queue = currentQueue
        val index = queue?.index ?: PlaybackSession.startIndex
        val total = queue?.size ?: PlaybackSession.queue.size
        val status = when {
            playing -> "Đang đọc"
            paused -> "Tạm dừng"
            else -> "Sẵn sàng"
        }
        val progress = if (total > 0) " · câu ${index + 1}/$total" else ""

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flags
        )
        val playPauseIntent = pendingAction(
            if (playing) ACTION_PAUSE else ACTION_PLAY,
            REQUEST_PLAY_PAUSE,
            flags
        )
        val previousIntent = pendingAction(ACTION_PREVIOUS, REQUEST_PREVIOUS, flags)
        val nextIntent = pendingAction(ACTION_NEXT, REQUEST_NEXT, flags)
        val stopIntent = pendingAction(ACTION_STOP, REQUEST_STOP, flags)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reader)
            .setContentTitle(PlaybackSession.bookTitle.ifBlank { "Universal Reader" })
            .setContentText(status + progress)
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Trước",
                    previousIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "Tạm dừng" else "Phát",
                    playPauseIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Tiếp",
                    nextIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Dừng",
                    stopIntent
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun pendingAction(action: String, requestCode: Int, flags: Int): PendingIntent =
        PendingIntent.getForegroundService(
            this,
            requestCode,
            Intent(this, TtsPlaybackService::class.java).setAction(action),
            flags
        )

    private fun updateNotification() {
        if (!foregroundStarted) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun updatePlaybackState(state: Int) {
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        // A transient loss keeps the existing request alive; requesting again
        // would leak the old listener.
        if (focusRequest != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "UniversalReader:Tts"
        ).apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Đọc bằng giọng nói",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Điều khiển phát TTS khi màn hình tắt"
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        playing = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        releaseWakeLock()
        abandonAudioFocus()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(noisyReceiver) }
            receiverRegistered = false
        }
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "tts_playback"
        private const val NOTIFICATION_ID = 42
        private const val REQUEST_PLAY_PAUSE = 1
        private const val REQUEST_PREVIOUS = 2
        private const val REQUEST_NEXT = 3
        private const val REQUEST_STOP = 4

        private const val ACTION_START = "com.universalreader.app.tts.START"
        private const val ACTION_PLAY = "com.universalreader.app.tts.PLAY"
        private const val ACTION_PAUSE = "com.universalreader.app.tts.PAUSE"
        private const val ACTION_STOP = "com.universalreader.app.tts.STOP"
        private const val ACTION_NEXT = "com.universalreader.app.tts.NEXT"
        private const val ACTION_PREVIOUS = "com.universalreader.app.tts.PREVIOUS"
        private const val ACTION_UPDATE = "com.universalreader.app.tts.UPDATE"

        fun start(context: Context) = send(context, ACTION_START)
        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun stop(context: Context) = send(context, ACTION_STOP)
        fun next(context: Context) = send(context, ACTION_NEXT)
        fun previous(context: Context) = send(context, ACTION_PREVIOUS)
        fun updateParams(context: Context) = send(context, ACTION_UPDATE)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, TtsPlaybackService::class.java).setAction(action)
            context.startForegroundService(intent)
        }
    }
}
