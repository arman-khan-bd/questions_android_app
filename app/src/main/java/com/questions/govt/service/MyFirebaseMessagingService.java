package com.questions.govt.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.questions.govt.GovtExamApp;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.local.entity.NotificationEntity;
import com.questions.govt.data.repository.NotificationRepository;
import com.questions.govt.ui.main.MainActivity;
import com.questions.govt.ui.notification.NotificationActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles Firebase Cloud Messaging (FCM HTTP v1) push tokens and payload alerts.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM device token: " + token);
        SessionManager.getInstance().saveFcmToken(token);

        // Sync with backend if user is logged in
        if (SessionManager.getInstance().isLoggedIn()) {
            syncFcmTokenWithBackend(token);
        }
    }

    private static final Map<String, Long> RECENT_NOTIFICATIONS = new HashMap<>();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = getString(R.string.app_name);
        String body = "";
        Map<String, String> data = remoteMessage.getData();

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (data.size() > 0) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            else if (data.containsKey("message")) body = data.get("message");
        }

        // Anti-Duplication: suppress duplicate push messages received within 15 seconds
        String notifKey = title + "::" + body;
        long now = System.currentTimeMillis();
        synchronized (RECENT_NOTIFICATIONS) {
            Long lastTime = RECENT_NOTIFICATIONS.get(notifKey);
            if (lastTime != null && (now - lastTime) < 15000) {
                Log.d(TAG, "Duplicate FCM message suppressed: " + notifKey);
                return;
            }
            RECENT_NOTIFICATIONS.put(notifKey, now);
            if (RECENT_NOTIFICATIONS.size() > 50) {
                RECENT_NOTIFICATIONS.entrySet().removeIf(entry -> (now - entry.getValue()) > 60000);
            }
        }

        // Save to Room DB with strict composite anti-duplication
        if (data.size() > 0) {
            try {
                NotificationEntity entity = new NotificationEntity();
                if (data.containsKey("id")) {
                    entity.setId(Long.parseLong(data.get("id")));
                }
                if (data.containsKey("recipient_id")) {
                    entity.setRecipientId(Long.parseLong(data.get("recipient_id")));
                }
                if (data.containsKey("actor_id")) {
                    entity.setActorId(Long.parseLong(data.get("actor_id")));
                }
                if (data.containsKey("question_id")) {
                    entity.setQuestionId(Long.parseLong(data.get("question_id")));
                }
                entity.setActorName(data.get("actor_name"));
                entity.setActorUsername(data.get("actor_username"));
                entity.setActorAvatarUrl(data.get("actor_avatar_url"));
                entity.setType(data.get("type"));
                entity.setTitle(title);
                entity.setMessage(body);
                entity.setRead(false);
                entity.setCreatedAt(data.get("created_at") != null ? data.get("created_at") : "Just now");

                NotificationRepository.getInstance(this).insertPushNotification(entity);
            } catch (Exception e) {
                Log.w(TAG, "Error parsing notification entity payload", e);
            }
        }

        showNotification(title, body, notifKey.hashCode());
    }

    private void showNotification(String title, String messageBody, int notificationId) {
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, GovtExamApp.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_app_logo)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private void syncFcmTokenWithBackend(String token) {
        Map<String, String> body = new HashMap<>();
        body.put("fcm_token", token);
        ApiClient.getApiService().syncFcmToken(body).enqueue(new Callback<com.questions.govt.data.remote.response.ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<com.questions.govt.data.remote.response.ApiResponse<Void>> call, Response<com.questions.govt.data.remote.response.ApiResponse<Void>> response) {
                Log.d(TAG, "FCM token synced with backend successfully.");
            }

            @Override
            public void onFailure(Call<com.questions.govt.data.remote.response.ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Failed to sync FCM token with backend: " + t.getMessage());
            }
        });
    }
}
