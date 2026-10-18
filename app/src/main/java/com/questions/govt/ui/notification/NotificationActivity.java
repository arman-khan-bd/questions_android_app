package com.questions.govt.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.questions.govt.R;
import com.questions.govt.data.local.entity.NotificationEntity;
import com.questions.govt.data.repository.NotificationRepository;
import com.questions.govt.ui.search.SearchResultActivity;

import java.util.List;

/**
 * Notifications Center Activity with anti-duplication, unread indicators,
 * real-time Room observation, and instant mark-all-read synchronization.
 */
public class NotificationActivity extends AppCompatActivity {

    private MaterialToolbar toolbarNotifications;
    private ImageButton btnMarkAllRead;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationRepository = NotificationRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        observeNotifications();
        syncData();
    }

    private void initViews() {
        toolbarNotifications = findViewById(R.id.toolbarNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefreshLayout.setOnRefreshListener(this::syncData);

        btnMarkAllRead.setOnClickListener(v -> {
            notificationRepository.markAllAsRead();
            Toast.makeText(this, R.string.mark_all_read, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupToolbar() {
        toolbarNotifications.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this::onNotificationClicked);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void observeNotifications() {
        notificationRepository.getAllNotifications().observe(this, (List<NotificationEntity> notifications) -> {
            if (notifications == null || notifications.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvNotifications.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvNotifications.setVisibility(View.VISIBLE);
                adapter.submitList(notifications);
            }
        });
    }

    private void syncData() {
        notificationRepository.syncNotifications(new NotificationRepository.NotificationSyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void onNotificationClicked(NotificationEntity notification) {
        // Mark as read locally and remotely
        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getId());
        }

        // Navigate if question-related
        if (notification.getQuestionId() != null && notification.getQuestionId() > 0) {
            Intent intent = new Intent(this, com.questions.govt.ui.question.QuestionDetailActivity.class);
            intent.putExtra(com.questions.govt.ui.question.QuestionDetailActivity.EXTRA_QUESTION_ID, notification.getQuestionId());
            startActivity(intent);
        } else {
            Toast.makeText(this, notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }
}
