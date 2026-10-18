package com.questions.govt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.security.ProviderInstaller;
import com.questions.govt.data.local.SessionManager;

/**
 * Custom Application class supporting Android 5.0 (API 21) through Android 16 (API 36).
 * Manages MultiDex, TLS 1.2 Provider installation on API 21, and Notification Channels.
 */
public class GovtExamApp extends MultiDexApplication {

    private static final String TAG = "GovtExamApp";
    public static final String NOTIFICATION_CHANNEL_ID = "govt_exam_alerts_channel";
    private static GovtExamApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Session Manager
        SessionManager.init(this);

        // Apply Dark / Light mode preference on startup
        boolean isDark = SessionManager.getInstance().isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Safeguard for Android 5.0 (API 21): Install modern TLS Provider
        installTls12Provider();

        // Create Android 8.0+ Notification Channels
        createNotificationChannels();
    }

    public static GovtExamApp getInstance() {
        return instance;
    }

    /**
     * Installs Google Play Services security provider to guarantee TLS 1.2 support
     * on Android 5.0 Lollipop devices communicating with modern SSL certificates.
     */
    private void installTls12Provider() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
                @Override
                public void onProviderInstalled() {
                    Log.i(TAG, "Security Provider installed successfully for TLS 1.2.");
                }

                @Override
                public void onProviderInstallFailed(int errorCode, android.content.Intent recoveryIntent) {
                    Log.w(TAG, "Security Provider installation failed with code: " + errorCode);
                }
            });
        }
    }

    /**
     * Creates notification channels for push alerts, exams, and community replies.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Govt Exam Prep Alerts";
            String description = "Notifications for exam reminders, community replies, stars, and admin announcements";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
