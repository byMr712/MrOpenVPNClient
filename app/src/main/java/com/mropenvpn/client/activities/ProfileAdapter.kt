package com.mropenvpn.client.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mropenvpn.client.ExperimentalThemes
import com.mropenvpn.client.R
import com.mropenvpn.client.VpnPrefs
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus

class ProfileAdapter(
    private val onConnect: (VpnProfile) -> Unit,
    private val onDisconnect: () -> Unit,
    private val onSelectUser: (VpnProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    private val profiles = mutableListOf<VpnProfile>()
    private var currentLevel: ConnectionStatus = ConnectionStatus.LEVEL_NOTCONNECTED
    private val animators = mutableListOf<StatusOutlineAnimator>()

    fun setProfiles(newProfiles: List<VpnProfile>) {
        profiles.clear()
        profiles.addAll(newProfiles)
        notifyDataSetChanged()
    }

    fun setLevel(level: ConnectionStatus) {
        currentLevel = level
        notifyDataSetChanged()
    }

    fun stopAnimations() {
        animators.forEach { it.stop() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        val holder = ProfileViewHolder(view)
        animators.add(holder.animator)
        return holder
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

        val animator = StatusOutlineAnimator(itemView.context, card) { ctx ->
            if (VpnPrefs.animSyncStatus(ctx)) {
                VpnPrefs.statusOutlineAnim(ctx)
            } else {
                VpnPrefs.profileOutlineAnim(ctx)
            }
        }

        fun bind(profile: VpnProfile) {
            nameText.text = itemView.context.getString(R.string.profile_name_format, profile.mName)
            itemView.setOnClickListener { onSelectUser(profile) }
            if (profile.mUsername.isNotEmpty()) {
                userText.text = itemView.context.getString(R.string.user_name_format, profile.mUsername)
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
            animator.setState(outlineStateFor(active))
        }

        private fun outlineStateFor(active: Boolean): StatusOutlineAnimator.State =
            if (!active) {
                StatusOutlineAnimator.State.DISCONNECTED
            } else {
                when (currentLevel) {
                    ConnectionStatus.LEVEL_CONNECTED -> StatusOutlineAnimator.State.CONNECTED
                    else -> StatusOutlineAnimator.State.CONNECTING
                }
            }
    }
}
