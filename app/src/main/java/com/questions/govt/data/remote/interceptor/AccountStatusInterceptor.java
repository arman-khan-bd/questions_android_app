package com.questions.govt.data.remote.interceptor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.questions.govt.GovtExamApp;
import com.questions.govt.data.local.SessionManager;

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
 * Intercepts HTTP 403 / 401 status responses from Laravel User Manager to handle
 * account suspension, auto logout, and permanent disabling dynamically.
 */
public class AccountStatusInterceptor implements Interceptor {

    private static final String TAG = "AccountStatus";
    public static final String ACTION_ACCOUNT_STATUS = "com.questions.govt.ACTION_ACCOUNT_STATUS";
    public static final String EXTRA_STATUS = "extra_status"; // "suspended" or "disabled"
    public static final String EXTRA_REASON = "extra_reason";
    public static final String EXTRA_EXPIRES = "extra_expires";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        // Check for 403 Forbidden custom account status payloads (suspended or disabled)
        if (response.code() == 403) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                try {
                    BufferedSource source = responseBody.source();
                    source.request(65536); // Safely buffer up to 64KB without blocking
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
                    JsonObject jsonObject = JsonParser.parseString(bodyString).getAsJsonObject();

                    boolean isSuspended = (jsonObject.has("account_status") && "suspended".equalsIgnoreCase(jsonObject.get("account_status").getAsString()))
                            || (jsonObject.has("is_suspended") && jsonObject.get("is_suspended").getAsBoolean());
                    boolean isDisabled = (jsonObject.has("account_status") && "disabled".equalsIgnoreCase(jsonObject.get("account_status").getAsString()));

                    if (isSuspended || isDisabled) {
                        String status = isSuspended ? "suspended" : "disabled";
                        String reason = "Administrative action / community guidelines.";
                        if (jsonObject.has("suspension_reason") && !jsonObject.get("suspension_reason").isJsonNull()) {
                            reason = jsonObject.get("suspension_reason").getAsString();
                        } else if (jsonObject.has("reason") && !jsonObject.get("reason").isJsonNull()) {
                            reason = jsonObject.get("reason").getAsString();
                        } else if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                            reason = jsonObject.get("message").getAsString();
                        }

                        String expires = null;
                        if (jsonObject.has("suspended_until") && !jsonObject.get("suspended_until").isJsonNull()) {
                            expires = jsonObject.get("suspended_until").getAsString();
                        } else if (jsonObject.has("suspension_expires_at") && !jsonObject.get("suspension_expires_at").isJsonNull()) {
                            expires = jsonObject.get("suspension_expires_at").getAsString();
                        }

                        handleAccountStatus(status, reason, expires);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Not a custom account status JSON response: " + e.getMessage());
                }
            }
        } else if (response.code() == 401) {
            // Token was revoked or deleted (e.g. user suspended by admin)
            // Immediately clear session so app auto-logs out on next reload / request
            try {
                if (SessionManager.getInstance().isLoggedIn()) {
                    SessionManager.getInstance().clearSession();
                    handleAccountStatus("disabled", "Session expired or revoked by administrator.", null);
                }
            } catch (Exception e) {
                Log.d(TAG, "401 auto logout handler error: " + e.getMessage());
            }
        }

        return response;
    }

    private void handleAccountStatus(String status, String reason, String expires) {
        Context context = GovtExamApp.getInstance();
        if (context == null) return;

        // Auto logout: immediately clear session on both suspend and disable
        SessionManager.getInstance().clearSession();

        Intent intent = new Intent(ACTION_ACCOUNT_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_REASON, reason);
        if (expires != null) {
            intent.putExtra(EXTRA_EXPIRES, expires);
        }
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}
