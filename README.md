# MrOpenVPN Client

A minimal Android client that connects to OpenVPN by importing a ready-made `.ovpn` file.

- Imports any `.ovpn`/`.conf` profile from the system file picker (tun mode only, same
  restrictions as OpenVPN for Android).
- Parses the config and stores it locally with `ConfigParser`/`ProfileManager`.
- Starts the VPN through the vendored `vpnlib` module (a fork of
  [ics-openvpn](https://github.com/schwabe/ics-openvpn) v0.7.64 that ships prebuilt
  native binaries).
- Shows connection state and lets you disconnect with one tap.

## Modules

- `app` — the Android app (Kotlin, `com.mropenovpn.client`).
- `vpnlib` — vendored OpenVPN library (`de.blinkt.openvpn`), GPL v3. Its `build.gradle`
  was rewritten so the project builds without JitPack or a manual AAR.

## Requirements

- Android Studio 2024.2 (Ladybug) or newer
- Android SDK Platform 34 (`compileSdk`/`targetSdk` 34, `minSdk` 24)
- JDK 17 (bundled with Android Studio)

## Build

Open the folder in Android Studio and run the `app` configuration, or from a terminal:

```
./gradlew assembleDebug
```

Install on a device/emulator with `adb install app/build/outputs/apk/debug/app-debug.apk`.

On first build Android Studio will offer to install the missing SDK components (Platform 34).

## Usage

1. Tap **Import .ovpn profile** and pick your `.ovpn` file.
2. Tap **Connect** next to the imported profile and approve the VPN permission dialog.
3. The notification shows the connection state; use **Disconnect** to stop.

## Quick Settings tile

A **MrOpenVPN** tile is available in the Quick Settings panel. Tap it to connect to the
last-used profile (or the only imported one), tap again to disconnect. If no profile
exists yet, the app opens so you can import one. The tile is `STATE_ACTIVE` while the
VPN is connected.

## Notes

- Only `tun` mode configs are supported (a `tap` config will be rejected by the parser).
- Username/password auth works with inline `auth-user-pass` credentials; interactive
  prompts are handled by `LaunchVPN` inside the library.
- The launcher icon is a simple placeholder.

## License

GPL v3. The app source is `com.mropenovpn.client`; the `vpnlib` module is derived from
[openvpn-lib-for-android](https://github.com/cucongcan/openvpn-lib-for-android) (GPL v3),
which is in turn derived from [ics-openvpn](https://github.com/schwabe/ics-openvpn)
(GPL v2, © 2012-2022 Arne Schwabe, with additional terms; see `vpnlib/doc/LICENSE.txt`).
The bundled OpenVPN executable is GPL v2 (OpenVPN 3.x is AGPL). See `LICENSE` and
`vpnlib/LICENSE`.
