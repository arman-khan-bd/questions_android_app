package com.questions.govt.ui.notification;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.questions.govt.ui.exam.ExamArenaActivity;
import com.questions.govt.ui.profile.UserProfileActivity;
import com.questions.govt.ui.question.QuestionDetailActivity;
import com.questions.govt.ui.search.SearchResultActivity;
import com.questions.govt.ui.subject.SubjectDetailActivity;

import java.util.List;

/**
 * Notifications Center Activity with anti-duplication, unread indicators,
 * real-time Room observation, skeleton loading, and instant mark-all-read synchronization.
 */
public class NotificationActivity extends AppCompatActivity {

    private MaterialToolbar toolbarNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutSkeleton;
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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutSkeleton = findViewById(R.id.layoutSkeleton);
        progressBar = findViewById(R.id.progressBar);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefreshLayout.setOnRefreshListener(this::syncData);
    }

    private void setupToolbar() {
        toolbarNotifications.setNavigationOnClickListener(v -> finish());
        toolbarNotifications.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_mark_all_read) {
                notificationRepository.markAllAsRead();
                Toast.makeText(this, R.string.mark_all_read, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this::onNotificationClicked);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void observeNotifications() {
        notificationRepository.getAllNotifications().observe(this, (List<NotificationEntity> notifications) -> {
            if (layoutSkeleton.getVisibility() == View.VISIBLE) {
                // Skeleton still active during sync
                return;
            }
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
        if (adapter == null || adapter.getItemCount() == 0) {
            layoutSkeleton.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }

        notificationRepository.syncNotifications(new NotificationRepository.NotificationSyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    layoutSkeleton.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    List<NotificationEntity> current = notificationRepository.getAllNotifications().getValue();
                    if (current == null || current.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.submitList(current);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    layoutSkeleton.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    List<NotificationEntity> current = notificationRepository.getAllNotifications().getValue();
                    if (current == null || current.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.submitList(current);
                    }
                });
            }
        });
    }

    private void onNotificationClicked(NotificationEntity notification) {
        if (notification == null) return;

        // Mark as read locally and remotely
        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getId());
        }

        String targetType = notification.getTargetType();
        String targetId = notification.getTargetId();
        String targetUrl = notification.getTargetUrl();
        Long questionId = notification.getQuestionId();

        // 1. External URL target
        if ("url".equalsIgnoreCase(targetType) || !TextUtils.isEmpty(targetUrl)) {
            try {
                String url = !TextUtils.isEmpty(targetUrl) ? targetUrl : targetId;
                if (!TextUtils.isEmpty(url)) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 2. Exam target
        if ("exam".equalsIgnoreCase(targetType) || "exam_reminder".equalsIgnoreCase(targetType)) {
            try {
                long examId = 0;
                if (!TextUtils.isEmpty(targetId)) {
                    examId = Long.parseLong(targetId);
                }
                Intent intent = new Intent(this, ExamArenaActivity.class);
                if (examId > 0) {
                    intent.putExtra(ExamArenaActivity.EXTRA_EXAM_ID, examId);
                }
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // 3. Subject target
        if ("subject".equalsIgnoreCase(targetType) && !TextUtils.isEmpty(targetId)) {
            try {
                long subjectId = Long.parseLong(targetId);
                Intent intent = new Intent(this, SubjectDetailActivity.class);
                intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_ID, subjectId);
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // 4. Category target
        if ("category".equalsIgnoreCase(targetType) && !TextUtils.isEmpty(targetId)) {
            try {
                long categoryId = Long.parseLong(targetId);
                Intent intent = new Intent(this, SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_CATEGORY_ID, categoryId);
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // 5. Question target (e.g. comment, reply, star, upvote, question review)
        long qId = questionId != null && questionId > 0 ? questionId : 0;
        if (qId == 0 && "question".equalsIgnoreCase(targetType) && !TextUtils.isEmpty(targetId)) {
            try {
                qId = Long.parseLong(targetId);
            } catch (Exception ignored) {}
        }
        if (qId > 0) {
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, qId);
            startActivity(intent);
            return;
        }

        // 6. User profile target
        if (("profile".equalsIgnoreCase(targetType) || "user".equalsIgnoreCase(targetType)) && !TextUtils.isEmpty(notification.getActorUsername())) {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_USERNAME, notification.getActorUsername());
            startActivity(intent);
            return;
        }

        // Fallback info toast
        Toast.makeText(this, notification.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
