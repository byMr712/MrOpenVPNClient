package com.mropenovpn.client.activities

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mropenovpn.client.ExperimentalThemes
import com.mropenovpn.client.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.VpnStatus

class ProfileAdapter(
    private val onConnect: (VpnProfile) -> Unit,
    private val onDisconnect: () -> Unit,
    private val onSelectUser: (VpnProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    private val profiles = mutableListOf<VpnProfile>()

    fun setProfiles(newProfiles: List<VpnProfile>) {
        profiles.clear()
        profiles.addAll(newProfiles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ProfileViewHolder(view)
    }

    override fun getItemCount(): Int = profiles.size

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(profiles[position])
        ExperimentalThemes.applyCurrentAccent(holder.itemView)
    }

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.profileNameText)
        private val userText: TextView = itemView.findViewById(R.id.profileUserText)
        private val connectButton: Button = itemView.findViewById(R.id.connectButton)
        private val card = itemView as MaterialCardView

        fun bind(profile: VpnProfile) {
            nameText.text = profile.mName
            itemView.setOnClickListener { onSelectUser(profile) }
            if (profile.mUsername.isNotEmpty()) {
                userText.text = profile.mUsername
                userText.visibility = View.VISIBLE
            } else {
                userText.visibility = View.GONE
            }
            val active = VpnStatus.isVPNActive() &&
                profile.uuid.toString() == VpnStatus.getLastConnectedVPNProfile()
            connectButton.text = itemView.context.getString(
                if (active) R.string.disconnect else R.string.connect
            )
            connectButton.setOnClickListener {
                if (active) onDisconnect() else onConnect(profile)
            }
            applyActiveOutline(active)
        }

        private fun applyActiveOutline(active: Boolean) {
            val density = itemView.resources.displayMetrics.density
            if (active) {
                card.strokeWidth = (2 * density).toInt()
                card.setStrokeColor(
                    ColorStateList.valueOf(
                        ExperimentalThemes.accentOrDefaultColor(
                            itemView.context,
                            primaryColor(itemView.context)
                        )
                    )
                )
            } else {
                card.strokeWidth = 0
            }
        }

        private fun primaryColor(context: Context): Int {
            val ta = context.theme.obtainStyledAttributes(
                intArrayOf(com.google.android.material.R.attr.colorPrimary)
            )
            val color = ta.getColor(0, Color.BLACK)
            ta.recycle()
            return color
        }
    }
}
