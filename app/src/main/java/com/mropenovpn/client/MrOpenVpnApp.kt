package com.mropenovpn.client

import android.app.Application
import de.blinkt.openvpn.core.VpnStatus

class MrOpenVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VpnStatus.initLogCache(cacheDir)
    }
}
