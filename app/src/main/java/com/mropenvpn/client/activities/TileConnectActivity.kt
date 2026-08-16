package com.mropenvpn.client.activities

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.mropenvpn.client.BaseActivity
import com.mropenvpn.client.VpnPrefs
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper

class TileConnectActivity : BaseActivity() {

    override val experimentalThemeEnabled: Boolean get() = false

    private var profile: VpnProfile? = null

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpn()
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uuid = intent.getStringExtra(EXTRA_PROFILE_UUID) ?: run {
            finish()
            return
        }
        val loadedProfile = ProfileManager.getInstance(this)
            .getProfiles()
            .firstOrNull { it.uuid.toString() == uuid }
        profile = loadedProfile
        if (loadedProfile == null) {
            finish()
            return
        }

        if (loadedProfile.needUserPWInput(null, null) != 0) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_AUTO_CONNECT_PROFILE_UUID, uuid)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }

        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            startVpn()
            finish()
        }
    }

    private fun startVpn() {
        profile?.let {
            VpnPrefs.setLastProfileUuid(this, it.uuid.toString())
            VpnPrefs.applyRouteToProfile(this, it)
            VPNLaunchHelper.startOpenVpn(it, applicationContext, "quick settings tile", true)
        }
    }

    companion object {
        const val EXTRA_PROFILE_UUID = "profile_uuid"
    }
}
