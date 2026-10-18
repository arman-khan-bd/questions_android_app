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

    private static final long HOME_CACHE_EXPIRATION_MS = 10 * 60 * 1000L; // 10 minutes cache
    private HomeFeedResponse cachedHomeFeed = null;
    private long lastHomeFeedFetchTime = 0L;

    public void clearHomeFeedCache() {
        cachedHomeFeed = null;
        lastHomeFeedFetchTime = 0L;
    }

    public boolean isHomeFeedCacheValid() {
        return cachedHomeFeed != null && (System.currentTimeMillis() - lastHomeFeedFetchTime < HOME_CACHE_EXPIRATION_MS);
    }

    /**
     * Synchronizes Home Feed from network and caches taxonomy items in SQLite Room.
     * Uses in-memory cache valid for 10 minutes unless forceRefresh is true.
     */
    public void fetchHomeFeed(HomeFeedCallback callback) {
        fetchHomeFeed(false, callback);
    }

    public void fetchHomeFeed(boolean forceRefresh, HomeFeedCallback callback) {
        if (!forceRefresh && isHomeFeedCacheValid()) {
            Log.d(TAG, "Serving Home Feed from 10-minute in-memory cache.");
            if (callback != null) {
                callback.onSuccess(cachedHomeFeed);
            }
            return;
        }

        apiService.getHomeFeed().enqueue(new Callback<ApiResponse<HomeFeedResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomeFeedResponse>> call, Response<ApiResponse<HomeFeedResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    HomeFeedResponse feed = response.body().getData();
                    cachedHomeFeed = feed;
                    lastHomeFeedFetchTime = System.currentTimeMillis();

                    List<TaxonomyItem> allItems = new ArrayList<>();
                    if (feed.getSubjects() != null) {
                        for (TaxonomyItem item : feed.getSubjects()) {
                            item.setType("subject");
                            allItems.add(item);
                        }
                    }
                    if (feed.getTopics() != null) {
                        for (TaxonomyItem item : feed.getTopics()) {
                            item.setType("topic");
                            allItems.add(item);
                        }
                    }
                    if (feed.getCategories() != null) {
                        for (TaxonomyItem item : feed.getCategories()) {
                            item.setType("category");
                            allItems.add(item);
                        }
                    }
                    if (feed.getTags() != null) {
                        for (TaxonomyItem item : feed.getTags()) {
                            item.setType("tag");
                            allItems.add(item);
                        }
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> taxonomyDao.insertOrReplaceAll(allItems));

                    if (callback != null) callback.onSuccess(feed);
                } else {
                    if (cachedHomeFeed != null && callback != null) {
                        callback.onSuccess(cachedHomeFeed);
                    } else if (callback != null) {
                        callback.onError("Failed to fetch home feed.");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HomeFeedResponse>> call, Throwable t) {
                Log.w(TAG, "Network failure fetching home feed, serving from cache: " + t.getMessage());
                if (cachedHomeFeed != null && callback != null) {
                    callback.onSuccess(cachedHomeFeed);
                } else if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public interface HomeFeedCallback {
        void onSuccess(HomeFeedResponse response);
        void onError(String message);
    }
}
