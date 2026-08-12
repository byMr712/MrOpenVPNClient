package com.mropenovpn.client.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.R
import de.blinkt.openvpn.core.VpnStatus

class LogWindow : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = TextView(this).apply {
            text = VpnStatus.getLastCleanLogMessage(this@LogWindow)
            textSize = 12f
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this).apply { addView(message) }
        val close = Button(this).apply {
            setText(R.string.close)
            setOnClickListener { finish() }
        }

        val root = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            addView(
                scroll,
                androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                    androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(close)
        }
        setContentView(root)
    }
}
