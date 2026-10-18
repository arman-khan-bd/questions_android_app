package com.questions.govt.ui.statistics;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButton;
import com.questions.govt.R;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.StatisticsResponse;
import com.questions.govt.ui.search.SearchResultActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Performance Analytics Fragment powered by MPAndroidChart:
 * - Pass/Fail Pie Chart
 * - Stacked Difficulty Bar Chart (Easy/Medium/Hard Correct vs Wrong)
 * - Cubic Spline Line Chart (15-exam score trend)
 * - Horizontal Bar Chart (Subject Mastery %) with passing threshold
 * - One-tap CTA to drill down & practice weakest topics
 */
public class StatisticsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvStatTotalExams, tvStatPassRate, tvStatAvgAccuracy, tvStatPassFailCount;
    private LineChart lineChartScoreProgression;
    private BarChart barChartDifficulty;
    private PieChart pieChartPassFail;
    private HorizontalBarChart horizontalBarChartMastery;
    private MaterialButton btnPracticeWeakTopics;
    private ProgressBar progressBar;

    private StatisticsResponse cachedStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);
        initViews(root);
        fetchStatistics();
        return root;
    }

    private void initViews(View root) {
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        tvStatTotalExams = root.findViewById(R.id.tvStatTotalExams);
        tvStatPassRate = root.findViewById(R.id.tvStatPassRate);
        tvStatAvgAccuracy = root.findViewById(R.id.tvStatAvgAccuracy);
        tvStatPassFailCount = root.findViewById(R.id.tvStatPassFailCount);

        lineChartScoreProgression = root.findViewById(R.id.lineChartScoreProgression);
        barChartDifficulty = root.findViewById(R.id.barChartDifficulty);
        pieChartPassFail = root.findViewById(R.id.pieChartPassFail);
        horizontalBarChartMastery = root.findViewById(R.id.horizontalBarChartMastery);
        btnPracticeWeakTopics = root.findViewById(R.id.btnPracticeWeakTopics);
        progressBar = root.findViewById(R.id.progressBar);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefreshLayout.setOnRefreshListener(this::fetchStatistics);

        btnPracticeWeakTopics.setOnClickListener(v -> handlePracticeWeakTopics());
    }

    private void fetchStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getStatistics().enqueue(new Callback<ApiResponse<StatisticsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StatisticsResponse>> call, Response<ApiResponse<StatisticsResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedStats = response.body().getData();
                    bindOverviewKpis(cachedStats);
                    renderPieChart(cachedStats);
                    renderDifficultyBarChart(cachedStats);
                    renderScoreTrendLineChart(cachedStats);
                    renderSubjectMasteryHorizontalBar(cachedStats);
                } else {
                    Toast.makeText(requireContext(), "Failed to load statistics.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StatisticsResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOverviewKpis(StatisticsResponse stats) {
        int total = stats.getTotalExamsAttended();
        int passed = stats.getExamsPassed();
        int failed = stats.getExamsFailed();

        tvStatTotalExams.setText(String.valueOf(total));

        double passRate = total > 0 ? ((double) passed / total) * 100 : 0.0;
        tvStatPassRate.setText(String.format(Locale.US, "%.1f%%", passRate));

        tvStatPassFailCount.setText(String.format(Locale.getDefault(), "%d / %d", passed, failed));

        // Calculate average accuracy across last exams
        float avgAcc = 0;
        if (stats.getScoreProgression() != null && !stats.getScoreProgression().isEmpty()) {
            float sum = 0;
            for (StatisticsResponse.ExamScorePoint p : stats.getScoreProgression()) {
                sum += p.getAccuracy();
            }
            avgAcc = sum / stats.getScoreProgression().size();
        }
        tvStatAvgAccuracy.setText(String.format(Locale.US, "%.1f%%", avgAcc));
    }

    private void renderPieChart(StatisticsResponse stats) {
        Context context = requireContext();
        pieChartPassFail.getDescription().setEnabled(false);
        pieChartPassFail.setUsePercentValues(true);
        pieChartPassFail.setDrawHoleEnabled(true);
        pieChartPassFail.setHoleRadius(52f);
        pieChartPassFail.setTransparentCircleRadius(58f);
        pieChartPassFail.setHoleColor(ContextCompat.getColor(context, R.color.card_background));
        pieChartPassFail.setCenterText("Exams Result");
        pieChartPassFail.setCenterTextSize(12f);
        pieChartPassFail.setCenterTextColor(ContextCompat.getColor(context, R.color.text_primary));

        List<PieEntry> entries = new ArrayList<>();
        if (stats.getExamsPassed() > 0) {
            entries.add(new PieEntry(stats.getExamsPassed(), "Passed"));
        }
        if (stats.getExamsFailed() > 0) {
            entries.add(new PieEntry(stats.getExamsFailed(), "Failed"));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Exams Yet"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        List<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(context, R.color.success));
        colors.add(ContextCompat.getColor(context, R.color.error));
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartPassFail));
        pieChartPassFail.setData(data);
        pieChartPassFail.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChartPassFail.animateY(900, Easing.EaseInOutQuad);
        pieChartPassFail.invalidate();
    }

    private void renderDifficultyBarChart(StatisticsResponse stats) {
        Context context = requireContext();
        barChartDifficulty.getDescription().setEnabled(false);
        barChartDifficulty.setDrawGridBackground(false);
        barChartDifficulty.setDrawBarShadow(false);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, new float[]{stats.getEasyCorrect(), stats.getEasyWrong()}));
        entries.add(new BarEntry(1f, new float[]{stats.getMediumCorrect(), stats.getMediumWrong()}));
        entries.add(new BarEntry(2f, new float[]{stats.getHardCorrect(), stats.getHardWrong()}));

        BarDataSet dataSet = new BarDataSet(entries, "Difficulty Breakdown");
        dataSet.setStackLabels(new String[]{"Correct", "Wrong"});
        dataSet.setColors(
                ContextCompat.getColor(context, R.color.success),
                ContextCompat.getColor(context, R.color.error)
        );
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.text_primary));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChartDifficulty.setData(barData);

        XAxis xAxis = barChartDifficulty.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Easy", "Medium", "Hard"}));
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

        barChartDifficulty.getAxisRight().setEnabled(false);
        barChartDifficulty.getAxisLeft().setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        barChartDifficulty.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barChartDifficulty.animateY(1000, Easing.EaseInOutCubic);
        barChartDifficulty.invalidate();
    }

    private void renderScoreTrendLineChart(StatisticsResponse stats) {
        Context context = requireContext();
        lineChartScoreProgression.getDescription().setEnabled(false);
        lineChartScoreProgression.setTouchEnabled(true);
        lineChartScoreProgression.setDrawGridBackground(false);

        List<StatisticsResponse.ExamScorePoint> points = stats.getScoreProgression();
        if (points == null || points.isEmpty()) {
            lineChartScoreProgression.setNoDataText("Take more exams to view score progression.");
            lineChartScoreProgression.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            StatisticsResponse.ExamScorePoint pt = points.get(i);
            entries.add(new Entry(i, pt.getAccuracy()));
            xLabels.add(pt.getExamTitle().isEmpty() ? ("Ex " + (i + 1)) : pt.getExamTitle());
        }

        LineDataSet dataSet = new LineDataSet(entries, "Accuracy (%)");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setColor(ContextCompat.getColor(context, R.color.primary));
        dataSet.setCircleColor(ContextCompat.getColor(context, R.color.secondary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.text_primary));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(context, R.color.primary_light));
        dataSet.setFillAlpha(45);

        LineData lineData = new LineData(dataSet);
        lineChartScoreProgression.setData(lineData);

        XAxis xAxis = lineChartScoreProgression.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        YAxis leftAxis = lineChartScoreProgression.getAxisLeft();
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

        lineChartScoreProgression.getAxisRight().setEnabled(false);
        lineChartScoreProgression.animateX(1100, Easing.EaseInOutSine);
        lineChartScoreProgression.invalidate();
    }

    private void renderSubjectMasteryHorizontalBar(StatisticsResponse stats) {
        Context context = requireContext();
        horizontalBarChartMastery.getDescription().setEnabled(false);
        horizontalBarChartMastery.setDrawGridBackground(false);

        List<StatisticsResponse.SubjectMasteryItem> items = stats.getSubjectMastery();
        if (items == null || items.isEmpty()) {
            horizontalBarChartMastery.setNoDataText("No subject mastery data available.");
            horizontalBarChartMastery.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> subjectLabels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            StatisticsResponse.SubjectMasteryItem item = items.get(i);
            float accuracy = item.getAccuracyPercentage();
            entries.add(new BarEntry(i, accuracy));
            subjectLabels.add(item.getSubjectTitle());

            if (accuracy < 50f) {
                colors.add(ContextCompat.getColor(context, R.color.error));
            } else if (accuracy < 75f) {
                colors.add(ContextCompat.getColor(context, R.color.warning));
            } else {
                colors.add(ContextCompat.getColor(context, R.color.success));
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Mastery %");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.text_primary));
        dataSet.setValueTextSize(11f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        horizontalBarChartMastery.setData(barData);

        XAxis xAxis = horizontalBarChartMastery.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(subjectLabels));
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

        YAxis yAxis = horizontalBarChartMastery.getAxisLeft();
        yAxis.setAxisMaximum(100f);
        yAxis.setAxisMinimum(0f);
        yAxis.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

        // Add 60% Passing Benchmark LimitLine
        LimitLine passLine = new LimitLine(60f, "Passing Benchmark (60%)");
        passLine.setLineColor(ContextCompat.getColor(context, R.color.primary_light));
        passLine.setLineWidth(1.5f);
        passLine.setTextColor(ContextCompat.getColor(context, R.color.primary));
        passLine.enableDashedLine(8f, 4f, 0f);
        yAxis.addLimitLine(passLine);

        horizontalBarChartMastery.getAxisRight().setEnabled(false);
        horizontalBarChartMastery.getLegend().setEnabled(false);
        horizontalBarChartMastery.animateY(1000, Easing.EaseInOutCubic);
        horizontalBarChartMastery.invalidate();
    }

    private void handlePracticeWeakTopics() {
        if (cachedStats == null || cachedStats.getSubjectMastery() == null || cachedStats.getSubjectMastery().isEmpty()) {
            Toast.makeText(requireContext(), "No subject analytics available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find subject with lowest accuracy
        StatisticsResponse.SubjectMasteryItem weakest = null;
        for (StatisticsResponse.SubjectMasteryItem item : cachedStats.getSubjectMastery()) {
            if (weakest == null || item.getAccuracyPercentage() < weakest.getAccuracyPercentage()) {
                weakest = item;
            }
        }

        if (weakest != null) {
            Intent intent = new Intent(requireContext(), SearchResultActivity.class);
            intent.putExtra("type", "subject_id");
            intent.putExtra("id", weakest.getSubjectId());
            intent.putExtra("title", "Drill: " + weakest.getSubjectTitle());
            startActivity(intent);
        }
    }
}
