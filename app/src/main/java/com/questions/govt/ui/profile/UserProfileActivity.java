package com.questions.govt.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.User;
import com.questions.govt.data.remote.response.UserProfileResponse;
import com.questions.govt.data.repository.UserRepository;

import java.util.Locale;

/**
 * Activity for viewing any student's public Facebook-style profile,
 * launched by tapping avatars/handles across leaderboard, comments, and posts.
 */
public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "username";

    private ImageView imgCoverPhoto;
    private ImageView imgAvatar;
    private MaterialButton btnEditProfile;
    private TextView tvProfileName;
    private TextView tvProfileUsername;
    private TextView tvProfileBio;

    private MaterialCardView cardAcademicInfo;
    private TextView tvInstitution;
    private TextView tvDegreeAndYear;
    private TextView tvPhone;

    private TextView tvRankValue;
    private TextView tvScoreValue;
    private TextView tvTierValue;

    private MaterialToolbar toolbarProfile;
    private TabLayout tabLayoutProfile;
    private ViewPager2 viewPagerProfile;

    private ProfilePagerAdapter pagerAdapter;
    private UserRepository userRepository;
    private TabLayoutMediator tabMediator;
    private String targetUsername;

    public static void start(Context context, String username) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(EXTRA_USERNAME, username);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        userRepository = UserRepository.getInstance(this);
        targetUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        if (targetUsername == null || targetUsername.isEmpty()) {
            User loggedInUser = SessionManager.getInstance().getUser();
            if (loggedInUser != null) {
                targetUsername = loggedInUser.getUsername();
            }
        }

        initViews();
        setupViewPager();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void initViews() {
        imgCoverPhoto = findViewById(R.id.imgCoverPhoto);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileBio = findViewById(R.id.tvProfileBio);

        cardAcademicInfo = findViewById(R.id.cardAcademicInfo);
        tvInstitution = findViewById(R.id.tvInstitution);
        tvDegreeAndYear = findViewById(R.id.tvDegreeAndYear);
        tvPhone = findViewById(R.id.tvPhone);

        tvRankValue = findViewById(R.id.tvRankValue);
        tvScoreValue = findViewById(R.id.tvScoreValue);
        tvTierValue = findViewById(R.id.tvTierValue);

        toolbarProfile = findViewById(R.id.toolbarProfile);
        tabLayoutProfile = findViewById(R.id.tabLayoutProfile);
        viewPagerProfile = findViewById(R.id.viewPagerProfile);

        toolbarProfile.setNavigationOnClickListener(v -> finish());
        toolbarProfile.setTitle("@" + targetUsername);

        boolean isSelf = SessionManager.getInstance().isLoggedIn() &&
                targetUsername.equalsIgnoreCase(SessionManager.getInstance().getUser().getUsername());

        btnEditProfile.setVisibility(isSelf ? View.VISIBLE : View.GONE);
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupViewPager() {
        pagerAdapter = new ProfilePagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPagerProfile.setAdapter(pagerAdapter);

        tabMediator = new TabLayoutMediator(tabLayoutProfile, viewPagerProfile,
                (tab, position) -> tab.setText(pagerAdapter.getTabTitle(position)));
        tabMediator.attach();
    }

    private void loadProfile() {
        if (targetUsername == null || targetUsername.isEmpty()) return;

        userRepository.fetchUserProfile(targetUsername, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfileResponse response) {
                if (isFinishing()) return;
                if (response.getUser() != null) {
                    bindUserInfo(response.getUser());
                }
                pagerAdapter.setProfileData(response);
                if (tabMediator != null) {
                    for (int i = 0; i < tabLayoutProfile.getTabCount(); i++) {
                        TabLayout.Tab tab = tabLayoutProfile.getTabAt(i);
                        if (tab != null) {
                            tab.setText(pagerAdapter.getTabTitle(i));
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing()) return;
                Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUserInfo(User user) {
        tvProfileName.setText(user.getName());
        tvProfileUsername.setText(String.format("@%s", user.getUsername()));

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvProfileBio.setVisibility(View.VISIBLE);
            tvProfileBio.setText(user.getBio());
        } else {
            tvProfileBio.setVisibility(View.GONE);
        }

        // Academic details
        boolean hasAcademic = false;
        if (user.getUniversity() != null && !user.getUniversity().isEmpty()) {
            tvInstitution.setVisibility(View.VISIBLE);
            tvInstitution.setText(String.format("🏛️ %s", user.getUniversity()));
            hasAcademic = true;
        } else {
            tvInstitution.setVisibility(View.GONE);
        }

        if (user.getGraduationYear() != null && !user.getGraduationYear().isEmpty()) {
            tvDegreeAndYear.setVisibility(View.VISIBLE);
            tvDegreeAndYear.setText(String.format(Locale.getDefault(), "🎓 Class of %s", user.getGraduationYear()));
            hasAcademic = true;
        } else {
            tvDegreeAndYear.setVisibility(View.GONE);
        }

        if (user.getPhone() != null && !user.getPhone().isEmpty() && !user.isPhonePrivate()) {
            tvPhone.setVisibility(View.VISIBLE);
            tvPhone.setText(String.format("📞 %s", user.getPhone()));
            hasAcademic = true;
        } else {
            tvPhone.setVisibility(View.GONE);
        }

        cardAcademicInfo.setVisibility(hasAcademic ? View.VISIBLE : View.GONE);

        // Stats
        tvRankValue.setText(user.getGlobalRank() > 0 ? String.format(Locale.getDefault(), "#%d", user.getGlobalRank()) : "--");
        tvScoreValue.setText(String.format(Locale.US, "%.1f", user.getTotalPoints()));
        tvTierValue.setText(user.getTierBadge());

        // Cover photo
        if (user.getCoverPhotoUrl() != null && !user.getCoverPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getCoverPhotoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.bg_splash_gradient)
                    .error(R.drawable.bg_splash_gradient)
                    .into(imgCoverPhoto);
        } else {
            imgCoverPhoto.setImageResource(R.drawable.bg_splash_gradient);
        }

        // Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_app_logo);
        }
    }

    @Override
    protected void onDestroy() {
        if (tabMediator != null) {
            tabMediator.detach();
            tabMediator = null;
        }
        super.onDestroy();
    }
}
