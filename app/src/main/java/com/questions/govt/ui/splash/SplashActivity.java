package com.questions.govt.ui.splash;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.questions.govt.BuildConfig;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.AppConfigData;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.ui.auth.LoginActivity;
import com.questions.govt.ui.main.MainActivity;
import com.questions.govt.util.FeatureFlagManager;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bootstrap SplashActivity.
 * - Loads /api/v1/app-config
 * - Dynamically binds branding (Name, Tagline, WebP logo)
 * - Intercepts for Maintenance Mode
 * - Handles Force and Optional OTA Updates with DownloadManager live progress
 * - Synchronizes remote feature flags and score multipliers
 * - Routes to MainActivity or LoginActivity
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private ImageView ivSplashLogo;
    private TextView tvAppName;
    private TextView tvAppTagline;
    private TextView tvStatus;
    private TextView tvVersion;
    private ProgressBar progressBar;

    private DownloadManager downloadManager;
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;
    private AlertDialog updateDialog;
    private ProgressBar pbDownload;
    private TextView tvProgressPercent;
    private LinearLayout layoutDownloadProgress;
    private Handler progressHandler;
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        applyCachedBranding();
        fetchRemoteAppConfig();
    }

    private void initViews() {
        ivSplashLogo = findViewById(R.id.ivSplashLogo);
        tvAppName = findViewById(R.id.tvAppName);
        tvAppTagline = findViewById(R.id.tvAppTagline);
        tvStatus = findViewById(R.id.tvStatus);
        tvVersion = findViewById(R.id.tvVersion);
        progressBar = findViewById(R.id.progressBar);

        String versionText = getString(R.string.app_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        tvVersion.setText(versionText);
    }

    /**
     * Applies cached branding immediately to ensure zero visual delay on cold launch.
     */
    private void applyCachedBranding() {
        AppConfigData config = FeatureFlagManager.getInstance().getConfig();
        if (config != null) {
            tvAppName.setText(config.getAppName());
            if (!TextUtils.isEmpty(config.getTagline())) {
                tvAppTagline.setText(config.getTagline());
            }
            if (!TextUtils.isEmpty(config.getLogoUrl())) {
                Glide.with(this)
                        .load(config.getLogoUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_app_logo)
                        .error(R.drawable.ic_app_logo)
                        .into(ivSplashLogo);
            }
        }
    }

    /**
     * Calls GET /api/v1/app-config to sync configuration, maintenance status, and updates.
     */
    private void fetchRemoteAppConfig() {
        tvStatus.setText(R.string.loading);

        ApiClient.getApiService().getAppConfig().enqueue(new Callback<ApiResponse<AppConfigData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AppConfigData>> call, Response<ApiResponse<AppConfigData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AppConfigData config = response.body().getData();
                    FeatureFlagManager.getInstance().updateConfig(config);
                    handleAppConfig(config);
                } else {
                    Log.w(TAG, "Config response not successful, using cached/offline fallback.");
                    proceedToNextScreen();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AppConfigData>> call, Throwable t) {
                Log.e(TAG, "Network failure fetching /app-config, continuing offline: " + t.getMessage());
                proceedToNextScreen();
            }
        });
    }

    private void handleAppConfig(AppConfigData config) {
        // 1. Dynamic Branding Update
        tvAppName.setText(config.getAppName());
        if (!TextUtils.isEmpty(config.getTagline())) {
            tvAppTagline.setText(config.getTagline());
        }
        if (!TextUtils.isEmpty(config.getLogoUrl())) {
            Glide.with(this)
                    .load(config.getLogoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(ivSplashLogo);
        }

        // 2. Maintenance Mode Check
        if (config.isMaintenanceMode()) {
            Intent intent = new Intent(this, MaintenanceActivity.class);
            intent.putExtra(MaintenanceActivity.EXTRA_MESSAGE, config.getMaintenanceMessage());
            intent.putExtra(MaintenanceActivity.EXTRA_BANNER, config.getMaintenanceBannerUrl());
            intent.putExtra(MaintenanceActivity.EXTRA_ESTIMATED_TIME, config.getEstimatedBackAt());
            startActivity(intent);
            finish();
            return;
        }

        // 3. OTA Version Update Controller
        int localVersionCode = BuildConfig.VERSION_CODE;
        int remoteVersionCode = config.getLatestVersionCode();
        String updateMode = config.getUpdateMode();

        if (remoteVersionCode > localVersionCode && !"none".equalsIgnoreCase(updateMode)) {
            showUpdateDialog(config, "force".equalsIgnoreCase(updateMode));
        } else {
            proceedToNextScreen();
        }
    }

    /**
     * Renders either an Optional or Non-Cancelable Force Update Dialog.
     */
    private void showUpdateDialog(AppConfigData config, boolean isForceUpdate) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ota_update, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvUpdateTitle);
        TextView tvVersionInfo = dialogView.findViewById(R.id.tvVersionInfo);
        TextView tvReleaseNotes = dialogView.findViewById(R.id.tvReleaseNotes);
        Button btnLater = dialogView.findViewById(R.id.btnLater);
        Button btnUpdateNow = dialogView.findViewById(R.id.btnUpdateNow);
        pbDownload = dialogView.findViewById(R.id.progressBarDownload);
        tvProgressPercent = dialogView.findViewById(R.id.tvProgressPercent);
        layoutDownloadProgress = dialogView.findViewById(R.id.layoutDownloadProgress);

        if (isForceUpdate) {
            tvTitle.setText(R.string.update_required_title);
            btnLater.setVisibility(View.GONE);
        } else {
            tvTitle.setText(R.string.update_available_title);
            btnLater.setVisibility(View.VISIBLE);
        }

        tvVersionInfo.setText(getString(R.string.app_version_format, config.getLatestVersionName(), config.getLatestVersionCode()));
        if (!TextUtils.isEmpty(config.getReleaseNotes())) {
            tvReleaseNotes.setText(config.getReleaseNotes());
        } else {
            tvReleaseNotes.setText("Performance improvements and new MCQ exam features.");
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(!isForceUpdate);

        updateDialog = builder.create();
        updateDialog.setCanceledOnTouchOutside(!isForceUpdate);

        btnLater.setOnClickListener(v -> {
            updateDialog.dismiss();
            proceedToNextScreen();
        });

        btnUpdateNow.setOnClickListener(v -> {
            String downloadUrl = config.getUpdateDownloadUrl();
            if (TextUtils.isEmpty(downloadUrl)) {
                openPlayStore();
            } else if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                if (downloadUrl.endsWith(".apk")) {
                    startApkDownload(downloadUrl);
                    btnUpdateNow.setEnabled(false);
                } else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    startActivity(browserIntent);
                }
            } else {
                openPlayStore();
            }
        });

        updateDialog.show();
    }

    private void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    /**
     * Downloads direct APK using Android's DownloadManager with live progress tracking.
     */
    private void startApkDownload(String apkUrl) {
        layoutDownloadProgress.setVisibility(View.VISIBLE);
        pbDownload.setProgress(0);
        tvProgressPercent.setText(getString(R.string.downloading_update, 0));

        try {
            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(apkUrl);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(getString(R.string.app_name) + " Update");
            request.setDescription("Downloading latest application update…");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "update.apk");

            downloadId = downloadManager.enqueue(request);
            registerDownloadReceiver();
            trackDownloadProgress();
        } catch (Exception e) {
            Log.e(TAG, "Error starting APK download: " + e.getMessage());
            Toast.makeText(this, "Failed to start direct download. Opening browser...", Toast.LENGTH_SHORT).show();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
            startActivity(browserIntent);
        }
    }

    private void trackDownloadProgress() {
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (downloadId != -1 && downloadManager != null) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(query);
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                        if (bytesDownloadedIdx != -1 && bytesTotalIdx != -1) {
                            long bytesDownloaded = cursor.getLong(bytesDownloadedIdx);
                            long bytesTotal = cursor.getLong(bytesTotalIdx);

                            if (bytesTotal > 0) {
                                int progress = (int) ((bytesDownloaded * 100) / bytesTotal);
                                pbDownload.setProgress(progress);
                                tvProgressPercent.setText(getString(R.string.downloading_update, progress));
                            }
                        }
                        cursor.close();
                    }
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    if (progressHandler != null && progressRunnable != null) {
                        progressHandler.removeCallbacks(progressRunnable);
                    }
                    installDownloadedApk();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void installDownloadedApk() {
        try {
            File apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
            if (apkFile.exists()) {
                Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(installIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package installer", e);
            Toast.makeText(this, "Could not open update file. Please install manually.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Determines whether to navigate to MainActivity or LoginActivity.
     */
    private void proceedToNextScreen() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;

            Intent intent;
            if (SessionManager.getInstance().isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1200);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadReceiver != null) {
            try {
                unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
            }
        }
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
    }
}
