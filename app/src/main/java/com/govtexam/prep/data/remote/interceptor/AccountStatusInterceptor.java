package com.govtexam.prep.data.remote.interceptor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.govtexam.prep.GovtExamApp;
import com.govtexam.prep.data.local.SessionManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Intercepts HTTP 403 / status responses from Laravel User Manager to handle
 * account suspension and permanent disabling dynamically.
 */
public class AccountStatusInterceptor implements Interceptor {

    private static final String TAG = "AccountStatus";
    public static final String ACTION_ACCOUNT_STATUS = "com.govtexam.prep.ACTION_ACCOUNT_STATUS";
    public static final String EXTRA_STATUS = "extra_status"; // "suspended" or "disabled"
    public static final String EXTRA_REASON = "extra_reason";
    public static final String EXTRA_EXPIRES = "extra_expires";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        // Check for 403 Forbidden or custom account status payloads
        if (response.code() == 403 || response.code() == 401) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body
                Buffer buffer = source.getBuffer();

                Charset charset = StandardCharsets.UTF_8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    Charset cs = contentType.charset(StandardCharsets.UTF_8);
                    if (cs != null) {
                        charset = cs;
                    }
                }

                String bodyString = buffer.clone().readString(charset);
                try {
                    JsonObject jsonObject = JsonParser.parseString(bodyString).getAsJsonObject();
                    if (jsonObject.has("account_status")) {
                        String status = jsonObject.get("account_status").getAsString();
                        String reason = jsonObject.has("suspension_reason") && !jsonObject.get("suspension_reason").isJsonNull()
                                ? jsonObject.get("suspension_reason").getAsString()
                                : "Policy violation or administrative action.";
                        String expires = jsonObject.has("suspension_expires_at") && !jsonObject.get("suspension_expires_at").isJsonNull()
                                ? jsonObject.get("suspension_expires_at").getAsString()
                                : null;

                        handleAccountStatus(status, reason, expires);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Not a custom account status JSON response: " + e.getMessage());
                }
            }
        }

        return response;
    }

    private void handleAccountStatus(String status, String reason, String expires) {
        Context context = GovtExamApp.getInstance();
        if (context == null) return;

        if ("disabled".equalsIgnoreCase(status)) {
            // Clear local token and session
            SessionManager.getInstance().clearSession();

            Intent intent = new Intent(ACTION_ACCOUNT_STATUS);
            intent.putExtra(EXTRA_STATUS, "disabled");
            intent.putExtra(EXTRA_REASON, reason);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        } else if ("suspended".equalsIgnoreCase(status)) {
            Intent intent = new Intent(ACTION_ACCOUNT_STATUS);
            intent.putExtra(EXTRA_STATUS, "suspended");
            intent.putExtra(EXTRA_REASON, reason);
            intent.putExtra(EXTRA_EXPIRES, expires);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }
}
