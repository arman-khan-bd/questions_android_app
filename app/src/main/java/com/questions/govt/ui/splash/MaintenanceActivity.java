package com.questions.govt.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.questions.govt.R;
import com.questions.govt.data.model.AppConfigData;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.util.FeatureFlagManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full-screen maintenance mode interceptor.
 * Blocks navigation and displays Admin's custom maintenance message, banner, and estimated return time.
 */
public class MaintenanceActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "extra_maintenance_msg";
    public static final String EXTRA_BANNER = "extra_maintenance_banner";
    public static final String EXTRA_ESTIMATED_TIME = "extra_estimated_time";

    private ImageView ivMaintenanceBanner;
    private TextView tvMaintenanceMessage;
    private MaterialCardView cardEstimatedTime;
    private TextView tvEstimatedTime;
    private Button btnRetry;
    private ProgressBar pbRetryLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        initViews();
        bindData();
    }

    private void initViews() {
        ivMaintenanceBanner = findViewById(R.id.ivMaintenanceBanner);
        tvMaintenanceMessage = findViewById(R.id.tvMaintenanceMessage);
        cardEstimatedTime = findViewById(R.id.cardEstimatedTime);
        tvEstimatedTime = findViewById(R.id.tvEstimatedTime);
        btnRetry = findViewById(R.id.btnRetry);
        pbRetryLoading = findViewById(R.id.pbRetryLoading);

        btnRetry.setOnClickListener(v -> checkMaintenanceStatus());
    }

    private void bindData() {
        AppConfigData config = FeatureFlagManager.getInstance().getConfig();

        String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (TextUtils.isEmpty(message) && config != null) {
            message = config.getMaintenanceMessage();
        }
        if (!TextUtils.isEmpty(message)) {
            tvMaintenanceMessage.setText(message);
        }

        String estimatedTime = getIntent().getStringExtra(EXTRA_ESTIMATED_TIME);
        if (TextUtils.isEmpty(estimatedTime) && config != null) {
            estimatedTime = config.getEstimatedBackAt();
        }
        if (!TextUtils.isEmpty(estimatedTime)) {
            cardEstimatedTime.setVisibility(View.VISIBLE);
            tvEstimatedTime.setText(getString(R.string.maintenance_est_time, estimatedTime));
        } else {
            cardEstimatedTime.setVisibility(View.GONE);
        }

        String bannerUrl = getIntent().getStringExtra(EXTRA_BANNER);
        if (TextUtils.isEmpty(bannerUrl) && config != null) {
            bannerUrl = config.getMaintenanceBannerUrl();
        }
        if (!TextUtils.isEmpty(bannerUrl)) {
            Glide.with(this)
                    .load(bannerUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_maintenance)
                    .error(R.drawable.ic_maintenance)
                    .into(ivMaintenanceBanner);
        }
    }

    private void checkMaintenanceStatus() {
        btnRetry.setEnabled(false);
        pbRetryLoading.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getAppConfig().enqueue(new Callback<ApiResponse<AppConfigData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AppConfigData>> call, Response<ApiResponse<AppConfigData>> response) {
                btnRetry.setEnabled(true);
                pbRetryLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AppConfigData newConfig = response.body().getData();
                    FeatureFlagManager.getInstance().updateConfig(newConfig);

                    if (!newConfig.isMaintenanceMode()) {
                        Toast.makeText(MaintenanceActivity.this, "Servers are back online!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MaintenanceActivity.this, SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        bindData();
                        Toast.makeText(MaintenanceActivity.this, "Maintenance is still ongoing. Please retry shortly.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MaintenanceActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AppConfigData>> call, Throwable t) {
                btnRetry.setEnabled(true);
                pbRetryLoading.setVisibility(View.GONE);
                Toast.makeText(MaintenanceActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back press from bypassing maintenance
        finishAffinity();
    }
}
