package com.mropenovpn.client

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import com.mropenovpn.client.activities.VpnTile0
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ProfileManager

object VpnTileManager {
    const val SLOT_COUNT = 1
    private const val FILE = "vpn_tiles"
    private const val PREFIX_SLOT = "slot_"
    private const val PREFIX_REQUESTED = "req_"

    fun tileClass(slot: Int): Class<*> = VpnTile0::class.java

    fun componentName(context: Context, slot: Int): ComponentName =
        ComponentName(context, tileClass(slot))

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun profileForSlot(context: Context, slot: Int): VpnProfile? {
        if (slot < 0 || slot >= SLOT_COUNT) return null
        val uuid = prefs(context).getString(PREFIX_SLOT + slot, null) ?: return null
        return ProfileManager.getInstance(context).getProfiles()
            .firstOrNull { it.uuid.toString() == uuid }
    }

    fun sync(context: Context) {
        val profiles = ProfileManager.getInstance(context)
            .getProfiles()
            .sortedBy { it.mName }
        val p = prefs(context)
        val editor = p.edit()
        for (i in 0 until SLOT_COUNT) {
            val uuid = if (i < profiles.size) profiles[i].uuid.toString() else null
            val old = p.getString(PREFIX_SLOT + i, null)
            editor.putString(PREFIX_SLOT + i, uuid)
            if (uuid != null && uuid != old) {
                val requested = p.getBoolean(PREFIX_REQUESTED + uuid, false)
                if (!requested) {
                    requestAddTile(context, i)
                    editor.putBoolean(PREFIX_REQUESTED + uuid, true)
                }
            }
        }
        editor.apply()
    }

    private fun requestAddTile(context: Context, slot: Int) {
        if (Build.VERSION.SDK_INT < 35) return
        try {
            val method = TileService::class.java.getMethod(
                "requestAddTileService",
                ComponentName::class.java
            )
            method.invoke(null, componentName(context, slot))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
