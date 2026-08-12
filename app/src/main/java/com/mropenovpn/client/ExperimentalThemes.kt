package com.mropenovpn.client

import android.content.Context

object ExperimentalThemes {

    data class ThemeDef(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val styleRes: Int
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
}
