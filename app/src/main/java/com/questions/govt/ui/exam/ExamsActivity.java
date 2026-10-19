package com.questions.govt.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.questions.govt.R;
import com.questions.govt.data.model.OfficialExamItem;
import com.questions.govt.data.repository.ExamRepository;

import java.util.List;
import java.util.Map;

/**
 * Exams page ("Exams") displaying all official competitive exams created by Admin.
 * Shows total exams count, students attended, and correct answers given.
 * Attending exams from this page affects user ranking on the leaderboard.
 */
public class ExamsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvKpiTotalExams;
    private TextView tvKpiUsersAttended;
    private TextView tvKpiTotalCorrect;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvExams;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private OfficialExamAdapter adapter;
    private ExamRepository examRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exams);

        examRepository = ExamRepository.getInstance(this);

        initViews();
        setupRecyclerView();
        loadExams(false);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvKpiTotalExams = findViewById(R.id.tvKpiTotalExams);
        tvKpiUsersAttended = findViewById(R.id.tvKpiUsersAttended);
        tvKpiTotalCorrect = findViewById(R.id.tvKpiTotalCorrect);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvExams = findViewById(R.id.rvExams);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Exams");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(() -> loadExams(true));
    }

    private void setupRecyclerView() {
        adapter = new OfficialExamAdapter(exam -> {
            if (exam != null && exam.getId() > 0) {
                Intent intent = new Intent(ExamsActivity.this, ExamArenaActivity.class);
                intent.putExtra(ExamArenaActivity.EXTRA_EXAM_ID, exam.getId());
                startActivity(intent);
            }
        });

        rvExams.setLayoutManager(new LinearLayoutManager(this));
        rvExams.setAdapter(adapter);
    }

    private void loadExams(boolean isPullToRefresh) {
        if (!isPullToRefresh) {
            progressBar.setVisibility(View.VISIBLE);
        }

        examRepository.getOfficialExams(new ExamRepository.OfficialExamsCallback() {
            @Override
            public void onSuccess(List<OfficialExamItem> exams, Map<String, Object> meta) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (exams != null && !exams.isEmpty()) {
                    layoutEmpty.setVisibility(View.GONE);
                    rvExams.setVisibility(View.VISIBLE);
                    adapter.setExams(exams);

                    // Compute telemetry from meta if present, or fallback to sum
                    int totalExams = exams.size();
                    int totalAttended = 0;
                    int totalCorrect = 0;

                    if (meta != null) {
                        try {
                            if (meta.containsKey("total_exams")) {
                                totalExams = ((Number) meta.get("total_exams")).intValue();
                            }
                            if (meta.containsKey("total_attended")) {
                                totalAttended = ((Number) meta.get("total_attended")).intValue();
                            }
                            if (meta.containsKey("total_correct")) {
                                totalCorrect = ((Number) meta.get("total_correct")).intValue();
                            }
                        } catch (Exception ignored) {}
                    } else {
                        for (OfficialExamItem item : exams) {
                            totalAttended += item.getTotalAttended();
                            totalCorrect += item.getTotalCorrect();
                        }
                    }

                    tvKpiTotalExams.setText(String.valueOf(totalExams));
                    tvKpiUsersAttended.setText(String.valueOf(totalAttended));
                    tvKpiTotalCorrect.setText(String.valueOf(totalCorrect));
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvExams.setVisibility(View.GONE);
                    tvKpiTotalExams.setText("0");
                    tvKpiUsersAttended.setText("0");
                    tvKpiTotalCorrect.setText("0");
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ExamsActivity.this, message != null ? message : "Unable to load exams", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
