package com.govtexam.prep.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.govtexam.prep.data.local.AppDatabase;
import com.govtexam.prep.data.local.SessionManager;
import com.govtexam.prep.data.local.dao.UserDao;
import com.govtexam.prep.data.model.User;
import com.govtexam.prep.data.remote.ApiClient;
import com.govtexam.prep.data.remote.ApiService;
import com.govtexam.prep.data.remote.response.ApiResponse;
import com.govtexam.prep.data.remote.response.UserProfileResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for Facebook-style student profiles, avatars/covers, and account state.
 */
public class UserRepository {

    private static volatile UserRepository instance;

    private final UserDao userDao;
    private final ApiService apiService;

    private UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.apiService = ApiClient.getApiService();
    }

    public static UserRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<User> getUserById(long userId) {
        return userDao.getUserById(userId);
    }

    public void fetchUserProfile(String username, UserProfileCallback callback) {
        apiService.getUserProfile(username).enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileResponse>> call, Response<ApiResponse<UserProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    UserProfileResponse profile = response.body().getData();
                    if (profile.getUser() != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> userDao.insertOrReplace(profile.getUser()));
                    }
                    callback.onSuccess(profile);
                } else {
                    callback.onError("Could not load user profile.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateProfile(
            Map<String, RequestBody> fields,
            MultipartBody.Part avatar,
            MultipartBody.Part coverPhoto,
            UserUpdateCallback callback
    ) {
        apiService.updateProfile(fields, avatar, coverPhoto).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User updatedUser = response.body().getData();
                    SessionManager.getInstance().saveUser(updatedUser);
                    AppDatabase.databaseWriteExecutor.execute(() -> userDao.insertOrReplace(updatedUser));
                    callback.onSuccess(updatedUser);
                } else {
                    callback.onError("Profile update failed.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface UserProfileCallback {
        void onSuccess(UserProfileResponse response);
        void onError(String message);
    }

    public interface UserUpdateCallback {
        void onSuccess(User user);
        void onError(String message);
    }
}
