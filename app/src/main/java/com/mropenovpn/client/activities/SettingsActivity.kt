package com.mropenovpn.client.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.ExperimentalThemes
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs
import com.mropenovpn.client.VpnUsers
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus

class SettingsActivity : BaseActivity() {

    private var suppressThemeToggle = false

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

        val swLightTheme = findViewById<MaterialSwitch>(R.id.swLightTheme)
        val themeSummary = findViewById<TextView>(R.id.themeSummaryText)
        swLightTheme.isChecked = VpnPrefs.isLightTheme(this)
        updateThemeSummary(themeSummary, swLightTheme.isChecked)
        swLightTheme.setOnCheckedChangeListener { _, checked ->
            if (suppressThemeToggle) return@setOnCheckedChangeListener
            if (ExperimentalThemes.isExperimental(this)) {
                suppressThemeToggle = true
                swLightTheme.isChecked = !checked
                suppressThemeToggle = false
                confirmDisableExperimentalThemes {
                    VpnPrefs.setExperimentalTheme(this, "")
                    VpnPrefs.setAccentColor(this, "")
                    VpnPrefs.setLightTheme(this, checked)
                    AppCompatDelegate.setDefaultNightMode(
                        if (checked) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    )
                    updateThemeSummary(themeSummary, checked)
                    restartApp()
                }
            } else {
                VpnPrefs.setLightTheme(this, checked)
                AppCompatDelegate.setDefaultNightMode(
                    if (checked) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                )
                updateThemeSummary(themeSummary, checked)
            }
        }

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

    override fun onResume() {
        super.onResume()
        suppressThemeToggle = true
        val swLightTheme = findViewById<MaterialSwitch>(R.id.swLightTheme)
        swLightTheme.isChecked = VpnPrefs.isLightTheme(this)
        suppressThemeToggle = false
    }

    private fun updateThemeSummary(summary: TextView, light: Boolean) {
        summary.text = getString(
            R.string.settings_theme_summary,
            getString(if (light) R.string.theme_light else R.string.theme_dark)
        )
    }

    private fun confirmDisableExperimentalThemes(onAgree: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.theme_dialog_title)
            .setMessage(R.string.theme_dialog_message)
            .setPositiveButton(R.string.yes) { _, _ -> onAgree() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
