package com.govtexam.prep.data.remote;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.govtexam.prep.AppConfig;
import com.govtexam.prep.BuildConfig;
import com.govtexam.prep.GovtExamApp;
import com.govtexam.prep.data.remote.interceptor.AccountStatusInterceptor;
import com.govtexam.prep.data.remote.interceptor.AuthInterceptor;
import com.govtexam.prep.data.remote.response.ApiResponse;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Thread-safe singleton providing OkHttp and Retrofit instances,
 * hardened for TLS 1.2 on Android 5.0 (API 21) devices and matched
 * to the Laravel Sanctum REST API at AppConfig.SERVER_URL.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static volatile ApiService apiService;
    private static volatile OkHttpClient okHttpClient;
    private static volatile Retrofit retrofit;

    public static final String DEFAULT_BASE_URL = AppConfig.SERVER_URL;

    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (ApiClient.class) {
                if (okHttpClient == null) {
                    okHttpClient = buildOkHttpClient();
                }
            }
        }
        return okHttpClient;
    }

    private static OkHttpClient buildOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(new AccountStatusInterceptor());

        // HTTP Cache
        Context context = GovtExamApp.getInstance();
        if (context != null) {
            try {
                File cacheDir = new File(context.getCacheDir(), "http_cache");
                builder.cache(new Cache(cacheDir, AppConfig.HTTP_CACHE_SIZE_BYTES));
            } catch (Exception e) {
                Log.w(TAG, "Could not setup HTTP cache: " + e.getMessage());
            }
        }

        // Logging interceptor for debugging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(logging);

        // Safeguard for Android 5.0 (API 21) TLS 1.2 Handshake
        Tls12Helper.enableTls12(builder);

        return builder.build();
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(ApiResponse.class, new ApiResponseDeserializer())
                            .setLenient()
                            .create();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(DEFAULT_BASE_URL)
                            .client(getOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

                    apiService = retrofit.create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    /**
     * Flexible deserializer that bridges Laravel API responses:
     * - Standard "data" wrapped responses
     * - Named object responses ("result", "exam", "question", "comment", "notifications", "student")
     * - Root-level properties (e.g. login tokens, home feed arrays, branding config)
     */
    public static class ApiResponseDeserializer implements JsonDeserializer<ApiResponse<?>> {
        @Override
        public ApiResponse<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ApiResponse<Object> response = new ApiResponse<>();

            if (!json.isJsonObject()) {
                return response;
            }

            JsonObject jsonObject = json.getAsJsonObject();

            if (jsonObject.has("success")) {
                response.setSuccess(jsonObject.get("success").getAsBoolean());
            } else {
                response.setSuccess(true);
            }

            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                response.setMessage(jsonObject.get("message").getAsString());
            }

            Type innerType = null;
            if (typeOfT instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) typeOfT).getActualTypeArguments();
                if (args.length > 0) {
                    innerType = args[0];
                }
            }

            if (innerType != null && !Void.class.equals(innerType)) {
                if (UserProfileResponse.class.equals(innerType)
                        || StatisticsResponse.class.equals(innerType)
                        || LeaderboardResponse.class.equals(innerType)
                        || HomeFeedResponse.class.equals(innerType)
                        || AppConfigData.class.equals(innerType)) {
                    try {
                        response.setData(context.deserialize(json, innerType));
                    } catch (Exception e) {
                        Log.w(TAG, "Root composite deserialization: " + e.getMessage());
                    }
                } else if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("data"), innerType));
                } else if (jsonObject.has("result") && !jsonObject.get("result").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("result"), innerType));
                } else if (jsonObject.has("exam") && !jsonObject.get("exam").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("exam"), innerType));
                } else if (jsonObject.has("question") && !jsonObject.get("question").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("question"), innerType));
                } else if (jsonObject.has("comment") && !jsonObject.get("comment").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("comment"), innerType));
                } else if (jsonObject.has("student") && !jsonObject.get("student").isJsonNull()) {
                    response.setData(context.deserialize(jsonObject.get("student"), innerType));
                } else if (jsonObject.has("notifications") && !jsonObject.get("notifications").isJsonNull()) {
                    JsonObject notifObj = jsonObject.getAsJsonObject("notifications");
                    if (notifObj.has("data")) {
                        response.setData(context.deserialize(notifObj.get("data"), innerType));
                    } else {
                        response.setData(context.deserialize(notifObj, innerType));
                    }
                } else {
                    // Fallback to deserializing root JSON object
                    try {
                        response.setData(context.deserialize(json, innerType));
                    } catch (Exception e) {
                        Log.w(TAG, "Root deserialization fallback info: " + e.getMessage());
                    }
                }
            }

            return response;
        }
    }
}
