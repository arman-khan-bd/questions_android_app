package com.govtexam.prep.data.remote.interceptor;

import androidx.annotation.NonNull;

import com.govtexam.prep.data.local.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor that attaches Laravel Sanctum Bearer token and standard JSON headers to every request.
 */
public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "GovtExamPrep-Android/1.0");

        String token = SessionManager.getInstance().getAuthToken();
        if (token != null && !token.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }

        return chain.proceed(builder.build());
    }
}
