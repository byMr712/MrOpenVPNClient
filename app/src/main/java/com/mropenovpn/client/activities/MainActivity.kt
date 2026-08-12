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
import android.widget.LinearLayout
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
        val card = findViewById<com.google.android.material.card.MaterialCardView>(R.id.statusCard)
        when (themeId) {
            "neon" -> {
                card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(16 * density)
                    .build()
            }
            "oled" -> {
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT))
                card.strokeWidth = (1 * density).toInt()
                card.setStrokeColor(ColorStateList.valueOf(Color.WHITE))
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

        val onSurface = themeColor(com.google.android.material.R.attr.colorOnSurface)
        val color = ColorStateList.valueOf(onSurface)
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
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateStatusLevel(level: ConnectionStatus) {
        val error = level == ConnectionStatus.LEVEL_AUTH_FAILED ||
            level == ConnectionStatus.LEVEL_NONETWORK
        val connecting = level == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED ||
            level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ||
            level == ConnectionStatus.LEVEL_START ||
            level == ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT ||
            level == ConnectionStatus.LEVEL_VPNPAUSED
        statusLevelText.text = when {
            level == ConnectionStatus.LEVEL_CONNECTED -> getString(R.string.state_connected)
            error -> getString(R.string.state_error)
            connecting -> getString(R.string.state_connecting)
            else -> getString(R.string.state_disconnected)
        }
        statusLevelText.setTextColor(
            if (error) {
                themeColor(com.google.android.material.R.attr.colorError)
            } else {
                ExperimentalThemes.accentOrDefaultColor(
                    this,
                    themeColor(com.google.android.material.R.attr.colorPrimary)
                )
            }
        )
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
        val list = view.findViewById<LinearLayout>(R.id.userPickerList)
        val users = VpnUsers.users(this)
        val current = profile.mUsername

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.select_user)
            .setView(view)
            .setNegativeButton(R.string.close, null)
            .create()

        if (users.isEmpty()) {
            list.addView(
                TextView(this).apply {
                    setText(R.string.no_users)
                    setPadding(16, 8, 16, 16)
                    setTextAppearance(this@MainActivity, com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                }
            )
        } else {
            users.sortedBy { it != current }.forEach { login ->
                list.addView(
                    com.google.android.material.radiobutton.MaterialRadioButton(this).apply {
                        text = login
                        isChecked = login == current
                        setOnClickListener {
                            selectUserForProfile(profile, login)
                            dialog.dismiss()
                        }
                    }
                )
            }
        }

        view.findViewById<Button>(R.id.pickAddUserButton).setOnClickListener {
            showAddUserAndSelect(profile) { dialog.dismiss() }
        }
        view.findViewById<Button>(R.id.pickRenameButton).setOnClickListener {
            showRenameDialog(profile) { dialog.dismiss() }
        }
        dialog.show()
        ExperimentalThemes.applyAccentToDialog(dialog)
    }

    private fun showRenameDialog(profile: VpnProfile, onDone: () -> Unit) {
        val input = EditText(this).apply {
            setText(profile.mName)
            setSingleLine(true)
            setSelectAllOnFocus(true)
            setPadding(24, 12, 24, 12)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_profile)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty() || newName == profile.mName) {
                    onDone()
                } else {
                    profile.mName = uniqueName(newName)
                    ProfileManager.saveProfile(this, profile)
                    VpnStatus.logInfo(R.string.profile_renamed, profile.mName)
                    refreshProfileList()
                    onDone()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
            .also { ExperimentalThemes.applyAccentToDialog(it) }
    }

    private fun selectUserForProfile(profile: VpnProfile, login: String) {
        profile.mUsername = login
        profile.mPassword = VpnUsers.password(this, login) ?: ""
        ProfileManager.saveProfile(this, profile)
        VpnStatus.logInfo(R.string.user_selected, login)
        refreshProfileList()
    }

    private fun showAddUserAndSelect(profile: VpnProfile, onDone: () -> Unit) {
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
                    selectUserForProfile(profile, username)
                    onDone()
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
            profile.mName = uniqueName(profile.mName.ifBlank { nextNumberedName() })

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
        val profiles = ProfileManager.getInstance(this)
            .getProfiles()
            .sortedBy { it.mName }
        adapter.setProfiles(profiles)
    }

    private fun updateStatusUi() {
        val active = VpnStatus.isVPNActive()
        statusLevelText.text = getString(
            if (active) R.string.state_connected else R.string.state_disconnected
        )
        statusLevelText.setTextColor(
            ExperimentalThemes.accentOrDefaultColor(
                this,
                themeColor(com.google.android.material.R.attr.colorPrimary)
            )
        )
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
