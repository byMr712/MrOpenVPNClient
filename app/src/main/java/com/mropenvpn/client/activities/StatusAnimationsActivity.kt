package com.mropenvpn.client.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.mropenvpn.client.BaseActivity
import com.mropenvpn.client.ExperimentalThemes
import com.mropenvpn.client.R
import com.mropenvpn.client.VpnPrefs

class StatusAnimationsActivity : BaseActivity() {

    companion object {
        const val EXTRA_TARGET = "animation_target"
        const val TARGET_STATUS = "status"
        const val TARGET_PROFILE = "profile"
    }

    private var previewAnim: String? = null
    private var previewAnimator: StatusOutlineAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_animations)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_STATUS
        findViewById<TextView>(R.id.titleText).setText(
            if (target == TARGET_PROFILE) {
                R.string.profile_animations_title
            } else {
                R.string.status_animations_title
            }
        )

        val container = findViewById<LinearLayout>(R.id.optionsContainer)

        fun rebuild(currentId: String) {
            previewAnim = currentId
            container.removeAllViews()
            StatusOutlineAnimator.variants.forEach { id ->
                container.addView(
                    buildOption(id, selected = id == currentId) {
                        if (id != currentId) {
                            if (target == TARGET_PROFILE) {
                                VpnPrefs.setProfileOutlineAnim(this, id)
                            } else {
                                VpnPrefs.setStatusOutlineAnim(this, id)
                            }
                            rebuild(id)
                        }
                    }
                )
            }
            addPreview(container, target)
        }

        rebuild(
            if (target == TARGET_PROFILE) {
                VpnPrefs.profileOutlineAnim(this)
            } else {
                VpnPrefs.statusOutlineAnim(this)
            }
        )
    }

    override fun onDestroy() {
        previewAnimator?.stop()
        super.onDestroy()
    }

    private fun addPreview(container: LinearLayout, target: String) {
        val density = resources.displayMetrics.density
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (target == TARGET_STATUS) {
                    (120 * density).toInt()
                } else {
                    (72 * density).toInt()
                }
            ).apply {
                topMargin = (8 * density).toInt()
            }
            radius = if (target == TARGET_STATUS) (16 * density) else (12 * density)
            isClickable = false
            isFocusable = false
        }

        previewAnimator?.stop()
        previewAnimator = StatusOutlineAnimator(this, card) { previewAnim }
        previewAnimator?.setState(StatusOutlineAnimator.State.CONNECTING)

        container.addView(card)
    }

    private fun buildOption(id: String, selected: Boolean, onClick: () -> Unit): MaterialCardView {
        val density = resources.displayMetrics.density
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            radius = (12 * density)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (16 * density).toInt(),
                (12 * density).toInt(),
                (8 * density).toInt(),
                (12 * density).toInt()
            )
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = nameFor(id)
            setTextAppearance(
                this@StatusAnimationsActivity,
                com.google.android.material.R.style.TextAppearance_Material3_TitleSmall
            )
            setTextColor(accentColor())
        })
        texts.addView(TextView(this).apply {
            text = descFor(id)
            setTextAppearance(
                this@StatusAnimationsActivity,
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            )
            setTextColor(accentColor())
        })

        val radio = MaterialRadioButton(this).apply {
            isClickable = false
            isChecked = selected
        }

        row.addView(texts)
        row.addView(radio)
        card.addView(row)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun themeColor(): Int {
        val ta = obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurface))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }

    private fun accentColor(): Int =
        ExperimentalThemes.accentOrDefaultColor(this, themeColor())

    private fun nameFor(id: String): String =
        when (id) {
            "off" -> getString(R.string.anim_off_name)
            "pulse" -> getString(R.string.anim_pulse_name)
            "blink" -> getString(R.string.anim_blink_name)
            "rainbow" -> getString(R.string.anim_rainbow_name)
            else -> getString(R.string.anim_throb_name)
        }

    private fun descFor(id: String): String =
        when (id) {
            "off" -> getString(R.string.anim_off_desc)
            "pulse" -> getString(R.string.anim_pulse_desc)
            "blink" -> getString(R.string.anim_blink_desc)
            "rainbow" -> getString(R.string.anim_rainbow_desc)
            else -> getString(R.string.anim_throb_desc)
        }
}
