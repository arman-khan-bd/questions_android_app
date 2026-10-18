package com.govtexam.prep.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.govtexam.prep.data.model.User;

/**
 * Thread-safe Session & Sanctum Bearer Token manager.
 * Supports Android 5.0 (API 21) with graceful fallback from Keystore EncryptedSharedPreferences.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "govt_exam_session_pref";
    private static final String KEY_TOKEN = "sanctum_bearer_token";
    private static final String KEY_USER = "current_authenticated_user";
    private static final String KEY_DARK_MODE = "is_dark_mode_enabled";
    private static final String KEY_APP_CONFIG = "cached_app_config_json";
    private static final String KEY_FCM_TOKEN = "fcm_device_token";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        this.gson = new Gson();
        SharedPreferences sharedPreferences = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                sharedPreferences = EncryptedSharedPreferences.create(
                        context,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception e) {
                Log.w(TAG, "EncryptedSharedPreferences init failed, falling back to standard prefs", e);
            }
        }

        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = sharedPreferences;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SessionManager must be initialized before use.");
        }
        return instance;
    }

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public void saveUser(User user) {
        if (user != null) {
            String json = gson.toJson(user);
            prefs.edit().putString(KEY_USER, json).apply();
        }
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json != null) {
            try {
                return gson.fromJson(json, User.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse cached user json", e);
            }
        }
        return null;
    }

    public void saveFcmToken(String fcmToken) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply();
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    public void saveCachedConfig(String configJson) {
        prefs.edit().putString(KEY_APP_CONFIG, configJson).apply();
    }

    public String getCachedConfig() {
        return prefs.getString(KEY_APP_CONFIG, null);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER)
                .apply();
    }
}
