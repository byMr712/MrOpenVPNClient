package com.mropenovpn.client.activities

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.animation.LinearInterpolator
import com.google.android.material.card.MaterialCardView
import com.mropenovpn.client.ExperimentalThemes
import com.mropenovpn.client.VpnPrefs

class StatusOutlineAnimator(
    private val context: Context,
    private val card: MaterialCardView,
    private val animIdProvider: (Context) -> String? = { null }
) {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED }

    companion object {
        const val ANIM_OFF = "off"
        const val ANIM_PULSE = "pulse"
        const val ANIM_BLINK = "blink"
        const val ANIM_RAINBOW = "rainbow"
        const val ANIM_THROB = "throb"

        val variants = listOf(ANIM_OFF, ANIM_PULSE, ANIM_BLINK, ANIM_RAINBOW, ANIM_THROB)
    }

    private val gray = 0xFF808080.toInt()
    private val baseStrokeWidth = (2 * context.resources.displayMetrics.density).toInt()
    private val idleStrokeWidth = context.resources.displayMetrics.density.toInt()

    private var animator: ValueAnimator? = null

    fun setState(state: State) {
        stopAnimation()
        when (state) {
            State.DISCONNECTED -> setIdleOutline()
            State.CONNECTED -> setStatic(accentColor())
            State.CONNECTING -> startAnimation(
                animIdProvider(context) ?: VpnPrefs.statusOutlineAnim(context)
            )
        }
    }

    fun stop() {
        stopAnimation()
    }

    private fun setStatic(color: Int) {
        card.strokeWidth = baseStrokeWidth
        card.setStrokeColor(ColorStateList.valueOf(color))
    }

    private fun setIdleOutline() {
        card.strokeWidth = idleStrokeWidth
        card.setStrokeColor(ColorStateList.valueOf(outlineVariantColor()))
    }

    private fun outlineVariantColor(): Int {
        val ta = context.theme.obtainStyledAttributes(
            intArrayOf(com.google.android.material.R.attr.colorOutlineVariant)
        )
        val color = ta.getColor(0, gray)
        ta.recycle()
        return color
    }

    private fun startAnimation(id: String) {
        when (id) {
            ANIM_PULSE -> startPulse()
            ANIM_BLINK -> startBlink()
            ANIM_RAINBOW -> startRainbow()
            ANIM_THROB -> startThrob()
            else -> setIdleOutline()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        card.strokeWidth = baseStrokeWidth
    }

    private fun startPulse() {
        val accent = accentColor()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { a ->
                card.setStrokeColor(
                    ColorStateList.valueOf(blend(gray, accent, a.animatedValue as Float))
                )
            }
            start()
        }
    }

    private fun startBlink() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { a ->
                val on = (a.animatedValue as Float) < 0.5f
                card.setStrokeColor(
                    ColorStateList.valueOf(if (on) accentColor() else Color.TRANSPARENT)
                )
            }
            start()
        }
    }

    private fun startRainbow() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { a ->
                card.setStrokeColor(
                    ColorStateList.valueOf(
                        Color.HSVToColor(floatArrayOf(a.animatedValue as Float, 1f, 1f))
                    )
                )
            }
            start()
        }
    }

    private fun startThrob() {
        animator = ValueAnimator.ofInt(baseStrokeWidth, baseStrokeWidth * 4).apply {
            duration = 800L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { a ->
                card.strokeWidth = a.animatedValue as Int
                card.setStrokeColor(ColorStateList.valueOf(accentColor()))
            }
            start()
        }
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    private fun accentColor(): Int =
        ExperimentalThemes.accentOrDefaultColor(context, primaryColor(context))

    private fun primaryColor(context: Context): Int {
        val ta = context.theme.obtainStyledAttributes(
            intArrayOf(com.google.android.material.R.attr.colorPrimary)
        )
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }
}
