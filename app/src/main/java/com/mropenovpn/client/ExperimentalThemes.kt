package com.mropenovpn.client

import android.content.Context

object ExperimentalThemes {

    data class ThemeDef(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val styleRes: Int
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

    val themes = listOf(
        ThemeDef(
            id = "neon",
            nameRes = R.string.theme_neon_name,
            descRes = R.string.theme_neon_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Neon
        ),
        ThemeDef(
            id = "oled",
            nameRes = R.string.theme_oled_name,
            descRes = R.string.theme_oled_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Oled
        ),
        ThemeDef(
            id = "paper",
            nameRes = R.string.theme_paper_name,
            descRes = R.string.theme_paper_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Paper
        ),
        ThemeDef(
            id = "redline",
            nameRes = R.string.theme_redline_name,
            descRes = R.string.theme_redline_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Redline
        ),
        ThemeDef(
            id = "mint",
            nameRes = R.string.theme_mint_name,
            descRes = R.string.theme_mint_desc,
            styleRes = R.style.Theme_MrOpenVPNClient_Mint
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
}
