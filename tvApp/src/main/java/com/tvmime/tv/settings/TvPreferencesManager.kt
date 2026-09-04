package com.tvmime.tv.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TvPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tvmime_prefs", Context.MODE_PRIVATE)

    private val _showClockOverlay = MutableStateFlow(prefs.getBoolean(KEY_SHOW_CLOCK, true))
    val showClockOverlay: StateFlow<Boolean> = _showClockOverlay.asStateFlow()

    private val _autoHideOsdSeconds = MutableStateFlow(prefs.getInt(KEY_OSD_TIMEOUT, 5))
    val autoHideOsdSeconds: StateFlow<Int> = _autoHideOsdSeconds.asStateFlow()

    private val _enableLastChannelZap = MutableStateFlow(prefs.getBoolean(KEY_LAST_CHANNEL_ZAP, true))
    val enableLastChannelZap: StateFlow<Boolean> = _enableLastChannelZap.asStateFlow()

    private val _defaultAspectMode = MutableStateFlow(prefs.getString(KEY_ASPECT_MODE, "FIT") ?: "FIT")
    val defaultAspectMode: StateFlow<String> = _defaultAspectMode.asStateFlow()

    private val _hiddenCategories = MutableStateFlow(prefs.getStringSet(KEY_HIDDEN_CATEGORIES, emptySet()) ?: emptySet())
    val hiddenCategories: StateFlow<Set<String>> = _hiddenCategories.asStateFlow()

    fun setShowClockOverlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CLOCK, enabled).apply()
        _showClockOverlay.value = enabled
    }

    fun setAutoHideOsdSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_OSD_TIMEOUT, seconds).apply()
        _autoHideOsdSeconds.value = seconds
    }

    fun setEnableLastChannelZap(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LAST_CHANNEL_ZAP, enabled).apply()
        _enableLastChannelZap.value = enabled
    }

    fun setDefaultAspectMode(mode: String) {
        prefs.edit().putString(KEY_ASPECT_MODE, mode).apply()
        _defaultAspectMode.value = mode
    }

    fun toggleCategoryVisibility(categoryId: String, hide: Boolean) {
        val current = _hiddenCategories.value.toMutableSet()
        if (hide) current.add(categoryId) else current.remove(categoryId)
        prefs.edit().putStringSet(KEY_HIDDEN_CATEGORIES, current).apply()
        _hiddenCategories.value = current
    }

    companion object {
        private const val KEY_SHOW_CLOCK = "pref_show_clock_overlay"
        private const val KEY_OSD_TIMEOUT = "pref_osd_timeout_sec"
        private const val KEY_LAST_CHANNEL_ZAP = "pref_last_channel_zap"
        private const val KEY_ASPECT_MODE = "pref_default_aspect_mode"
        private const val KEY_HIDDEN_CATEGORIES = "pref_hidden_categories"

        @Volatile
        private var INSTANCE: TvPreferencesManager? = null
        fun getInstance(context: Context): TvPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TvPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
