package com.questions.govt.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.questions.govt.data.local.AppDatabase;
import com.questions.govt.data.local.dao.NotificationDao;
import com.questions.govt.data.local.entity.NotificationEntity;
import com.questions.govt.data.model.NotificationItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.ApiService;
import com.questions.govt.data.remote.response.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for notifications with strict composite anti-duplication enforcement
 * on both client SQLite Room and backend API.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepo";
    private static volatile NotificationRepository instance;

    private final NotificationDao notificationDao;
    private final ApiService apiService;

    private NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.notificationDao = db.notificationDao();
        this.apiService = ApiClient.getApiService();
    }

    public static NotificationRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NotificationRepository.class) {
                if (instance == null) {
                    instance = new NotificationRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<NotificationEntity>> getAllNotifications() {
        return notificationDao.getAllNotifications();
    }

    public LiveData<Integer> getUnreadCount() {
        return notificationDao.getUnreadCount();
    }

    /**
     * Inserts a push notification into Room.
     * Strict anti-duplication: Ignored if (recipientId, actorId, questionId, type) already exists.
     */
    public void insertPushNotification(NotificationEntity entity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long insertedRowId = notificationDao.insert(entity);
            if (insertedRowId == -1) {
                Log.d(TAG, "Duplicate push notification ignored by Room composite UNIQUE constraint.");
            } else {
                Log.d(TAG, "New notification inserted with rowId: " + insertedRowId);
            }
        });
    }

    /**
     * Synchronizes latest notifications from server.
     */
    public void syncNotifications(NotificationSyncCallback callback) {
        apiService.getNotifications().enqueue(new Callback<ApiResponse<List<NotificationItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<NotificationItem>>> call, Response<ApiResponse<List<NotificationItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<NotificationItem> remoteList = response.body().getData();
                    List<NotificationEntity> entities = new ArrayList<>();

                    for (NotificationItem item : remoteList) {
                        NotificationEntity entity = new NotificationEntity();
                        entity.setId(item.getId());
                        entity.setRecipientId(item.getRecipientId());
                        entity.setActorId(item.getActorId());
                        entity.setActorName(item.getActorName());
                        entity.setActorUsername(item.getActorUsername());
                        entity.setActorAvatarUrl(item.getActorAvatarUrl());
                        entity.setQuestionId(item.getQuestionId());
                        entity.setType(item.getType());
                        entity.setTargetType(item.getTargetType());
                        entity.setTargetId(item.getTargetId());
                        entity.setTargetUrl(item.getTargetUrl());
                        entity.setTitle(item.getTitle());
                        entity.setMessage(item.getMessage());
                        entity.setRead(item.isRead());
                        entity.setCreatedAt(item.getCreatedAt());
                        entities.add(entity);
                    }

                    AppDatabase.databaseWriteExecutor.execute(() ->
                            notificationDao.insertOrReplaceAll(entities));

                    if (callback != null) callback.onSuccess();
                } else {
                    if (callback != null) callback.onError("Failed to fetch notifications.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<NotificationItem>>> call, Throwable t) {
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    public void markAsRead(long notificationId) {
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.markAsRead(notificationId));

        apiService.markNotificationRead(notificationId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d(TAG, "Notification marked read on backend: " + notificationId);
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.w(TAG, "Failed to mark read on server, updated in local Room.", t);
            }
        });
    }

    public void markAllAsRead() {
        AppDatabase.databaseWriteExecutor.execute(notificationDao::markAllAsRead);

        apiService.markAllNotificationsRead().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d(TAG, "All notifications marked read on server.");
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.w(TAG, "Failed to mark all read on server, updated in local Room.", t);
            }
        });
    }

    public interface NotificationSyncCallback {
        void onSuccess();
        void onError(String message);
    }
}
