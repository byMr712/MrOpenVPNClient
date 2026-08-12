package com.mropenovpn.client

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus

class MrOpenVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            if (VpnPrefs.isLightTheme(this)) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )
        OpenVPNService.setNotificationVisible(VpnPrefs.notifyEnabled(this))
        VpnStatus.initLogCache(cacheDir)
    }
}
