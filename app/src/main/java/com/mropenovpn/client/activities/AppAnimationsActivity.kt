package com.mropenovpn.client.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs

class AppAnimationsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_animations)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val swAnimSync = findViewById<MaterialSwitch>(R.id.swAnimSync)
        val statusAnimEntry = findViewById<View>(R.id.statusAnimEntry)
        val profileAnimEntry = findViewById<View>(R.id.profileAnimEntry)

        swAnimSync.isChecked = VpnPrefs.animSyncStatus(this)
        swAnimSync.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setAnimSyncStatus(this, checked)
            updateProfileEntry(profileAnimEntry, checked)
        }

        statusAnimEntry.setOnClickListener {
            startActivity(
                Intent(this, StatusAnimationsActivity::class.java)
                    .putExtra(
                        StatusAnimationsActivity.EXTRA_TARGET,
                        StatusAnimationsActivity.TARGET_STATUS
                    )
            )
        }

        profileAnimEntry.setOnClickListener {
            startActivity(
                Intent(this, StatusAnimationsActivity::class.java)
                    .putExtra(
                        StatusAnimationsActivity.EXTRA_TARGET,
                        StatusAnimationsActivity.TARGET_PROFILE
                    )
            )
        }

        updateProfileEntry(profileAnimEntry, swAnimSync.isChecked)
    }

    private fun updateProfileEntry(entry: View, syncEnabled: Boolean) {
        entry.isEnabled = !syncEnabled
        entry.alpha = if (syncEnabled) 0.38f else 1f
    }
}
