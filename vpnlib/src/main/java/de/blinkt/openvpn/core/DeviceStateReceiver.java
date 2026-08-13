/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;

import java.util.LinkedList;

import static de.blinkt.openvpn.core.OpenVPNManagement.pauseReason;

public class DeviceStateReceiver extends BroadcastReceiver implements ByteCountListener, OpenVPNManagement.PausedStateCallback {
    private final Handler mDisconnectHandler;
    private OpenVPNManagement mManagement;

    // Window time in s
    private final int TRAFFIC_WINDOW = 60;
    // Data traffic limit in bytes
    private final long TRAFFIC_LIMIT = 64 * 1024;

    // Time to wait after network disconnect to pause the VPN
    private final int DISCONNECT_WAIT = 20;

    // Debounce for bursts of NetworkCallback events during network transitions
    // (e.g. Wi-Fi <-> cellular handover). Prevents a storm of reconnect signals.
    private static final long NETWORK_ACTION_COOLDOWN_MS = 1500;
    private long mLastNetworkActionTime = 0;
    private long mLastNetworkActionId = 0;
    private boolean mNetworkRecheckScheduled = false;

    connectState network = connectState.DISCONNECTED;
    connectState screen = connectState.SHOULDBECONNECTED;
    connectState userpause = connectState.SHOULDBECONNECTED;

    private String lastStateMsg = null;
    private final java.lang.Runnable mDelayDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!(network == connectState.PENDINGDISCONNECT))
                return;

            network = connectState.DISCONNECTED;

            // Set screen state to be disconnected if disconnect pending
            if (screen == connectState.PENDINGDISCONNECT)
                screen = connectState.DISCONNECTED;

            mManagement.pause(getPauseReason());
        }
    };
    private long lastConnectedNetwork = 0;

    @Override
    public boolean shouldBeRunning() {
        return shouldBeConnected();
    }

    private enum connectState {
        SHOULDBECONNECTED,
        PENDINGDISCONNECT,
        DISCONNECTED
    }

    private static class Datapoint {
        private Datapoint(long t, long d) {
            timestamp = t;
            data = d;
        }

        long timestamp;
        long data;
    }

    private final LinkedList<Datapoint> trafficdata = new LinkedList<>();

    private ConnectivityManager mConnMan;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mNetworkCallbackRegistered = false;
    private Network mLastPhysicalNetwork;
    private Context mContext;

    /**
     * Registers a NetworkCallback that reliably detects changes of the
     * underlying (non-VPN) network. The legacy CONNECTIVITY_ACTION broadcast
     * is no longer delivered reliably on modern Android versions, so without
     * this the "Reconnect on network change"
     * setting would never react to a network change.
     */
    public void startNetworkMonitoring(Context context) {
        mContext = context;
        mConnMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnMan == null || mNetworkCallbackRegistered)
            return;

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    trackPhysicalNetwork(network);
                    networkStateChange(mContext);
                }

                @Override
                public void onLost(Network network) {
                    if (network.equals(mLastPhysicalNetwork))
                        mLastPhysicalNetwork = null;
                    networkStateChange(mContext);
                }

                @Override
                public void onUnavailable() {
                    mLastPhysicalNetwork = null;
                    networkStateChange(mContext);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                    trackPhysicalNetwork(network);
                    networkStateChange(mContext);
                }
            };
            mConnMan.registerNetworkCallback(request, mNetworkCallback);
            mNetworkCallbackRegistered = true;
        } catch (Exception e) {
            VpnStatus.logException(e);
        }
    }

    public void stopNetworkMonitoring() {
        if (mNetworkCallbackRegistered && mConnMan != null) {
            try {
                mConnMan.unregisterNetworkCallback(mNetworkCallback);
            } catch (Exception ignored) {
            }
        }
        mNetworkCallbackRegistered = false;
        mNetworkCallback = null;
    }

    private void trackPhysicalNetwork(Network network) {
        if (mConnMan == null)
            return;
        NetworkCapabilities caps = mConnMan.getNetworkCapabilities(network);
        if (caps != null
                && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                && isUsableNetwork(network, caps))
            mLastPhysicalNetwork = network;
    }


    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (screen != connectState.PENDINGDISCONNECT)
            return;

        long total = diffIn + diffOut;
        trafficdata.add(new Datapoint(System.currentTimeMillis(), total));

        while (trafficdata.getFirst().timestamp <= (System.currentTimeMillis() - TRAFFIC_WINDOW * 1000)) {
            trafficdata.removeFirst();
        }

        long windowtraffic = 0;
        for (Datapoint dp : trafficdata)
            windowtraffic += dp.data;

        if (windowtraffic < TRAFFIC_LIMIT) {
            screen = connectState.DISCONNECTED;
            VpnStatus.logInfo(R.string.screenoff_pause,
                    "64 kB", TRAFFIC_WINDOW);

            mManagement.pause(getPauseReason());
        }
    }


    public void userPause(boolean pause) {
        if (pause) {
            userpause = connectState.DISCONNECTED;
            // Check if we should disconnect
            mManagement.pause(getPauseReason());
        } else {
            boolean wereConnected = shouldBeConnected();
            userpause = connectState.SHOULDBECONNECTED;
            if (shouldBeConnected() && !wereConnected)
                mManagement.resume();
            else
                // Update the reason why we currently paused
                mManagement.pause(getPauseReason());
        }
    }

    public DeviceStateReceiver(OpenVPNManagement management) {
        super();
        mManagement = management;
        mManagement.setPauseCallback(this);
        mDisconnectHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(context);


        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            networkStateChange(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            boolean screenOffPause = prefs.getBoolean("screenoff", false);

            if (screenOffPause) {
                if (ProfileManager.getLastConnectedVpn() != null && !ProfileManager.getLastConnectedVpn().mPersistTun)
                    VpnStatus.logError(R.string.screen_nopersistenttun);

                screen = connectState.DISCONNECTED;
                VpnStatus.logInfo(R.string.screenoff_pause);
                mManagement.pause(getPauseReason());
            }
        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            // Network was disabled because screen off
            boolean connected = shouldBeConnected();
            screen = connectState.SHOULDBECONNECTED;

            /* We should connect now, cancel any outstanding disconnect timer */
            mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
            /* should be connected has changed because the screen is on now, connect the VPN */
            if (shouldBeConnected() != connected)
                mManagement.resume();
            else if (!shouldBeConnected())
                /*Update the reason why we are still paused */
                mManagement.pause(getPauseReason());

        }
    }

    private void fillTrafficData() {
        trafficdata.add(new Datapoint(System.currentTimeMillis(), TRAFFIC_LIMIT));
    }

    public void networkStateChange(Context context) {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(context);
        boolean reconnectOnChange = prefs.getBoolean("netchangereconnect", true);

        if (mConnMan == null)
            mConnMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network physicalNetwork = getActivePhysicalNetwork();
        NetworkCapabilities caps = physicalNetwork == null ? null : mConnMan.getNetworkCapabilities(physicalNetwork);
        boolean usableNetwork = isUsableNetwork(physicalNetwork, caps);

        String netstatestring;
        if (usableNetwork) {
            String transport = transportsToString(caps);
            netstatestring = String.format("%2$s %3$s to %1$s", transport, "CONNECTED", physicalNetwork);
        } else {
            netstatestring = "not connected";
        }

        if (usableNetwork) {
            long newnet = physicalNetwork.getNetworkHandle();
            boolean pendingDisconnect = (network == connectState.PENDINGDISCONNECT);
            network = connectState.SHOULDBECONNECTED;

            boolean sameNetwork = (lastConnectedNetwork != 0 && lastConnectedNetwork == newnet);
            boolean actionTaken = false;

            if (reconnectOnChange) {
                if (pendingDisconnect && sameNetwork) {
                    /* Same network, connection still 'established' */
                    mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                    // Reprotect the sockets just be sure
                    mManagement.networkChange(true);
                    actionTaken = true;
                } else {
                    /* Different network or connection not established anymore */

                    if (screen == connectState.PENDINGDISCONNECT)
                        screen = connectState.DISCONNECTED;

                    if (shouldBeConnected()) {
                        // During a burst of NetworkCallback events (Wi-Fi <-> cellular
                        // handover) skip redundant commands and let openvpn recover
                        // once the network settles.
                        if (!pendingDisconnect && isNetworkActionDebounced()) {
                            VpnStatus.logDebug("Network change debounced (network " + newnet + ")");
                            scheduleNetworkRecheck();
                        } else {
                            mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);

                            if (pendingDisconnect || !sameNetwork)
                                mManagement.networkChange(sameNetwork);
                            else
                                mManagement.resume();
                            actionTaken = true;
                        }
                    }
                }
            } else if (pendingDisconnect) {
                /* Reconnect disabled: keep the tunnel running, only re-connect
                 * if it was paused while waiting for a network. */
                mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                mManagement.resume();
                actionTaken = true;
            }

            if (actionTaken) {
                mLastNetworkActionTime = SystemClock.elapsedRealtime();
                mLastNetworkActionId = newnet;
            }
            lastConnectedNetwork = newnet;
        } else {
            // Not connected, stop openvpn, set last connected network to no network
            lastConnectedNetwork = 0;
            if (reconnectOnChange) {
                network = connectState.PENDINGDISCONNECT;
                mDisconnectHandler.postDelayed(mDelayDisconnectRunnable, DISCONNECT_WAIT * 1000);
            }
        }


        if (!netstatestring.equals(lastStateMsg))
            VpnStatus.logInfo(R.string.netstatus, netstatestring);
        VpnStatus.logDebug(String.format("Debug state info: %s, pause: %s, shouldbeconnected: %s, network: %s ",
                netstatestring, getPauseReason(), shouldBeConnected(), network));
        lastStateMsg = netstatestring;

    }

    private boolean isNetworkActionDebounced() {
        long now = SystemClock.elapsedRealtime();
        if ((now - mLastNetworkActionTime) >= NETWORK_ACTION_COOLDOWN_MS)
            return false;
        // Still inside the cooldown window: suppress further commands even if
        // the network flips back and forth (Wi-Fi <-> cellular handover).
        return true;
    }

    private void scheduleNetworkRecheck() {
        if (mNetworkRecheckScheduled)
            return;
        mNetworkRecheckScheduled = true;
        mDisconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNetworkRecheckScheduled = false;
                if (mManagement != null)
                    networkStateChange(mContext);
            }
        }, NETWORK_ACTION_COOLDOWN_MS);
    }

    /**
     * Returns the current physical (non-VPN) network when it is usable. While
     * the VPN is running, getActiveNetwork() returns our own VPN tunnel, which
     * would otherwise make every network change look like the VPN network
     * itself and would be ignored. On such a network we fall back to the last
     * physical network tracked by the NetworkCallback instead.
     */
    private Network getActivePhysicalNetwork() {
        if (mConnMan == null)
            return null;

        Network active = mConnMan.getActiveNetwork();
        if (active == null)
            return mLastPhysicalNetwork;

        NetworkCapabilities caps = mConnMan.getNetworkCapabilities(active);
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            return mLastPhysicalNetwork;
        return active;
    }


    public boolean isUserPaused() {
        return userpause == connectState.DISCONNECTED;
    }

    public boolean isPaused() {
        return userpause == connectState.DISCONNECTED
                || screen == connectState.DISCONNECTED
                || network == connectState.DISCONNECTED;
    }

    private boolean shouldBeConnected() {
        return (screen == connectState.SHOULDBECONNECTED && userpause == connectState.SHOULDBECONNECTED &&
                network == connectState.SHOULDBECONNECTED);
    }

    public pauseReason getPauseReason() {
        if (userpause == connectState.DISCONNECTED)
            return pauseReason.userPause;

        if (screen == connectState.DISCONNECTED)
            return pauseReason.screenOff;

        if (network == connectState.DISCONNECTED)
            return pauseReason.noNetwork;

        return pauseReason.userPause;
    }

    private static boolean isUsableNetwork(Network network, NetworkCapabilities caps) {
        if (network == null || caps == null)
            return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
    }

    private static String transportsToString(NetworkCapabilities caps) {
        StringBuilder sb = new StringBuilder();
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) sb.append("WIFI ");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) sb.append("CELLULAR ");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) sb.append("ETHERNET ");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) sb.append("VPN ");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) sb.append("BLUETOOTH ");
        if (sb.length() == 0) sb.append("UNKNOWN ");
        return sb.toString().trim();
    }
}
