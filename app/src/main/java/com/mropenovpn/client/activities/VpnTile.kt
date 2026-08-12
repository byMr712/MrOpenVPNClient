package com.mropenovpn.client.activities

import android.content.Intent
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs
import com.mropenovpn.client.VpnTileManager
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus

abstract class VpnTileBase(private val slot: Int) : TileService(), VpnStatus.StateListener {

    override fun onStartListening() {
        super.onStartListening()
        VpnStatus.addStateListener(this)
        updateTile()
    }

    override fun onStopListening() {
        VpnStatus.removeStateListener(this)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val profile = VpnTileManager.profileForSlot(this, slot)
        if (profile == null) {
            startActivityAndCollapse(Intent(this, MainActivity::class.java))
            return
        }
        val connectedUuid = VpnStatus.getLastConnectedVPNProfile()
        if (VpnStatus.isVPNActive() && connectedUuid == profile.uuid.toString()) {
            val intent = Intent(this, OpenVPNService::class.java)
            intent.action = OpenVPNService.DISCONNECT_VPN
            startService(intent)
        } else {
            connect(profile)
        }
    }

    private fun connect(profile: VpnProfile) {
        VpnPrefs.setLastProfileUuid(this, profile.uuid.toString())
        if (profile.needUserPWInput(null, null) != 0) {
            startActivityAndCollapse(
                Intent(this, MainActivity::class.java)
                    .putExtra(
                        MainActivity.EXTRA_AUTO_CONNECT_PROFILE_UUID,
                        profile.uuid.toString()
                    )
            )
            return
        }
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            startActivityAndCollapse(
                Intent(this, TileConnectActivity::class.java)
                    .putExtra(TileConnectActivity.EXTRA_PROFILE_UUID, profile.uuid.toString())
            )
        } else {
            VPNLaunchHelper.startOpenVpn(profile, this, "quick settings tile", true)
        }
    }

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        level: ConnectionStatus,
        intent: Intent?
    ) {
        updateTile()
    }

    override fun setConnectedVPN(uuid: String?) {
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val profile = VpnTileManager.profileForSlot(this, slot)
        if (profile == null) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.tile_no_profile_slot)
        } else {
            val connectedUuid = VpnStatus.getLastConnectedVPNProfile()
            val active = VpnStatus.isVPNActive() && connectedUuid == profile.uuid.toString()
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = profile.mName
        }
        tile.updateTile()
    }
}

class VpnTile0 : VpnTileBase(0)
