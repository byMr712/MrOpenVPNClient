package com.mropenovpn.client.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

        container.addView(buildAccentSection())
    }

    private fun buildAccentSection(): View {
        val density = resources.displayMetrics.density
        val currentHex = VpnPrefs.accentColor(this)

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }

        section.addView(TextView(this).apply {
            text = getString(R.string.accent_title)
            setTextAppearance(this@ExperimentalThemesActivity, com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
        })

        section.addView(TextView(this).apply {
            text = getString(R.string.accent_hint)
            setPadding(0, (2 * density).toInt(), 0, (8 * density).toInt())
            setTextAppearance(this@ExperimentalThemesActivity, com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        })

        val swatches = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        swatches.addView(buildSwatch("", currentHex) {
            if (currentHex != "") {
                VpnPrefs.setAccentColor(this@ExperimentalThemesActivity, "")
                recreate()
            }
        })
        ExperimentalThemes.accents.forEach { accent ->
            swatches.addView(buildSwatch(accent.hex, currentHex) {
                if (currentHex != accent.hex) {
                    VpnPrefs.setAccentColor(this@ExperimentalThemesActivity, accent.hex)
                    recreate()
                }
            })
        }
        section.addView(swatches)
        return section
    }

    private fun buildSwatch(
        hex: String,
        currentHex: String,
        onClick: () -> Unit
    ): View {
        val density = resources.displayMetrics.density
        val size = (44 * density).toInt()
        val selected = hex == currentHex

        val circle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(if (hex.isEmpty()) Color.TRANSPARENT else Color.parseColor(hex))
            setStroke(
                (if (selected) 3 else 1) * density.toInt(),
                if (selected) themeColor() else 0x33000000.toInt()
            )
        }

        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (12 * density).toInt()
            }
            background = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(themeColor()),
                circle,
                circle
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        if (selected) {
            frame.addView(TextView(this).apply {
                text = if (hex.isEmpty()) "–" else "✓"
                gravity = Gravity.CENTER
                setTextColor(contrastColor(hex))
                textSize = 18f
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }
        return frame
    }

    private fun themeColor(): Int {
        val ta = obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurface))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }

    private fun contrastColor(hex: String): Int {
        if (hex.isEmpty()) return themeColor()
        val color = Color.parseColor(hex)
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
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
