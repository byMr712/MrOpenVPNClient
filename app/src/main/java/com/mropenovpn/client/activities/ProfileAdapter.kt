package com.mropenovpn.client.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mropenovpn.client.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.VpnStatus

class ProfileAdapter(
    private val onConnect: (VpnProfile) -> Unit,
    private val onDisconnect: () -> Unit
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
    }

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.profileNameText)
        private val userText: TextView = itemView.findViewById(R.id.profileUserText)
        private val connectButton: Button = itemView.findViewById(R.id.connectButton)
        private val disconnectButton: Button = itemView.findViewById(R.id.disconnectButton)

        fun bind(profile: VpnProfile) {
            nameText.text = profile.mName
            if (profile.mUsername.isNotEmpty()) {
                userText.text = profile.mUsername
                userText.visibility = View.VISIBLE
            } else {
                userText.visibility = View.GONE
            }
            val active = VpnStatus.isVPNActive()
            connectButton.isEnabled = !active
            disconnectButton.isEnabled = active
            connectButton.setOnClickListener { onConnect(profile) }
            disconnectButton.setOnClickListener { onDisconnect() }
        }
    }
}
