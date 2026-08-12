package com.mropenovpn.client

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    protected open val experimentalThemeEnabled: Boolean get() = true

    override fun attachBaseContext(newBase: Context) {
        val tag = VpnPrefs.language(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (experimentalThemeEnabled) {
            val themeRes = ExperimentalThemes.styleFor(this)
            if (themeRes != 0) setTheme(themeRes)
        }
        super.onCreate(savedInstanceState)
    }

    protected fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
