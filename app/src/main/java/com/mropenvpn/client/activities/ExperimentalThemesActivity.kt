package com.mropenvpn.client.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.mropenvpn.client.BaseActivity
import com.mropenvpn.client.ExperimentalThemes
import com.mropenvpn.client.R
import com.mropenvpn.client.VpnPrefs
import java.util.Locale

class ExperimentalThemesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_experimental_themes)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.optionsContainer)
        val current = if (VpnPrefs.experimentalTheme(this).isEmpty()) {
            if (VpnPrefs.isLightTheme(this)) "default_white" else "default_black"
        } else {
            VpnPrefs.experimentalTheme(this)
        }

        val options = buildList {
            add(Triple<String?, String, String>("default_black", getString(R.string.theme_default_black), getString(R.string.theme_default_black_desc)))
            add(Triple<String?, String, String>("default_white", getString(R.string.theme_default_white), getString(R.string.theme_default_white_desc)))
            ExperimentalThemes.themes.forEach { theme ->
                add(Triple(theme.id, getString(theme.nameRes), getString(theme.descRes)))
            }
        }

        options.forEach { (id, name, desc) ->
            container.addView(buildOption(id, name, desc, selected = id == current, current = current))
        }

        container.addView(buildAccentSection())
    }

    private data class Swatch(
        val hex: String,
        val frame: FrameLayout,
        val circle: android.graphics.drawable.GradientDrawable
    )

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
        val swatchList = mutableListOf<Swatch>()

        fun addPresetSwatch(hex: String) {
            val sw = buildSwatch(hex, currentHex) {
                if (VpnPrefs.accentColor(this@ExperimentalThemesActivity) != hex) {
                    VpnPrefs.setAccentColor(this@ExperimentalThemesActivity, hex)
                    ExperimentalThemes.applyAccentOverlay(this@ExperimentalThemesActivity, hex)
                    refreshSwatches(swatchList)
                }
            }
            swatchList.add(sw)
            swatches.addView(sw.frame)
        }

        ExperimentalThemes.accents.forEach { accent ->
            addPresetSwatch(accent.hex)
        }
        section.addView(swatches)

        val hexRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
        }

        val input = EditText(this).apply {
            hint = getString(R.string.accent_custom_hint)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(currentHex)
            setTextColor(accentColor())
            setHintTextColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor(), 0x66))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        hexRow.addView(input)
        section.addView(hexRow)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    if (VpnPrefs.accentColor(this@ExperimentalThemesActivity).isNotEmpty()) {
                        VpnPrefs.setAccentColor(this@ExperimentalThemesActivity, "")
                        recreate()
                    }
                    return
                }
                val normalized = normalizeHex(s.toString())
                if (normalized != null &&
                    normalized != VpnPrefs.accentColor(this@ExperimentalThemesActivity)
                ) {
                    VpnPrefs.setAccentColor(this@ExperimentalThemesActivity, normalized)
                    ExperimentalThemes.applyAccentOverlay(this@ExperimentalThemesActivity, normalized)
                    refreshSwatches(swatchList)
                }
            }
        })

        return section
    }

    private fun normalizeHex(raw: String): String? {
        var s = raw.trim()
        if (s.startsWith("#")) s = s.substring(1)
        if (!s.matches(Regex("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}"))) return null
        if (s.length == 3) s = s.map { "$it$it" }.joinToString("")
        return "#" + s.uppercase(Locale.US)
    }

    private fun refreshSwatches(swatchList: List<Swatch>) {
        val current = VpnPrefs.accentColor(this)
        swatchList.forEach { sw ->
            val selected = sw.hex == current
            sw.frame.removeAllViews()
            if (selected) sw.frame.addView(buildCheck(sw.hex))
        }
    }

    private fun buildCheck(hex: String): TextView = TextView(this).apply {
        text = if (hex.isEmpty()) "–" else "✓"
        gravity = Gravity.CENTER
        setTextColor(contrastColor(hex))
        textSize = 18f
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun buildSwatch(
        hex: String,
        currentHex: String,
        onClick: () -> Unit
    ): Swatch {
        val density = resources.displayMetrics.density
        val size = (44 * density).toInt()
        val selected = hex == currentHex

        val circle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(if (hex.isEmpty()) Color.TRANSPARENT else Color.parseColor(hex))
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
            frame.addView(buildCheck(hex))
        }
        return Swatch(hex, frame, circle)
    }

    private fun themeColor(): Int {
        val ta = obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurface))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }

    private fun accentColor(): Int =
        ExperimentalThemes.accentOrDefaultColor(this, themeColor())

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
            val themeId = id.orEmpty()
            if (themeId != current) {
                val accentHex = when (themeId) {
                    "default_black" -> ExperimentalThemes.defaultBlackAccentHex
                    "default_white" -> ExperimentalThemes.defaultWhiteAccentHex
                    else -> ExperimentalThemes.themes.firstOrNull { it.id == themeId }?.accentHex
                        ?: ExperimentalThemes.defaultBlackAccentHex
                }
                VpnPrefs.setAccentColor(this, accentHex)
                when (themeId) {
                    "default_black" -> {
                        VpnPrefs.setExperimentalTheme(this, "")
                        VpnPrefs.setLightTheme(this, false)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    "default_white" -> {
                        VpnPrefs.setExperimentalTheme(this, "")
                        VpnPrefs.setLightTheme(this, true)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    else -> {
                        VpnPrefs.setExperimentalTheme(this, themeId)
                        AppCompatDelegate.setDefaultNightMode(
                            if (ExperimentalThemes.themes.firstOrNull { it.id == themeId }?.dark == true) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                AppCompatDelegate.MODE_NIGHT_NO
                            }
                        )
                    }
                }
                restartApp()
            }
        }
        return card
    }
}
