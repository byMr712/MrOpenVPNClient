package com.mropenovpn.client.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.mropenovpn.client.R
import com.mropenovpn.client.VpnPrefs
import com.mropenovpn.client.VpnTileManager
import com.mropenovpn.client.VpnUsers
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), VpnStatus.StateListener {

    private lateinit var statusLevelText: TextView
    private lateinit var statusMessageText: TextView
    private lateinit var profileList: RecyclerView
    private lateinit var adapter: ProfileAdapter
    private lateinit var drawerLayout: DrawerLayout

    private var pendingProfile: VpnProfile? = null

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importProfile(it) }
        }

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val profile = pendingProfile
            pendingProfile = null
            if (result.resultCode == Activity.RESULT_OK && profile != null) {
                startVpn(profile)
            } else {
                Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Optional permission, no further action needed.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusLevelText = findViewById(R.id.statusLevelText)
        statusMessageText = findViewById(R.id.statusMessageText)
        profileList = findViewById(R.id.profileList)

        adapter = ProfileAdapter(
            onConnect = ::connectToProfile,
            onDisconnect = ::disconnectCurrent
        )
        profileList.layoutManager = LinearLayoutManager(this)
        profileList.adapter = adapter

        findViewById<Button>(R.id.importButton).setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.copyLogButton).setOnClickListener {
            copyLogToClipboard()
        }

        OpenVPNService.setNotificationActivityClass(MainActivity::class.java)

        requestNotificationPermissionIfNeeded()
        refreshProfileList()
        VpnTileManager.sync(this)
        updateStatusUi(VpnStatus.getLastCleanLogMessage(this))

        setupDrawer()
        autoConnectIfEnabled()

        intent?.getStringExtra(EXTRA_AUTO_CONNECT_PROFILE_UUID)?.let { uuid ->
            ProfileManager.getInstance(this)
                .getProfiles()
                .firstOrNull { it.uuid.toString() == uuid }
                ?.let { connectToProfile(it) }
        }
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        navView.setCheckedItem(R.id.nav_profiles)

        findViewById<android.widget.ImageButton>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profiles -> drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                R.id.nav_users -> {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    startActivity(Intent(this, UsersActivity::class.java))
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_about -> {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    showAboutDialog()
                }
            }
            true
        }
    }

    private fun autoConnectIfEnabled() {
        if (intent?.getStringExtra(EXTRA_AUTO_CONNECT_PROFILE_UUID) != null) return
        if (!VpnPrefs.autoConnect(this)) return
        VpnPrefs.lastProfileUuid(this)?.let { uuid ->
            ProfileManager.getInstance(this)
                .getProfiles()
                .firstOrNull { it.uuid.toString() == uuid }
                ?.let { connectToProfile(it) }
        }
    }

    private fun showAboutDialog() {
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "1.0"
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.about_message, version))
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_AUTO_CONNECT_PROFILE_UUID)?.let { uuid ->
            ProfileManager.getInstance(this)
                .getProfiles()
                .firstOrNull { it.uuid.toString() == uuid }
                ?.let { connectToProfile(it) }
        }
    }

    override fun onStart() {
        super.onStart()
        VpnStatus.addStateListener(this)
    }

    override fun onStop() {
        VpnStatus.removeStateListener(this)
        super.onStop()
    }

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        level: ConnectionStatus,
        intent: Intent?
    ) {
        runOnUiThread {
            statusLevelText.text = when (level) {
                ConnectionStatus.LEVEL_CONNECTED -> getString(R.string.state_connected)
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED ->
                    getString(R.string.state_connecting_server_replied)
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ->
                    getString(R.string.state_connecting)
                ConnectionStatus.LEVEL_AUTH_FAILED -> getString(R.string.state_auth_failed)
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT ->
                    getString(R.string.state_waiting_input)
                ConnectionStatus.LEVEL_NONETWORK -> getString(R.string.state_no_network)
                ConnectionStatus.LEVEL_VPNPAUSED -> getString(R.string.state_paused)
                ConnectionStatus.LEVEL_START -> getString(R.string.state_starting)
                ConnectionStatus.LEVEL_NOTCONNECTED -> getString(R.string.state_disconnected)
                else -> getString(R.string.state_disconnected)
            }
            statusMessageText.text = logmessage.ifBlank { VpnStatus.getLastCleanLogMessage(this) }
            adapter.notifyDataSetChanged()
        }
    }

    override fun setConnectedVPN(uuid: String?) {
        runOnUiThread {
            refreshProfileList()
        }
    }

    private fun connectToProfile(profile: VpnProfile) {
        VpnPrefs.setLastProfileUuid(this, profile.uuid.toString())
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            pendingProfile = profile
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            startVpn(profile)
        }
    }

    private fun startVpn(profile: VpnProfile) {
        if (profile.needUserPWInput(null, null) != 0) {
            showCredentialsDialog(profile)
            return
        }
        try {
            VpnStatus.logDebug("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            val cfg = profile.getConfigFile(applicationContext, false)
            VpnStatus.logDebug("---- generated config ----")
            cfg.lineSequence().forEach { VpnStatus.logDebug("cfg| $it") }
            VpnStatus.logDebug("---- end config ----")

            VPNLaunchHelper.startOpenVpn(profile, applicationContext, "user request", true)
            statusLevelText.text = getString(R.string.state_starting)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                e.localizedMessage ?: getString(R.string.vpn_start_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showCredentialsDialog(profile: VpnProfile) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_credentials, null)
        val userSelect = view.findViewById<Spinner>(R.id.userSelect)
        val userInput = view.findViewById<EditText>(R.id.usernameInput)
        val passInput = view.findViewById<EditText>(R.id.passwordInput)
        val rememberUser = view.findViewById<CheckBox>(R.id.rememberUser)

        val savedUsers = VpnUsers.users(this)
        val userItems = mutableListOf(getString(R.string.new_user)).apply { addAll(savedUsers) }
        userSelect.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            userItems
        )

        userInput.setText(profile.mUsername)
        val existingIndex = savedUsers.indexOf(profile.mUsername)
        if (existingIndex >= 0) {
            userSelect.setSelection(existingIndex + 1)
            VpnUsers.password(this, profile.mUsername)?.let { passInput.setText(it) }
        }

        userSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val login = savedUsers[position - 1]
                    userInput.setText(login)
                    VpnUsers.password(this@MainActivity, login)?.let { passInput.setText(it) }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.credentials_title)
            .setView(view)
            .setPositiveButton(R.string.connect) { _, _ ->
                val username = userInput.text.toString().trim()
                val password = passInput.text.toString()
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, R.string.credentials_required, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (rememberUser.isChecked) {
                    VpnUsers.save(this, username, password)
                }
                profile.mUsername = username
                profile.mPassword = password
                ProfileManager.saveProfile(this, profile)
                startVpn(profile)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun copyLogToClipboard() {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        val sb = StringBuilder()
        for (item in VpnStatus.getlogbuffer()) {
            val time = format.format(java.util.Date(item.logtime))
            sb.append(time).append(" [").append(item.logLevel).append("] ")
                .append(item.getString(this)).append('\n')
        }
        val text = sb.toString().ifBlank { getString(R.string.status_no_connection_message) }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("vpn log", text))
        val lines = text.lineSequence().count()
        Toast.makeText(this, getString(R.string.log_copied, lines), Toast.LENGTH_SHORT).show()
    }

    private fun disconnectCurrent() {
        val intent = Intent(this, OpenVPNService::class.java)
        intent.action = OpenVPNService.DISCONNECT_VPN
        startService(intent)
    }

    private fun importProfile(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Not critical for a one-time import.
        }

        try {
            val parser = ConfigParser()
            val input = contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open profile file")
            input.use {
                parser.parseConfig(BufferedReader(InputStreamReader(it)))
            }
            val profile = parser.convertProfile()
            profile.mName = uniqueName(profile.mName.ifBlank { "MrOpenVPN Profile" })

            val pm = ProfileManager.getInstance(this)
            pm.addProfile(profile)
            pm.saveProfileList(this)
            ProfileManager.saveProfile(this, profile)

            refreshProfileList()
            VpnTileManager.sync(this)
            Toast.makeText(
                this,
                getString(R.string.import_success, profile.mName),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.import_error,
                    e.localizedMessage ?: e.javaClass.simpleName
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun uniqueName(base: String): String {
        val names = ProfileManager.getInstance(this)
            .getProfiles()
            .map { it.mName }
            .toMutableSet()
        if (base !in names) return base
        var i = 2
        while ("$base ($i)" in names) i++
        return "$base ($i)"
    }

    private fun refreshProfileList() {
        val profiles = ProfileManager.getInstance(this)
            .getProfiles()
            .sortedBy { it.mName }
        adapter.setProfiles(profiles)
    }

    private fun updateStatusUi(message: String) {
        statusLevelText.text =
            if (VpnStatus.isVPNActive()) getString(R.string.state_connected)
            else getString(R.string.state_disconnected)
        statusMessageText.text = message
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_AUTO_CONNECT_PROFILE_UUID = "auto_connect_profile_uuid"
    }
}
