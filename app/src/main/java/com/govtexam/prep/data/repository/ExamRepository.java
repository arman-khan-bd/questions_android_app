package com.govtexam.prep.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.govtexam.prep.data.local.AppDatabase;
import com.govtexam.prep.data.local.dao.ExamDao;
import com.govtexam.prep.data.local.entity.OfflineExamSubmission;
import com.govtexam.prep.data.model.CustomExamRequest;
import com.govtexam.prep.data.model.ExamQuestionPreview;
import com.govtexam.prep.data.model.ExamResult;
import com.govtexam.prep.data.remote.ApiClient;
import com.govtexam.prep.data.remote.ApiService;
import com.govtexam.prep.data.remote.response.ApiResponse;
import com.govtexam.prep.data.remote.response.CustomExamGenerateResponse;
import com.govtexam.prep.data.remote.response.ExamDetailResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository managing exam generation, submissions, answers review, and offline queue resilience.
 */
public class ExamRepository {

    private static final String TAG = "ExamRepo";
    private static volatile ExamRepository instance;

    private final ExamDao examDao;
    private final ApiService apiService;
    private final Gson gson;

    private ExamRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.examDao = db.examDao();
        this.apiService = ApiClient.getApiService();
        this.gson = new Gson();
    }

    public static ExamRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ExamRepository.class) {
                if (instance == null) {
                    instance = new ExamRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<ExamResult>> getExamHistory() {
        return examDao.getAllExamResults();
    }

    public void generateCustomExam(CustomExamRequest request, ExamCallback<CustomExamGenerateResponse> callback) {
        apiService.generateCustomExam(request).enqueue(new Callback<ApiResponse<CustomExamGenerateResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CustomExamGenerateResponse>> call, Response<ApiResponse<CustomExamGenerateResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Could not generate custom exam. Please check taxonomy filters.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CustomExamGenerateResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getExamDetail(long examId, ExamCallback<ExamDetailResponse> callback) {
        apiService.getExamDetail(examId).enqueue(new Callback<ApiResponse<ExamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamDetailResponse>> call, Response<ApiResponse<ExamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Exam details unavailable.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamDetailResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    /**
     * Submits exam answers.
     * If network drops at exam conclusion, securely queues the submission in Room.
     */
    public void submitExam(
            long examId,
            String examTitle,
            Map<Long, String> answersMap,
            long timeSpentSeconds,
            ExamCallback<ExamResult> callback
    ) {
        Map<String, Object> submissionBody = new HashMap<>();
        submissionBody.put("answers", answersMap);
        submissionBody.put("time_spent_seconds", timeSpentSeconds);

        apiService.submitExam(examId, submissionBody).enqueue(new Callback<ApiResponse<ExamResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamResult>> call, Response<ApiResponse<ExamResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    ExamResult result = response.body().getData();
                    // Cache result in Room
                    AppDatabase.databaseWriteExecutor.execute(() -> examDao.insertExamResult(result));
                    callback.onSuccess(result);
                } else {
                    handleOfflineQueueFallback(examId, examTitle, answersMap, timeSpentSeconds, callback);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamResult>> call, Throwable t) {
                handleOfflineQueueFallback(examId, examTitle, answersMap, timeSpentSeconds, callback);
            }
        });
    }

    private void handleOfflineQueueFallback(
            long examId,
            String examTitle,
            Map<Long, String> answersMap,
            long timeSpentSeconds,
            ExamCallback<ExamResult> callback
    ) {
        Log.w(TAG, "Network connection unavailable at exam submission. Enqueuing locally in Room.");

        OfflineExamSubmission offlineItem = new OfflineExamSubmission();
        offlineItem.setExamId(examId);
        offlineItem.setExamTitle(examTitle);
        offlineItem.setAnswersJson(gson.toJson(answersMap));
        offlineItem.setTimeSpentSeconds(timeSpentSeconds);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            long localId = examDao.insertOfflineSubmission(offlineItem);

            // Construct synthetic result for immediate user reassurance
            ExamResult synthetic = new ExamResult();
            synthetic.setId(localId);
            synthetic.setExamId(examId);
            synthetic.setTitle(examTitle);
            synthetic.setTotalQuestions(answersMap.size());
            synthetic.setAttemptedCount(answersMap.size());
            synthetic.setTimeSpentSeconds(timeSpentSeconds);
            synthetic.setCompletedAt("Pending Sync (Saved Offline)");

            callback.onSuccess(synthetic);
        });
    }

    /**
     * Synchronizes queued offline submissions when network becomes available.
     */
    public void syncPendingSubmissions() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<OfflineExamSubmission> pending = examDao.getPendingSubmissions();
            if (pending == null || pending.isEmpty()) return;

            for (OfflineExamSubmission item : pending) {
                try {
                    Map<String, String> answers = gson.fromJson(item.getAnswersJson(), Map.class);
                    Map<String, Object> body = new HashMap<>();
                    body.put("answers", answers);
                    body.put("time_spent_seconds", item.getTimeSpentSeconds());

                    Response<ApiResponse<ExamResult>> response =
                            apiService.submitExam(item.getExamId(), body).execute();

                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        examDao.markSubmissionSynced(item.getLocalId());
                        examDao.insertExamResult(response.body().getData());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed syncing offline exam submission: " + item.getLocalId(), e);
                }
            }
            examDao.deleteSyncedSubmissions();
        });
    }

    public void getExamAnswerPreview(long examId, ExamCallback<List<ExamQuestionPreview>> callback) {
        apiService.getExamAnswerPreview(examId).enqueue(new Callback<ApiResponse<List<ExamQuestionPreview>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ExamQuestionPreview>>> call, Response<ApiResponse<List<ExamQuestionPreview>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Answer preview unavailable.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ExamQuestionPreview>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface ExamCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
