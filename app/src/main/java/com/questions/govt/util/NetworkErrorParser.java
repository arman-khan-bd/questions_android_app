package com.questions.govt.util;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.questions.govt.data.remote.response.ApiResponse;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLException;

import retrofit2.Response;

/**
 * Robust error extractor for Laravel 11 Sanctum REST API responses.
 * Parses validation errors, JSON message envelopes, and HTTP network exceptions.
 */
public class NetworkErrorParser {

    private static final String TAG = "NetworkErrorParser";

    public static String parseError(Response<?> response, String fallbackMessage) {
        if (response == null) {
            return fallbackMessage;
        }

        try {
            // Check error body first (returned on 4xx, 5xx)
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                if (!TextUtils.isEmpty(errorJson)) {
                    JsonObject jsonObject = JsonParser.parseString(errorJson).getAsJsonObject();

                    // 1. Direct "message" field
                    if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                        String msg = jsonObject.get("message").getAsString();
                        if (!TextUtils.isEmpty(msg)) {
                            return msg;
                        }
                    }

                    // 2. Laravel validation "errors" object
                    if (jsonObject.has("errors") && jsonObject.get("errors").isJsonObject()) {
                        JsonObject errorsObj = jsonObject.getAsJsonObject("errors");
                        for (Map.Entry<String, JsonElement> entry : errorsObj.entrySet()) {
                            if (entry.getValue().isJsonArray()) {
                                JsonArray arr = entry.getValue().getAsJsonArray();
                                if (arr.size() > 0) {
                                    return arr.get(0).getAsString();
                                }
                            } else if (entry.getValue().isJsonPrimitive()) {
                                return entry.getValue().getAsString();
                            }
                        }
                    }
                }
            }

            // Fallback to successful body if message is present
            if (response.body() instanceof ApiResponse) {
                ApiResponse<?> apiResponse = (ApiResponse<?>) response.body();
                if (!TextUtils.isEmpty(apiResponse.getMessage())) {
                    return apiResponse.getMessage();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse API error response: " + e.getMessage());
        }

        return fallbackMessage;
    }

    public static String parseFailure(Throwable t, String defaultNetworkError) {
        if (t == null) {
            return defaultNetworkError;
        }

        if (t instanceof SocketTimeoutException) {
            return "Connection timed out. Please check your internet connection.";
        } else if (t instanceof UnknownHostException) {
            return "Unable to reach server. Please check your network and DNS connection.";
        } else if (t instanceof ConnectException) {
            return "Server refused connection. Please ensure the server is online.";
        } else if (t instanceof SSLException) {
            return "Secure connection failed (SSL/TLS certificate error).";
        }

        String msg = t.getLocalizedMessage();
        if (!TextUtils.isEmpty(msg)) {
            return msg;
        }

        return defaultNetworkError;
    }
}
