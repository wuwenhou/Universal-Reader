package com.universalreader.app.data

import android.content.Context

/** User preferences, persisted with SharedPreferences for the MVP. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()

    var autoNextChapter: Boolean
        get() = prefs.getBoolean(KEY_AUTO_NEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_NEXT, value).apply()

    var ttsRate: Float
        get() = prefs.getFloat(KEY_TTS_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_RATE, value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()

    var readerFontScale: Float
        get() = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SCALE, value).apply()

    fun isAdBlockException(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return prefs.getStringSet(KEY_AD_EXCEPTIONS, emptySet())
            ?.contains(host.lowercase()) == true
    }

    fun setAdBlockException(host: String, allowed: Boolean) {
        val current = prefs.getStringSet(KEY_AD_EXCEPTIONS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        if (allowed) current.add(host.lowercase()) else current.remove(host.lowercase())
        prefs.edit().putStringSet(KEY_AD_EXCEPTIONS, current).apply()
    }

    fun clearAdBlockExceptions() {
        prefs.edit().remove(KEY_AD_EXCEPTIONS).apply()
    }

    private companion object {
        const val PREFS_NAME = "universal_reader_settings"
        const val KEY_AD_BLOCK = "adblock_enabled"
        const val KEY_AD_EXCEPTIONS = "adblock_exceptions"
        const val KEY_AUTO_NEXT = "auto_next_chapter"
        const val KEY_TTS_RATE = "tts_rate"
        const val KEY_TTS_PITCH = "tts_pitch"
        const val KEY_FONT_SCALE = "reader_font_scale"
    }
}
