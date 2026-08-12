package com.mropenovpn.client.activities

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.ExperimentalThemes
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs

class ExperimentalThemesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_experimental_themes)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.optionsContainer)
        val current = VpnPrefs.experimentalTheme(this)

        val options = buildList {
            add(Triple<String?, String, String>("", getString(R.string.experimental_default), getString(R.string.experimental_default_desc)))
            ExperimentalThemes.themes.forEach { theme ->
                add(Triple(theme.id, getString(theme.nameRes), getString(theme.descRes)))
            }
        }

        options.forEach { (id, name, desc) ->
            container.addView(buildOption(id, name, desc, selected = id == current, current = current))
        }
    }

    private fun buildOption(id: String?, name: String, desc: String, selected: Boolean, current: String): MaterialCardView {
        val card = MaterialCardView(this)
        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (10 * resources.displayMetrics.density).toInt()
        }
        card.radius = (12 * resources.displayMetrics.density)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (8 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = name
            setTextAppearance(this@ExperimentalThemesActivity, com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
        })
        texts.addView(TextView(this).apply {
            text = desc
            setTextAppearance(this@ExperimentalThemesActivity, com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        })

        val radio = MaterialRadioButton(this).apply {
            isClickable = false
            isChecked = selected
        }

        row.addView(texts)
        row.addView(radio)
        card.addView(row)

        card.setOnClickListener {
            if (id.orEmpty() != current) {
                VpnPrefs.setExperimentalTheme(this, id.orEmpty())
                restartApp()
            }
        }
        return card
    }
}
