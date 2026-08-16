package com.mropenvpn.client.activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.mropenvpn.client.BaseActivity
import com.mropenvpn.client.ExperimentalThemes
import com.mropenvpn.client.R
import com.mropenvpn.client.VpnPrefs
import com.mropenvpn.client.VpnUsers
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        setupLanguageSection()

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

        val statusAnimEntry = findViewById<View>(R.id.statusAnimEntry)
        statusAnimEntry.setOnClickListener {
            startActivity(Intent(this, AppAnimationsActivity::class.java))
        }

        val swDebugMode = findViewById<MaterialSwitch>(R.id.swDebugMode)
        swDebugMode.isChecked = VpnPrefs.debugMode(this)
        swDebugMode.setOnClickListener {
            if (swDebugMode.isChecked) {
                showDebugModeConfirmDialog(
                    onConfirm = {
                        VpnPrefs.setDebugMode(this, true)
                        VpnStatus.initLogCache(applicationContext.cacheDir)
                        updateDebugUi()
                    },
                    onCancel = {
                        swDebugMode.isChecked = false
                    }
                )
            } else {
                VpnPrefs.setDebugMode(this, false)
                VpnStatus.stopLogCache()
                updateDebugUi()
            }
        }
        updateDebugUi()

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

        val swFullTunnel = findViewById<MaterialSwitch>(R.id.swFullTunnel)
        swFullTunnel.isChecked = VpnPrefs.fullTunnel(this)
        swFullTunnel.setOnCheckedChangeListener { _, checked ->
            VpnPrefs.setFullTunnel(this, checked)
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

        findViewById<TextView>(R.id.resetDataButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.reset_data)
                .setMessage(R.string.reset_data_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    VpnPrefs.clearAllData(this)
                    VpnStatus.logInfo(R.string.reset_data_done)
                    restartApp()
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

    private fun setupLanguageSection() {
        findViewById<View>(R.id.languageEnglishRow).setOnClickListener {
            if (VpnPrefs.language(this) != "en") setAppLanguage("en")
        }
        findViewById<View>(R.id.languageRussianRow).setOnClickListener {
            if (VpnPrefs.language(this) != "ru") setAppLanguage("ru")
        }
        applyLanguageSelectionUi()
    }

    private fun applyLanguageSelectionUi() {
        val currentLang = VpnPrefs.language(this)
        findViewById<MaterialRadioButton>(R.id.languageEnglishRadio).isChecked = currentLang == "en"
        findViewById<MaterialRadioButton>(R.id.languageRussianRadio).isChecked = currentLang == "ru"

        findViewById<TextView>(R.id.languageEnglishTitle).text = if (currentLang == "en") {
            underlined(getString(R.string.language_en))
        } else {
            getString(R.string.language_en)
        }
        findViewById<TextView>(R.id.languageRussianTitle).text = if (currentLang == "ru") {
            underlined(getString(R.string.language_ru))
        } else {
            getString(R.string.language_ru)
        }
    }

    private fun underlined(text: String): Spanned {
        val spanned = SpannableString(text)
        spanned.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spanned
    }

    private fun setAppLanguage(tag: String) {
        VpnPrefs.setLanguage(this, tag)
        applyLanguageSelectionUi()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun showDebugModeConfirmDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_debug_mode_confirm, null)
        view.findViewById<TextView>(R.id.debugConfirmMessage).text =
            getString(R.string.debug_mode_confirm_message)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.confirmEnableButton).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        view.findViewById<Button>(R.id.cancelEnableButton).setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        dialog.show()
        ExperimentalThemes.applyAccentToDialog(dialog)
    }

    private fun updateDebugUi() {
        val debug = VpnPrefs.debugMode(this)
        val visibility = if (debug) View.VISIBLE else View.GONE
        findViewById<View>(R.id.notificationEntry).visibility = visibility
        findViewById<View>(R.id.debugDividerNotify).visibility = visibility
        findViewById<View>(R.id.debugDivider1).visibility = visibility
        findViewById<View>(R.id.clearUsersButton).visibility = visibility
        findViewById<View>(R.id.debugDivider2).visibility = visibility
        findViewById<View>(R.id.resetDataButton).visibility = visibility
    }
}
