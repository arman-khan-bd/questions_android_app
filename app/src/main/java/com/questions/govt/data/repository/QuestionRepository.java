package com.questions.govt.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.questions.govt.data.local.AppDatabase;
import com.questions.govt.data.local.dao.QuestionDao;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.ApiService;
import com.questions.govt.data.remote.response.AnswerCheckResponse;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.StarResponse;
import com.questions.govt.data.remote.response.VoteResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository orchestrating Question data, local Room caching, telemetry view batching,
 * offline mistake notebook, and rank-neutral answer checking.
 */
public class QuestionRepository {

    private static final String TAG = "QuestionRepo";
    private static volatile QuestionRepository instance;

    private final QuestionDao questionDao;
    private final ApiService apiService;
    private final Set<Long> pendingViewImpressions = Collections.synchronizedSet(new HashSet<>());

    private QuestionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.questionDao = db.questionDao();
        this.apiService = ApiClient.getApiService();
    }

    public static QuestionRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (QuestionRepository.class) {
                if (instance == null) {
                    instance = new QuestionRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<Question>> getStarredQuestions() {
        return questionDao.getStarredQuestions();
    }

    public LiveData<List<Question>> getMistakeNotebookQuestions() {
        return questionDao.getMistakeNotebookQuestions();
    }

    public LiveData<Question> getQuestionById(long id) {
        return questionDao.getQuestionById(id);
    }

    public void cacheQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) return;
        AppDatabase.databaseWriteExecutor.execute(() -> questionDao.insertOrReplaceAll(questions));
    }

    public void cacheQuestion(Question question) {
        if (question == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> questionDao.insertOrReplace(question));
    }

    /**
     * Toggles star status with optimistic UI update in Room and syncs with backend.
     */
    public void toggleStar(Question question, RepositoryCallback<Boolean> callback) {
        final boolean newStarred = !question.isStarred();
        final int newCount = Math.max(0, question.getStarsCount() + (newStarred ? 1 : -1));

        question.setStarred(newStarred);
        question.setStarsCount(newCount);

        // Optimistic local update
        AppDatabase.databaseWriteExecutor.execute(() ->
                questionDao.updateStarStatus(question.getId(), newStarred, newCount));

        // Sync with backend
        apiService.toggleStarQuestion(question.getId()).enqueue(new Callback<ApiResponse<StarResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StarResponse>> call, Response<ApiResponse<StarResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    StarResponse data = response.body().getData();
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            questionDao.updateStarStatus(question.getId(), data.isStarred(), data.getStarsCount()));
                    if (callback != null) callback.onSuccess(data.isStarred());
                } else {
                    if (callback != null) callback.onSuccess(newStarred);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StarResponse>> call, Throwable t) {
                // Kept in offline cache
                if (callback != null) callback.onSuccess(newStarred);
            }
        });
    }

    /**
     * Updates mistake notebook flag in local Room database.
     */
    public void setMistakeStatus(long questionId, boolean isMistake) {
        AppDatabase.databaseWriteExecutor.execute(() ->
                questionDao.updateMistakeStatus(questionId, isMistake));
    }

    /**
     * Records a question view impression into a batch set and dispatches to server.
     */
    public void recordQuestionView(long questionId) {
        pendingViewImpressions.add(questionId);
        if (pendingViewImpressions.size() >= 5) {
            flushViewImpressions();
        }
    }

    public void flushViewImpressions() {
        if (pendingViewImpressions.isEmpty()) return;

        List<Long> batch;
        synchronized (pendingViewImpressions) {
            batch = new ArrayList<>(pendingViewImpressions);
            pendingViewImpressions.clear();
        }

        Map<String, List<Long>> body = new HashMap<>();
        body.put("question_ids", batch);

        apiService.recordQuestionViews(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d(TAG, "Question views telemetry flushed: " + batch.size());
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.w(TAG, "Failed to flush view telemetry", t);
            }
        });
    }

    /**
     * Instant MCQ answer check with telemetry percentage bars.
     */
    public void checkAnswer(long questionId, String selectedOption, RepositoryCallback<AnswerCheckResponse> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("selected_option", selectedOption);

        apiService.checkAnswer(questionId, body).enqueue(new Callback<ApiResponse<AnswerCheckResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AnswerCheckResponse>> call, Response<ApiResponse<AnswerCheckResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AnswerCheckResponse checkResult = response.body().getData();
                    // Update Room telemetry
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            questionDao.updateTelemetry(
                                    questionId,
                                    0,
                                    checkResult.getAttemptsCount(),
                                    checkResult.getOptionAPct(),
                                    checkResult.getOptionBPct(),
                                    checkResult.getOptionCPct(),
                                    checkResult.getOptionDPct()
                            ));
                    if (callback != null) callback.onSuccess(checkResult);
                } else {
                    if (callback != null) callback.onError("Failed to verify answer from server.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AnswerCheckResponse>> call, Throwable t) {
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    /**
     * Submits an answer accuracy vote. Rank is strictly protected.
     */
    public void voteAnswer(long questionId, String voteType, RepositoryCallback<VoteResponse> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("vote_type", voteType); // "up" or "down"

        apiService.voteQuestionAnswer(questionId, body).enqueue(new Callback<ApiResponse<VoteResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<VoteResponse>> call, Response<ApiResponse<VoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    if (callback != null) callback.onSuccess(response.body().getData());
                } else {
                    if (callback != null) callback.onError("Failed to submit vote.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VoteResponse>> call, Throwable t) {
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    public LiveData<List<Question>> searchQuestionsOffline(String query) {
        return questionDao.searchQuestionsOffline(query);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
