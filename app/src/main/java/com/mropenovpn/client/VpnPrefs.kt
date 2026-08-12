package com.mropenovpn.client

import android.content.Context

object VpnPrefs {
    private const val FILE = "vpn_prefs"
    private const val KEY_LAST_PROFILE = "last_profile_uuid"
    private const val KEY_AUTO_CONNECT = "auto_connect"

    private const val KEY_SCREENOFF = "screenoff"
    private const val KEY_IGNORE_NET_STATE = "ignorenetstate"
    private const val KEY_NET_CHANGE_RECONNECT = "netchangereconnect"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_LIGHT_THEME = "light_theme"
    private const val KEY_NOTIFY = "notify"
    private const val KEY_EXPERIMENTAL_THEME = "experimental_theme"

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
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXPERIMENTAL_THEME, id)
            .apply()
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

    fun debugMode(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG_MODE, false)

    fun setDebugMode(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG_MODE, value)
            .apply()
    }

    fun isLightTheme(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_LIGHT_THEME, false)

    fun setLightTheme(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT_THEME, value)
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

    fun ignoreNetState(context: Context): Boolean =
        vpnPrefs(context).getBoolean(KEY_IGNORE_NET_STATE, false)

    fun setIgnoreNetState(context: Context, value: Boolean) {
        vpnPrefs(context).edit().putBoolean(KEY_IGNORE_NET_STATE, value).apply()
    }

    fun netChangeReconnect(context: Context): Boolean =
        vpnPrefs(context).getBoolean(KEY_NET_CHANGE_RECONNECT, true)

    fun setNetChangeReconnect(context: Context, value: Boolean) {
        vpnPrefs(context).edit().putBoolean(KEY_NET_CHANGE_RECONNECT, value).apply()
    }

    fun clearUsers(context: Context) {
        VpnUsers.clearAll(context)
    }

    private fun vpnPrefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}
