package com.questions.govt.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
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
import com.questions.govt.ui.question.CreateQuestionActivity;

import java.util.Locale;

/**
 * Facebook-style student profile fragment embedded into main navigation:
 * - 16:9 Cover Photo & overlapping circle avatar
 * - Personal bio, academic details card
 * - Global rank, points, and tier chip
 * - 6 ViewPager2 tabs: Exams, Created, Custom Tests, Starred, Reported, Mistake Notebook
 */
public class ProfileFragment extends Fragment {

    private ImageView imgCoverPhoto;
    private ImageView imgAvatar;
    private MaterialButton btnEditProfile;
    private MaterialButton btnProfileSettings;
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
    private SwipeRefreshLayout swipeRefreshProfile;

    private ProfilePagerAdapter pagerAdapter;
    private UserRepository userRepository;
    private TabLayoutMediator tabMediator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        userRepository = UserRepository.getInstance(requireContext());
        initViews(root);
        setupViewPager();
        loadProfileData();
        return root;
    }

    private void initViews(View root) {
        imgCoverPhoto = root.findViewById(R.id.imgCoverPhoto);
        imgAvatar = root.findViewById(R.id.imgAvatar);
        btnEditProfile = root.findViewById(R.id.btnEditProfile);
        tvProfileName = root.findViewById(R.id.tvProfileName);
        tvProfileUsername = root.findViewById(R.id.tvProfileUsername);
        tvProfileBio = root.findViewById(R.id.tvProfileBio);

        cardAcademicInfo = root.findViewById(R.id.cardAcademicInfo);
        tvInstitution = root.findViewById(R.id.tvInstitution);
        tvDegreeAndYear = root.findViewById(R.id.tvDegreeAndYear);
        tvPhone = root.findViewById(R.id.tvPhone);

        tvRankValue = root.findViewById(R.id.tvRankValue);
        tvScoreValue = root.findViewById(R.id.tvScoreValue);
        tvTierValue = root.findViewById(R.id.tvTierValue);

        toolbarProfile = root.findViewById(R.id.toolbarProfile);
        tabLayoutProfile = root.findViewById(R.id.tabLayoutProfile);
        viewPagerProfile = root.findViewById(R.id.viewPagerProfile);
        swipeRefreshProfile = root.findViewById(R.id.swipeRefreshProfile);

        // Hide back button in fragment context
        toolbarProfile.setNavigationIcon(null);
        toolbarProfile.setTitle("");

        // Collapsible Header: Hide "Profile" title when unscrolled; animate/show in toolbar when scrolled down
        AppBarLayout appBarLayout = root.findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
                int totalRange = appBar.getTotalScrollRange();
                if (totalRange == 0) return;
                float scrollPercentage = (float) Math.abs(verticalOffset) / totalRange;
                if (scrollPercentage > 0.65f) {
                    toolbarProfile.setTitle(getString(R.string.profile_title));
                } else {
                    toolbarProfile.setTitle("");
                }
            });
        }

        if (swipeRefreshProfile != null) {
            swipeRefreshProfile.setColorSchemeResources(R.color.primary, R.color.secondary);
            swipeRefreshProfile.setOnRefreshListener(this::loadProfileData);
        }

        btnProfileSettings = root.findViewById(R.id.btnProfileSettings);
        if (btnProfileSettings != null) {
            btnProfileSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UserSettingsActivity.class);
                startActivity(intent);
            });
        }

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupViewPager() {
        pagerAdapter = new ProfilePagerAdapter(getChildFragmentManager(), getLifecycle());
        viewPagerProfile.setAdapter(pagerAdapter);

        tabMediator = new TabLayoutMediator(tabLayoutProfile, viewPagerProfile,
                (tab, position) -> tab.setText(pagerAdapter.getTabTitle(position)));
        tabMediator.attach();
    }

    private void loadProfileData() {
        User loggedInUser = SessionManager.getInstance().getUser();
        if (loggedInUser == null) {
            if (swipeRefreshProfile != null) swipeRefreshProfile.setRefreshing(false);
            return;
        }

        // Populate initial data from local session
        bindUserInfo(loggedInUser);

        // Fetch full profile (all 6 tabs) from server
        String username = loggedInUser.getUsername() != null ? loggedInUser.getUsername() : "";
        userRepository.fetchUserProfile(username, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfileResponse response) {
                if (swipeRefreshProfile != null) {
                    swipeRefreshProfile.setRefreshing(false);
                }
                if (!isAdded()) return;
                if (response.getUser() != null) {
                    bindUserInfo(response.getUser());
                }
                pagerAdapter.setProfileData(response);
                // Refresh tab titles with counts
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
                if (swipeRefreshProfile != null) {
                    swipeRefreshProfile.setRefreshing(false);
                }
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUserInfo(User user) {
        Context context = requireContext();

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
            String uni = user.getUniversity().replaceAll("^[🏛️🎓📞\\s]+", "");
            tvInstitution.setText(uni);
            hasAcademic = true;
        } else {
            tvInstitution.setVisibility(View.GONE);
        }

        if (user.getGraduationYear() != null && !user.getGraduationYear().isEmpty()) {
            tvDegreeAndYear.setVisibility(View.VISIBLE);
            String year = user.getGraduationYear().replaceAll("^[🏛️🎓📞\\s]+", "");
            tvDegreeAndYear.setText(String.format(Locale.getDefault(), "Class of %s", year));
            hasAcademic = true;
        } else {
            tvDegreeAndYear.setVisibility(View.GONE);
        }

        if (user.getPhone() != null && !user.getPhone().isEmpty() && !user.isPhonePrivate()) {
            tvPhone.setVisibility(View.VISIBLE);
            String phone = user.getPhone().replaceAll("^[🏛️🎓📞\\s]+", "");
            tvPhone.setText(phone);
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
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    @Override
    public void onDestroyView() {
        if (tabMediator != null) {
            tabMediator.detach();
            tabMediator = null;
        }
        super.onDestroyView();
    }
}
