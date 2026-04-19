package com.dtcteam.pypos.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;

public class NetworkMonitor {
    private static NetworkMonitor instance;
    private boolean isOnline = true;
    private Context context;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    public interface NetworkListener {
        void onNetworkChanged(boolean isOnline);
    }

    private NetworkListener listener;

    private NetworkMonitor(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void startMonitoring(NetworkListener listener) {
        this.listener = listener;
        
        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isOnline = true;
                if (listener != null) {
                    listener.onNetworkChanged(true);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                isOnline = false;
                if (listener != null) {
                    listener.onNetworkChanged(false);
                }
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                if (isOnline != hasInternet) {
                    isOnline = hasInternet;
                    if (listener != null) {
                        listener.onNetworkChanged(hasInternet);
                    }
                }
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
        
        // Initial check
        isOnline = checkCurrentNetwork();
    }

    private boolean checkCurrentNetwork() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) return false;
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public void stopMonitoring() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                // Ignore if not registered
            }
        }
    }
}