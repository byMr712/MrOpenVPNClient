package com.mropenvpn.client

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus

class MrOpenVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val dark = ExperimentalThemes.themeFor(this)?.dark ?: !VpnPrefs.isLightTheme(this)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        OpenVPNService.setNotificationVisible(VpnPrefs.notifyEnabled(this))
        if (VpnPrefs.debugMode(this)) {
            VpnStatus.initLogCache(cacheDir)
        }
        VpnPrefs.forceNetChangeReconnect(this)
    }
}
