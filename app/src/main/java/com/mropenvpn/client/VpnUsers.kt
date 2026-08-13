package com.mropenvpn.client

import android.content.Context
import android.util.Base64

object VpnUsers {
    private const val FILE = "vpn_users"
    private const val KEY_USERS = "users"
    private const val PW_PREFIX = "pw_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun key(login: String): String =
        PW_PREFIX + Base64.encodeToString(
            login.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )

    fun users(context: Context): List<String> =
        prefs(context).getStringSet(KEY_USERS, emptySet())
            ?.sorted()
            ?: emptyList()

    fun password(context: Context, login: String): String? =
        prefs(context).getString(key(login), null)

    fun uniqueName(context: Context, base: String): String {
        val names = users(context).toMutableSet()
        if (base !in names) return base
        var i = 2
        while ("$base ($i)" in names) i++
        return "$base ($i)"
    }

    fun save(context: Context, login: String, password: String) {
        val p = prefs(context)
        val set = HashSet(p.getStringSet(KEY_USERS, emptySet()))
        set.add(login)
        p.edit()
            .putStringSet(KEY_USERS, set)
            .putString(key(login), password)
            .apply()
    }

    fun delete(context: Context, login: String) {
        val p = prefs(context)
        val set = HashSet(p.getStringSet(KEY_USERS, emptySet()))
        set.remove(login)
        p.edit()
            .putStringSet(KEY_USERS, set)
            .remove(key(login))
            .apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
