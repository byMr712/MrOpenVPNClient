package com.mropenovpn.client

import android.content.Context

object VpnPrefs {
    private const val FILE = "vpn_prefs"
    private const val KEY_LAST_PROFILE = "last_profile_uuid"
    private const val KEY_AUTO_CONNECT = "auto_connect"

    private const val KEY_SCREENOFF = "screenoff"
    private const val KEY_NET_CHANGE_RECONNECT = "netchangereconnect"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_NOTIFY = "notify"
    private const val KEY_EXPERIMENTAL_THEME = "experimental_theme"
    private const val KEY_LIGHT_THEME = "light_theme"
    private const val KEY_ACCENT = "accent_color"
    private const val KEY_STATUS_ANIM = "status_outline_anim"
    private const val KEY_PROFILE_ANIM = "profile_outline_anim"
    private const val KEY_ANIM_SYNC = "anim_sync_with_status"
    private const val KEY_PROFILE_ORDER = "profile_order"

    fun notifyEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFY, true)

    fun setNotifyEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFY, value)
            .apply()
    }

    fun experimentalTheme(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_EXPERIMENTAL_THEME, "")
            ?: ""

    fun setExperimentalTheme(context: Context, id: String) {
        // commit() is required here: the app is killed immediately after this
        // via restartApp(), so an async apply() would be lost before the
        // process exits and the experimental theme would not be applied.
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXPERIMENTAL_THEME, id)
            .commit()
    }

    fun isLightTheme(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_LIGHT_THEME, false)

    fun setLightTheme(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT_THEME, value)
            .commit()
    }

    fun language(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en")
            ?: "en"

    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, tag)
            .apply()
    }

    fun accentColor(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_ACCENT, ExperimentalThemes.defaultBlackAccentHex)
            ?: ExperimentalThemes.defaultBlackAccentHex

    fun setAccentColor(context: Context, hex: String) {
        // commit() is required here: clearing the accent while disabling an
        // experimental theme is followed by restartApp(), and an async
        // apply() would be lost when the process is killed.
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCENT, hex)
            .commit()
    }

    fun debugMode(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG_MODE, false)

    fun setDebugMode(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG_MODE, value)
            .apply()
    }

    fun statusOutlineAnim(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_STATUS_ANIM, "pulse")
            ?: "pulse"

    fun setStatusOutlineAnim(context: Context, id: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATUS_ANIM, id)
            .apply()
    }

    fun profileOutlineAnim(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_ANIM, "pulse")
            ?: "pulse"

    fun setProfileOutlineAnim(context: Context, id: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_ANIM, id)
            .apply()
    }

    fun animSyncStatus(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ANIM_SYNC, true)

    fun setAnimSyncStatus(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ANIM_SYNC, value)
            .apply()
    }

    fun profileOrder(context: Context): List<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_ORDER, null)
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun setProfileOrder(context: Context, uuids: List<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_ORDER, uuids.joinToString("|"))
            .apply()
    }

    fun lastProfileUuid(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LAST_PROFILE, null)

    fun setLastProfileUuid(context: Context, uuid: String?) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_PROFILE, uuid)
            .apply()
    }

    fun autoConnect(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CONNECT, false)

    fun setAutoConnect(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CONNECT, value)
            .apply()
    }

    fun screenOffPause(context: Context): Boolean =
        vpnPrefs(context).getBoolean(KEY_SCREENOFF, false)

    fun setScreenOffPause(context: Context, value: Boolean) {
        vpnPrefs(context).edit().putBoolean(KEY_SCREENOFF, value).apply()
    }

    fun forceNetChangeReconnect(context: Context) {
        vpnPrefs(context).edit().putBoolean(KEY_NET_CHANGE_RECONNECT, true).commit()
    }

    fun clearUsers(context: Context) {
        VpnUsers.clearAll(context)
    }

    private fun vpnPrefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}
