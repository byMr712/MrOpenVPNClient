package com.mropenovpn.client.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs
import com.mropenovpn.client.VpnUsers
import de.blinkt.openvpn.core.VpnStatus

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val swAutoConnect = findViewById<MaterialSwitch>(R.id.swAutoConnect)
        val swScreenOff = findViewById<MaterialSwitch>(R.id.swScreenOff)
        val swIgnoreNetState = findViewById<MaterialSwitch>(R.id.swIgnoreNetState)
        val swNetChangeReconnect = findViewById<MaterialSwitch>(R.id.swNetChangeReconnect)

        swAutoConnect.isChecked = VpnPrefs.autoConnect(this)
        swAutoConnect.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setAutoConnect(this, checked)
        }

        swScreenOff.isChecked = VpnPrefs.screenOffPause(this)
        swScreenOff.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setScreenOffPause(this, checked)
        }

        swIgnoreNetState.isChecked = VpnPrefs.ignoreNetState(this)
        swIgnoreNetState.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setIgnoreNetState(this, checked)
        }

        swNetChangeReconnect.isChecked = VpnPrefs.netChangeReconnect(this)
        swNetChangeReconnect.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setNetChangeReconnect(this, checked)
        }

        findViewById<Button>(R.id.clearUsersButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_users)
                .setMessage(R.string.clear_users_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    VpnUsers.clearAll(this)
                    VpnStatus.logInfo(R.string.users_cleared)
                }
                .setNegativeButton(R.string.close, null)
                .show()
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
