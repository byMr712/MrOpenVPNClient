package com.mropenovpn.client

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.shape.MaterialShapeDrawable

object ExperimentalThemes {

    data class ThemeDef(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val styleRes: Int,
        val accentHex: String,
        val dark: Boolean
    )

    data class AccentDef(
        val hex: String,
        val styleRes: Int
    )

    val accents = listOf(
        AccentDef("#1E88E5", R.style.AppThemeOverlay_Accent_Blue),
        AccentDef("#2E7D32", R.style.AppThemeOverlay_Accent_Green),
        AccentDef("#EF6C00", R.style.AppThemeOverlay_Accent_Orange),
        AccentDef("#8E24AA", R.style.AppThemeOverlay_Accent_Purple),
        AccentDef("#D81B60", R.style.AppThemeOverlay_Accent_Pink),
        AccentDef("#00897B", R.style.AppThemeOverlay_Accent_Teal)
    )

    const val defaultBlackAccentHex = "#FFFFFF"
    const val defaultWhiteAccentHex = "#000000"

    val themes = listOf(
        ThemeDef(
            id = "neon",
            nameRes = R.string.theme_neon_name,
            descRes = R.string.theme_neon_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Neon,
            accentHex = "#00E5FF",
            dark = true
        ),
        ThemeDef(
            id = "oled",
            nameRes = R.string.theme_oled_name,
            descRes = R.string.theme_oled_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Oled,
            accentHex = "#33FF33",
            dark = true
        ),
        ThemeDef(
            id = "paper",
            nameRes = R.string.theme_paper_name,
            descRes = R.string.theme_paper_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Paper,
            accentHex = "#22355E",
            dark = false
        ),
        ThemeDef(
            id = "redline",
            nameRes = R.string.theme_redline_name,
            descRes = R.string.theme_redline_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Redline,
            accentHex = "#FF453A",
            dark = true
        ),
        ThemeDef(
            id = "mint",
            nameRes = R.string.theme_mint_name,
            descRes = R.string.theme_mint_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Mint,
            accentHex = "#00A67D",
            dark = false
        )
    )

    fun current(context: Context): String = VpnPrefs.experimentalTheme(context)

    fun themeFor(context: Context): ThemeDef? =
        themes.firstOrNull { it.id == current(context) }

    fun styleFor(context: Context): Int = themeFor(context)?.styleRes ?: 0

    fun isExperimental(context: Context): Boolean = styleFor(context) != 0

    fun accentStyleFor(context: Context): Int {
        val hex = VpnPrefs.accentColor(context)
        if (hex.isEmpty()) return 0
        return accents.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.styleRes ?: 0
    }

    fun applyAccent(context: Context) {
        val styleRes = accentStyleFor(context)
        if (styleRes != 0) context.theme.applyStyle(styleRes, true)
    }

    fun parseColor(hex: String): Int? = try {
        Color.parseColor(hex)
    } catch (e: Exception) {
        null
    }

    fun accentOrDefaultColor(context: Context, fallback: Int): Int {
        val hex = VpnPrefs.accentColor(context)
        if (hex.isEmpty()) return fallback
        return parseColor(hex) ?: fallback
    }

    fun applyAccentOverlay(activity: AppCompatActivity, hex: String) {
        val accent = parseColor(hex) ?: return
        tintViewTree(activity.window.decorView, accent)
    }

    fun applyAccentOverlay(root: View, hex: String) {
        val accent = parseColor(hex) ?: return
        tintViewTree(root, accent)
    }

    fun applyCurrentAccent(view: View) {
        val hex = VpnPrefs.accentColor(view.context)
        if (hex.isNotEmpty()) applyAccentOverlay(view, hex)
    }

    fun applyAccentToDialog(dialog: Dialog) {
        val window = dialog.window ?: return
        val accent = parseColor(VpnPrefs.accentColor(dialog.context))

        val density = dialog.context.resources.displayMetrics.density
        val ta = dialog.context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        val bgColor = ta.getColor(0, Color.BLACK)
        ta.recycle()

        val shape = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16 * density
            if (accent != null) {
                setStroke((1.5 * density).toInt(), accent)
            }
        }
        window.setBackgroundDrawable(shape)

        if (accent == null) return
        tintViewTree(window.decorView, accent)

        fun clearButtonBackground(view: View) {
            if (view is Button) {
                view.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                view.setTextColor(accent)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) clearButtonBackground(view.getChildAt(i))
            }
        }
        clearButtonBackground(window.decorView)
    }

    private fun tintViewTree(root: View, accent: Int) {
        val ta = root.context.theme.obtainStyledAttributes(
            intArrayOf(
                com.google.android.material.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnSurface
            )
        )
        val primary = ta.getColor(0, Color.BLACK)
        val onSurface = ta.getColor(1, Color.DKGRAY)
        ta.recycle()

        val onAccent = if (Color.luminance(accent) > 0.5f) Color.BLACK else Color.WHITE
        val uncheckedTrack = (onSurface and 0x00FFFFFF) or 0x33000000.toInt()

        fun tint(view: View) {
            when {
                view is MaterialSwitch -> {
                    view.trackTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(accent, uncheckedTrack)
                    )
                    view.thumbTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(onAccent, onSurface)
                    )
                    view.thumbIconTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(onAccent, onSurface)
                    )
                }
                view is SwitchCompat -> {
                    view.trackTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(accent, uncheckedTrack)
                    )
                    view.thumbTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(onAccent, onSurface)
                    )
                }
                view is Switch -> {
                    view.trackTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(accent, uncheckedTrack)
                    )
                    view.thumbTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(onAccent, onSurface)
                    )
                }
                view is RadioButton -> view.buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(accent, onSurface)
                )
                view is CheckBox -> view.buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(accent, onSurface)
                )
                view is MaterialButton -> {
                    val shapeBg = view.background as? MaterialShapeDrawable
                    val hasStroke = shapeBg?.strokeColor?.defaultColor?.let { Color.alpha(it) > 0 } == true
                    if (hasStroke) {
                        view.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        view.strokeColor = ColorStateList.valueOf(accent)
                        view.setTextColor(accent)
                    } else if (shapeBg?.fillColor?.defaultColor?.let { Color.alpha(it) > 0 } == true) {
                        view.backgroundTintList = ColorStateList.valueOf(accent)
                        view.setTextColor(onAccent)
                    } else {
                        view.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        view.setTextColor(accent)
                    }
                }
                view is Spinner -> view.backgroundTintList = ColorStateList.valueOf(accent)
                view is Button -> view.backgroundTintList = ColorStateList.valueOf(accent)
                view is EditText -> view.backgroundTintList = ColorStateList.valueOf(accent)
                view is TextView -> if (view.currentTextColor == primary) {
                    view.setTextColor(accent)
                }
                view is ImageView -> view.imageTintList = ColorStateList.valueOf(accent)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) tint(view.getChildAt(i))
            }
        }
        tint(root)
    }
}
