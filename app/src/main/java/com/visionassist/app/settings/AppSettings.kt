package com.visionassist.app.settings

import android.content.Context

/**
 * Stores simple user preferences, like whether online product lookup is allowed.
 * Defaults to false (fully offline) until the user explicitly opts in.
 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("visionassist_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONLINE_LOOKUP_ENABLED = "online_lookup_enabled"
    }

    var isOnlineLookupEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_LOOKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONLINE_LOOKUP_ENABLED, value).apply()
}