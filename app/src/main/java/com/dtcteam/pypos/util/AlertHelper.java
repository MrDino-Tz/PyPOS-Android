package com.dtcteam.pypos.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.dtcteam.pypos.MainActivity;
import com.dtcteam.pypos.R;

public class AlertHelper {
    private static final String CHANNEL_ID = "pypos_alerts";
    private static final String LOW_STOCK_CHANNEL = "low_stock";
    private static final String OFFLINE_CHANNEL = "offline_mode";
    private Context context;
    private NotificationManager notificationManager;
    private boolean wasOffline = false;

    public AlertHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Low stock notifications
            NotificationChannel lowStockChannel = new NotificationChannel(
                LOW_STOCK_CHANNEL,
                "Low Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            lowStockChannel.setDescription("Notifications for low stock items");
            notificationManager.createNotificationChannel(lowStockChannel);

            // Offline mode notifications
            NotificationChannel offlineChannel = new NotificationChannel(
                OFFLINE_CHANNEL,
                "Connection Status",
                NotificationManager.IMPORTANCE_LOW
            );
            offlineChannel.setDescription("Network connection status");
            notificationManager.createNotificationChannel(offlineChannel);
        }
    }

    public void showOfflineNotification(boolean isOffline) {
        String title, message;

        if (isOffline) {
            title = "Offline Mode";
            message = "No internet connection. Changes will sync when online.";
            wasOffline = true;
        } else {
            title = "Back Online";
            message = "Internet connected. Syncing data...";
            wasOffline = false;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, OFFLINE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    public void showLowStockNotification(int count) {
        if (count <= 0) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("open", "low_stock");
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Low Stock Alert";
        String message = count + " item" + (count > 1 ? "s" : "") + " running low on stock";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOW_STOCK_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(1002, builder.build());
    }

    public void clearNotifications() {
        notificationManager.cancelAll();
    }
}