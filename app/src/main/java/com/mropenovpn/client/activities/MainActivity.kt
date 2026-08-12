package com.mropenovpn.client.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.mropenovpn.client.BaseActivity
import com.mropenovpn.client.ExperimentalThemes
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

class MainActivity : BaseActivity(), VpnStatus.StateListener {

    private lateinit var statusLevelText: TextView
    private lateinit var statusCard: com.google.android.material.card.MaterialCardView
    private lateinit var statusOutlineAnimator: StatusOutlineAnimator
    private lateinit var profileList: RecyclerView
    private lateinit var adapter: ProfileAdapter
    private lateinit var drawerLayout: DrawerLayout

    private var pendingProfile: VpnProfile? = null
    private var lastLevel: ConnectionStatus? = null

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
        statusCard = findViewById(R.id.statusCard)
        statusOutlineAnimator = StatusOutlineAnimator(this, statusCard)
        profileList = findViewById(R.id.profileList)

        adapter = ProfileAdapter(
            onConnect = ::connectToProfile,
            onDisconnect = ::disconnectCurrent,
            onSelectUser = ::showUserPicker
        )
        profileList.layoutManager = LinearLayoutManager(this)
        profileList.adapter = adapter

        findViewById<Button>(R.id.addProfileButton).setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.copyLogButton).setOnClickListener {
            copyLogToClipboard()
        }

        applyExperimentalTheme()

        findViewById<View>(R.id.statusCard).setOnClickListener {
            if (VpnStatus.isVPNActive()) {
                disconnectCurrent()
            } else {
                val profile = VpnPrefs.lastProfileUuid(this)?.let { uuid ->
                    ProfileManager.getInstance(this)
                        .getProfiles()
                        .firstOrNull { it.uuid.toString() == uuid }
                }
                if (profile != null) {
                    connectToProfile(profile)
                } else {
                    Toast.makeText(this, R.string.no_profiles, Toast.LENGTH_SHORT).show()
                }
            }
        }

        OpenVPNService.setNotificationActivityClass(MainActivity::class.java)

        requestNotificationPermissionIfNeeded()
        refreshProfileList()
        VpnTileManager.sync(this)
        updateStatusUi()
        updateDebugUi()

        setupDrawer()
        autoConnectIfEnabled()

        intent?.getStringExtra(EXTRA_AUTO_CONNECT_PROFILE_UUID)?.let { uuid ->
            ProfileManager.getInstance(this)
                .getProfiles()
                .firstOrNull { it.uuid.toString() == uuid }
                ?.let { connectToProfile(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDebugUi()
        updateStatusUi()
    }

    private fun updateDebugUi() {
        findViewById<Button>(R.id.copyLogButton).visibility =
            if (VpnPrefs.debugMode(this)) View.VISIBLE else View.GONE
    }

    private fun applyExperimentalTheme() {
        val themeId = VpnPrefs.experimentalTheme(this)
        if (themeId.isEmpty()) return
        val density = resources.displayMetrics.density
        val card = statusCard
        when (themeId) {
            "neon" -> {
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(16 * density)
                    .build()
            }
            "oled" -> {
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT))
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes((16 * density))
                    .build()
            }
            "redline" -> {
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes((16 * density))
                    .build()
            }
            "paper" -> {
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes((28 * density))
                    .build()
            }
            "mint" -> {
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes((28 * density))
                    .build()
            }
        }
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        applyNavAccent(navView)

        val currentLang = VpnPrefs.language(this)
        val languageEntryText = findViewById<TextView>(R.id.languageEntryText)
        languageEntryText.text = if (currentLang == "ru") getString(R.string.language_ru)
        else getString(R.string.language_en)

        findViewById<View>(R.id.languageEntry).setOnClickListener {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            val next = if (VpnPrefs.language(this) == "ru") "en" else "ru"
            setAppLanguage(next)
        }

        findViewById<android.widget.ImageButton>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        findViewById<View>(R.id.settingsEntry).setOnClickListener {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.aboutEntry).setOnClickListener {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            showAboutDialog()
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profiles -> drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                R.id.nav_users -> {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    startActivity(Intent(this, UsersActivity::class.java))
                }
            }
            true
        }
    }

    private fun setAppLanguage(tag: String) {
        VpnPrefs.setLanguage(this, tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun applyNavAccent(navView: NavigationView) {
        val accent = ExperimentalThemes.accentOrDefaultColor(
            this,
            themeColor(com.google.android.material.R.attr.colorPrimary)
        )

        val itemBackground = StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                ColorDrawable(0x33FFFFFF and (accent and 0x00FFFFFF))
            )
            addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        }
        navView.itemBackground = itemBackground

        val color = ColorStateList.valueOf(accent)
        navView.itemTextColor = color
        navView.itemIconTintList = color
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
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        view.findViewById<TextView>(R.id.aboutTitle).text = getString(R.string.app_name)
        view.findViewById<TextView>(R.id.aboutVersion).text =
            getString(R.string.about_version, version)
        view.findViewById<TextView>(R.id.aboutSource).setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/byMr712/MrOpenVPNClient"))
            )
        }
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.close, null)
            .show()
            .also { ExperimentalThemes.applyAccentToDialog(it) }
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
        statusOutlineAnimator.stop()
        adapter.stopAnimations()
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
            updateStatusLevel(level)
            adapter.setLevel(level)
        }
    }

    private fun isConnecting(level: ConnectionStatus): Boolean =
        level == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED ||
            level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ||
            level == ConnectionStatus.LEVEL_START ||
            level == ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT ||
            level == ConnectionStatus.LEVEL_VPNPAUSED

    private fun updateStatusLevel(level: ConnectionStatus) {
        lastLevel = level
        val error = level == ConnectionStatus.LEVEL_AUTH_FAILED ||
            level == ConnectionStatus.LEVEL_NONETWORK
        statusLevelText.text = when {
            level == ConnectionStatus.LEVEL_CONNECTED -> getString(R.string.state_connected)
            error -> getString(R.string.state_error)
            isConnecting(level) -> getString(R.string.state_connecting)
            else -> getString(R.string.state_disconnected)
        }
        statusLevelText.setTextColor(
            ExperimentalThemes.accentOrDefaultColor(
                this,
                themeColor(com.google.android.material.R.attr.colorPrimary)
            )
        )
        statusOutlineAnimator.setState(outlineStateFor(level))
        adapter.setLevel(level)
    }

    private fun outlineStateFor(level: ConnectionStatus): StatusOutlineAnimator.State =
        when {
            level == ConnectionStatus.LEVEL_CONNECTED -> StatusOutlineAnimator.State.CONNECTED
            isConnecting(level) -> StatusOutlineAnimator.State.CONNECTING
            else -> StatusOutlineAnimator.State.DISCONNECTED
        }

    private fun themeColor(attr: Int): Int {
        val ta = obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
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

    private fun showUserPicker(profile: VpnProfile) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_user_picker, null)
        val userSelect = view.findViewById<Spinner>(R.id.userSelect)
        val profileNameInput = view.findViewById<EditText>(R.id.profileNameInput)
        profileNameInput.setText(profile.mName)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.profile_edit)
            .setView(view)
            .create()

        fun refreshUserSelect(select: String? = null) {
            val users = VpnUsers.users(this)
            val items = mutableListOf(getString(R.string.no_account)).apply { addAll(users) }
            userSelect.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                items
            )
            val target = select ?: profile.mUsername
            val index = if (target.isNotEmpty()) users.indexOf(target) else -1
            userSelect.setSelection(if (index >= 0) index + 1 else 0)
        }

        refreshUserSelect()

        view.findViewById<Button>(R.id.pickAddUserButton).setOnClickListener {
            showAddUserAndSelect { login -> refreshUserSelect(login) }
        }

        view.findViewById<Button>(R.id.deleteProfileButton).setOnClickListener {
            dialog.dismiss()
            confirmDeleteProfile(profile)
        }

        view.findViewById<Button>(R.id.closeProfileButton).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.saveProfileButton).setOnClickListener {
            val users = VpnUsers.users(this)
            val selected = userSelect.selectedItemPosition
            if (selected <= 0) {
                profile.mUsername = ""
                profile.mPassword = ""
            } else {
                val login = users[selected - 1]
                profile.mUsername = login
                profile.mPassword = VpnUsers.password(this, login) ?: ""
                VpnStatus.logInfo(R.string.user_selected, login)
            }
            val newName = profileNameInput.text.toString().trim()
            if (newName.isNotEmpty() && newName != profile.mName) {
                profile.mName = uniqueName(newName)
                VpnStatus.logInfo(R.string.profile_renamed, profile.mName)
            }
            ProfileManager.saveProfile(this, profile)
            refreshProfileList()
            dialog.dismiss()
        }

        dialog.show()
        ExperimentalThemes.applyAccentToDialog(dialog)
    }

    private fun confirmDeleteProfile(profile: VpnProfile) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_profile, null)
        view.findViewById<TextView>(R.id.deleteMessageText).text =
            getString(R.string.delete_profile) + "\n" +
                getString(R.string.delete_profile_confirm_name, profile.mName)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.confirmDeleteButton).setOnClickListener {
            if (VpnStatus.isVPNActive() &&
                profile.uuid.toString() == VpnStatus.getLastConnectedVPNProfile()
            ) {
                disconnectCurrent()
            }
            ProfileManager.getInstance(this).removeProfile(this, profile)
            VpnStatus.logInfo(R.string.profile_deleted, profile.mName)
            refreshProfileList()
            VpnTileManager.sync(this)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.cancelDeleteButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        ExperimentalThemes.applyAccentToDialog(dialog)
    }

    private fun showAddUserAndSelect(onUserAdded: (String) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_user)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val username = view.findViewById<EditText>(R.id.newUsername).text.toString().trim()
                val password = view.findViewById<EditText>(R.id.newPassword).text.toString()
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, R.string.credentials_required, Toast.LENGTH_LONG).show()
                } else {
                    VpnUsers.save(this, username, password)
                    VpnStatus.logInfo(R.string.user_added, username)
                    onUserAdded(username)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
            .also { ExperimentalThemes.applyAccentToDialog(it) }
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
            .also { ExperimentalThemes.applyAccentToDialog(it) }
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
            profile.mName = nextNumberedName()

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

    private fun nextNumberedName(): String {
        val names = ProfileManager.getInstance(this)
            .getProfiles()
            .map { it.mName }
            .toMutableSet()
        var i = 1
        while ("$i" in names) i++
        return i.toString()
    }

    private fun refreshProfileList() {
        val all = ProfileManager.getInstance(this).getProfiles()
        val byUuid = all.associateBy { it.uuid.toString() }
        val storedOrder = VpnPrefs.profileOrder(this)
        val known = storedOrder.filter { byUuid.containsKey(it) }
        val missing = all.filter { it.uuid.toString() !in storedOrder }
        if (known.size != storedOrder.size || missing.isNotEmpty()) {
            VpnPrefs.setProfileOrder(this, known + missing.map { it.uuid.toString() })
        }
        adapter.setProfiles(known.mapNotNull { byUuid[it] } + missing)
    }

    private fun updateStatusUi() {
        val level = lastLevel ?: ConnectionStatus.LEVEL_NOTCONNECTED
        statusLevelText.text = getString(
            when {
                level == ConnectionStatus.LEVEL_CONNECTED -> R.string.state_connected
                isConnecting(level) -> R.string.state_connecting
                else -> R.string.state_disconnected
            }
        )
        statusLevelText.setTextColor(
            ExperimentalThemes.accentOrDefaultColor(
                this,
                themeColor(com.google.android.material.R.attr.colorPrimary)
            )
        )
        statusOutlineAnimator.setState(outlineStateFor(level))
        adapter.setLevel(level)
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
