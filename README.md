# MrOpenVPN Client

**MrOpenVPN Client is an [OpenVPN](https://openvpn.net/) client for Android — a fork of the [ics-openvpn](https://github.com/schwabe/ics-openvpn) v0.7.64 project.**

- **Original core:** [Arne Schwabe](https://github.com/schwabe)
- **Custom shell:** Kotlin `app` module (`com.mropenvpn.client`)
- **License:** [GNU GPL v3](LICENSE)

A simple Android client that connects to an OpenVPN server using a ready-made `.ovpn` file — no extra configuration or registration required. Import a profile, click **Connect** — and it works.

## Features

- **Import any `.ovpn`/`.conf` profile** from the system file manager. The profile name is taken from the file name (the extension is stripped); on collision a `(2)`, `(3)` suffix is added automatically.
- **Connection status window** on the home screen with an animated accent outline, plus a **Disconnect** button.
- **Saved users (accounts):** store a login/password once, attach a user to a profile, or enter credentials at connect time. Interactive `auth-user-pass` requests are handled internally.
- **Experimental themes:** Black, White, Neon Grid, Lime Amoled, Paper Desk, Red Line, Mint — with an **accent color picker** (including custom HEX values, applied immediately).
- **Outline animations** for the status window and the selected profile: Pulse, Blink, Rainbow, Throb, or off — with an option to sync all animations to the status window.
- **Language selection** — English / Русский.
- **Persistent notification** with connection status and a Disconnect button.
- **Quick Settings tile** with the **square** icon and the **MrOpenVPN** label. Tap to connect, tap again to disconnect.
- **Auto-connect** to the last profile on app start, **pause when the screen is off**, and **reconnect on network change** (with protection from network event "storms").
- **Debug mode** (disabled by default): Copy log button, show/hide notification, delete all users, full app data reset.

# Local Development

### Cloning a Repository

```
git clone https://github.com/byMr712/MrOpenVPNClient.git
```

### Opening a Project

Open the project folder in **Android Studio** (requires version 2024.2 or later, JDK 17 included).

### Build

From the project root — from the terminal, or the 'app' configuration in Android Studio:

```
./gradlew :app:assembleDebug
```

On Windows:

```
gradlew.bat :app:assembleDebug
```

On the first build, Android Studio will prompt you to install additional SDK components (Platform 34).

## Installation

The APK will appear in `app/build/outputs/apk/debug/app-debug.apk`. Install on device:

```
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Virus check

You can check a released APK for viruses on **VirusTotal**:

[Check MrOpenVPN Client v1.1 on VirusTotal](https://www.virustotal.com/gui/file/105c8d571f4b2b3084145ce6353ca6f2c530790f13f2e8cc8d926b3980b3eb07?nocache=1)

# Usage

1. Tap **Import .ovpn profile** and select your `.ovpn` file.
2. If the profile requires a login, enter your credentials (or pick a saved user).
3. Tap **Connect** next to the imported profile and confirm the permission to connect to the VPN.
4. The connection status is visible in the notification and on the main screen; you can stop it by pressing **Disconnect**.

## Quick Settings

The **MrOpenVPN** tile (icon: **square**) is available in the Quick Settings panel. It connects the last used profile (or the only imported profile); tapping it again disconnects it. While the VPN is active, the tile is highlighted. If there are no profiles yet, the tile opens the app so you can import one.

# Notes

- Only tun mode configurations are supported; anything else is rejected by the parser.
- Login/password work with the built-in `auth-user-pass` config; interactive requests are handled by `LaunchVPN` internally by the library.
- The app's main and tile icons are the letters **square**.
- Debug mode is intended for troubleshooting — deleting all users or resetting the app data is irreversible (both actions require confirmation).

# License

GPL v3. The application source code is `com.mropenvpn.client`; the `vpnlib` module is based on [openvpn-lib-for-android](https://github.com/cucongcan/openvpn-lib-for-android) (GPL v3), which in turn is based on [ics-openvpn](https://github.com/schwabe/ics-openvpn) (GPL v2, © 2012–2022 Arne Schwabe, with additional terms; see `vpnlib/doc/LICENSE.txt`). The built-in OpenVPN executable is GPL v2 (OpenVPN 3.x is AGPL). See `LICENSE` and `vpnlib/LICENSE` for details.
