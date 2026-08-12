# Keep VpnProfile and related core classes used by the library (library ships its own rules;
# these cover app-specific usage).
-keep class de.blinkt.openvpn.** { *; }
-dontwarn de.blinkt.openvpn.**
