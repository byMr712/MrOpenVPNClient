package com.mropenovpn.client.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnUsers
import de.blinkt.openvpn.core.VpnStatus

class UsersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.addUserButton).setOnClickListener { showAddUserDialog() }

        renderUsers()
    }

    override fun onResume() {
        super.onResume()
        renderUsers()
    }

    private fun renderUsers() {
        val container = findViewById<LinearLayout>(R.id.usersContainer)
        container.removeAllViews()

        val users = VpnUsers.users(this)
        if (users.isEmpty()) {
            container.addView(
                TextView(this).apply {
                    setText(R.string.no_users)
                    setPadding(16, 16, 16, 16)
                    setTextAppearance(android.R.style.TextAppearance_Material_Body1)
                }
            )
            return
        }

        for (login in users) {
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_user, container, false)
            row.findViewById<TextView>(R.id.userNameText).text = login
            row.findViewById<ImageButton>(R.id.deleteUserButton).setOnClickListener {
                confirmDelete(login)
            }
            container.addView(row)
        }
    }

    private fun showAddUserDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val userInput = view.findViewById<EditText>(R.id.newUsername)
        val passInput = view.findViewById<EditText>(R.id.newPassword)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_user)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val username = userInput.text.toString().trim()
                val password = passInput.text.toString()
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, R.string.credentials_required, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                VpnUsers.save(this, username, password)
                VpnStatus.logInfo(R.string.user_added, username)
                renderUsers()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun confirmDelete(login: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_user)
            .setMessage(getString(R.string.delete_user_confirm, login))
            .setPositiveButton(R.string.delete) { _, _ ->
                VpnUsers.delete(this, login)
                VpnStatus.logInfo(R.string.user_deleted, login)
                renderUsers()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }
}
