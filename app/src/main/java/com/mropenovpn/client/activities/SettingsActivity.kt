package com.mropenovpn.client.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.materialswitch.MaterialSwitch
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.ExperimentalThemes
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs
import com.mropenovpn.client.VpnUsers
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val swNotification = findViewById<MaterialSwitch>(R.id.swNotification)
        swNotification.isChecked = VpnPrefs.notifyEnabled(this)
        swNotification.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setNotifyEnabled(this, checked)
            OpenVPNService.setNotificationVisible(checked)
        }

        val experimentalEntry = findViewById<View>(R.id.experimentalEntry)
        experimentalEntry.setOnClickListener {
            startActivity(Intent(this, ExperimentalThemesActivity::class.java))
        }

        val swDebugMode = findViewById<MaterialSwitch>(R.id.swDebugMode)
        swDebugMode.isChecked = VpnPrefs.debugMode(this)
        swDebugMode.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setDebugMode(this, checked)
        }

        val swAutoConnect = findViewById<MaterialSwitch>(R.id.swAutoConnect)
        val swScreenOff = findViewById<MaterialSwitch>(R.id.swScreenOff)

        swAutoConnect.isChecked = VpnPrefs.autoConnect(this)
        swAutoConnect.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setAutoConnect(this, checked)
        }

        swScreenOff.isChecked = VpnPrefs.screenOffPause(this)
        swScreenOff.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setScreenOffPause(this, checked)
        }

        findViewById<TextView>(R.id.clearUsersButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_users)
                .setMessage(R.string.clear_users_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    VpnUsers.clearAll(this)
                    VpnStatus.logInfo(R.string.users_cleared)
                }
                .setNegativeButton(R.string.close, null)
                .show()
                .also { ExperimentalThemes.applyAccentToDialog(it) }
        }

        findViewById<TextView>(R.id.versionText).text = getString(
            R.string.settings_version,
            runCatching { packageManager.getPackageInfo(packageName, 0) }
                .getOrNull()
                ?.let { "${it.versionName} (${it.versionCode})" }
                ?: "1.0 (1)"
        )
    }
}
