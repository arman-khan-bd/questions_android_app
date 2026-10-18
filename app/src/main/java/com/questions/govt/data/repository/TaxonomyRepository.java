package com.questions.govt.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.questions.govt.data.local.AppDatabase;
import com.questions.govt.data.local.dao.TaxonomyDao;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.ApiService;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.HomeFeedResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository caching the Subjects, Topics, Categories, and Tags taxonomy tree in Room.
 * Enables complete offline browsing for Home Screen and Left Navigation Drawer.
 */
public class TaxonomyRepository {

    private static final String TAG = "TaxonomyRepo";
    private static volatile TaxonomyRepository instance;

    private final TaxonomyDao taxonomyDao;
    private final ApiService apiService;

    private TaxonomyRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.taxonomyDao = db.taxonomyDao();
        this.apiService = ApiClient.getApiService();
    }

    public static TaxonomyRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TaxonomyRepository.class) {
                if (instance == null) {
                    instance = new TaxonomyRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<TaxonomyItem>> getSubjects() {
        return taxonomyDao.getSubjects();
    }

    public LiveData<List<TaxonomyItem>> getTopics() {
        return taxonomyDao.getTopics();
    }

    public LiveData<List<TaxonomyItem>> getCategories() {
        return taxonomyDao.getCategories();
    }

    public LiveData<List<TaxonomyItem>> getTags() {
        return taxonomyDao.getTags();
    }

    /**
     * Synchronizes Home Feed from network and caches taxonomy items in SQLite Room.
     */
    public void fetchHomeFeed(HomeFeedCallback callback) {
        apiService.getHomeFeed().enqueue(new Callback<ApiResponse<HomeFeedResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomeFeedResponse>> call, Response<ApiResponse<HomeFeedResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    HomeFeedResponse feed = response.body().getData();

                    List<TaxonomyItem> allItems = new ArrayList<>();
                    if (feed.getSubjects() != null) allItems.addAll(feed.getSubjects());
                    if (feed.getTopics() != null) allItems.addAll(feed.getTopics());
                    if (feed.getCategories() != null) allItems.addAll(feed.getCategories());
                    if (feed.getTags() != null) allItems.addAll(feed.getTags());

                    AppDatabase.databaseWriteExecutor.execute(() -> taxonomyDao.insertOrReplaceAll(allItems));

                    if (callback != null) callback.onSuccess(feed);
                } else {
                    if (callback != null) callback.onError("Failed to fetch home feed.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HomeFeedResponse>> call, Throwable t) {
                Log.w(TAG, "Network failure fetching home feed, serving from Room cache: " + t.getMessage());
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    public interface HomeFeedCallback {
        void onSuccess(HomeFeedResponse response);
        void onError(String message);
    }
}
