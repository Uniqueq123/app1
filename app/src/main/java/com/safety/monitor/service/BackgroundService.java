package com.safety.monitor.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.safety.monitor.R;
import com.safety.monitor.config.AppConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "SafetyMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private ScheduledExecutorService scheduler;
    private Handler handler = new Handler();
    private static String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        // Send initial status
        sendDeviceStatus(true);

        // Schedule periodic status updates
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendDeviceStatus(true);
                handler.postDelayed(this, 5 * 60 * 1000); // Every 5 minutes
            }
        }, 5 * 60 * 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        super.onDestroy();
        // Send inactive status when service stops
        sendDeviceStatus(false);
        handler.removeCallbacksAndMessages(null);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safety Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for monitoring notifications");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety Monitor Active")
                .setContentText("Monitoring notifications for safety concerns")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        return builder.build();
    }

    private void sendDeviceStatus(boolean isActive) {
        new Thread(() -> {
            try {
                URL url = new URL(AppConfig.SERVER_URL + "/api/device-status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject data = new JSONObject();
                data.put("deviceId", deviceId);
                data.put("status", isActive ? "active" : "inactive");
                data.put("model", Build.MODEL);
                data.put("manufacturer", Build.MANUFACTURER);
                data.put("installedAt", System.currentTimeMillis());

                try(OutputStream os = conn.getOutputStream()) {
                    os.write(data.toString().getBytes());
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Device status sent. Response: " + responseCode);
            } catch (Exception e) {
                Log.e(TAG, "Error sending device status: " + e.getMessage());
            }
        }).start();
    }
} 