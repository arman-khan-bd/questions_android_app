package com.questions.govt.ui.leaderboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.questions.govt.R;
import com.questions.govt.data.model.LeaderboardEntry;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.LeaderboardResponse;
import com.questions.govt.ui.profile.UserProfileActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Global Leaderboard Fragment featuring:
 * - Dynamic difficulty weight modes (Easy, Medium, Hard, Custom, Creators)
 * - Timeframe filtering (Today, Weekly, All Time)
 * - Top-3 Podium Card (Gold 1st, Silver 2nd, Bronze 3rd)
 * - Sticky User Standing Card
 * - Interactive profile deep-linking
 */
public class LeaderboardFragment extends Fragment {

    private TabLayout tabLayoutLeaderboard;
    private ChipGroup chipGroupTimeframe;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialCardView cardPodium;
    private LinearLayout layoutRank1, layoutRank2, layoutRank3;
    private ImageView imgPodiumAvatar1, imgPodiumAvatar2, imgPodiumAvatar3;
    private TextView tvPodiumName1, tvPodiumName2, tvPodiumName3;
    private TextView tvPodiumScore1, tvPodiumScore2, tvPodiumScore3;

    private MaterialCardView cardCurrentUserStanding;
    private TextView tvMyRank, tvMyTier, tvMyScore;
    private ImageView imgMyAvatar;

    private RecyclerView rvLeaderboard;
    private LinearLayout layoutLeaderboardEmpty;
    private ProgressBar progressBar;

    private LeaderboardAdapter adapter;
    private String currentTier = "all";
    private String currentTimeframe = "weekly";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        initViews(root);
        setupTabs();
        setupChips();
        setupRecyclerView();
        fetchLeaderboard();
        return root;
    }

    private void initViews(View root) {
        tabLayoutLeaderboard = root.findViewById(R.id.tabLayoutLeaderboard);
        chipGroupTimeframe = root.findViewById(R.id.chipGroupTimeframe);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        cardPodium = root.findViewById(R.id.cardPodium);

        layoutRank1 = root.findViewById(R.id.layoutRank1);
        layoutRank2 = root.findViewById(R.id.layoutRank2);
        layoutRank3 = root.findViewById(R.id.layoutRank3);

        imgPodiumAvatar1 = root.findViewById(R.id.imgPodiumAvatar1);
        imgPodiumAvatar2 = root.findViewById(R.id.imgPodiumAvatar2);
        imgPodiumAvatar3 = root.findViewById(R.id.imgPodiumAvatar3);

        tvPodiumName1 = root.findViewById(R.id.tvPodiumName1);
        tvPodiumName2 = root.findViewById(R.id.tvPodiumName2);
        tvPodiumName3 = root.findViewById(R.id.tvPodiumName3);

        tvPodiumScore1 = root.findViewById(R.id.tvPodiumScore1);
        tvPodiumScore2 = root.findViewById(R.id.tvPodiumScore2);
        tvPodiumScore3 = root.findViewById(R.id.tvPodiumScore3);

        cardCurrentUserStanding = root.findViewById(R.id.cardCurrentUserStanding);
        tvMyRank = root.findViewById(R.id.tvMyRank);
        tvMyTier = root.findViewById(R.id.tvMyTier);
        tvMyScore = root.findViewById(R.id.tvMyScore);
        imgMyAvatar = root.findViewById(R.id.imgMyAvatar);

        rvLeaderboard = root.findViewById(R.id.rvLeaderboard);
        layoutLeaderboardEmpty = root.findViewById(R.id.layoutLeaderboardEmpty);
        progressBar = root.findViewById(R.id.progressBar);

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefreshLayout.setOnRefreshListener(this::fetchLeaderboard);
    }

    private void setupTabs() {
        tabLayoutLeaderboard.addTab(tabLayoutLeaderboard.newTab().setText("All"));
        tabLayoutLeaderboard.addTab(tabLayoutLeaderboard.newTab().setText(R.string.tab_medium));
        tabLayoutLeaderboard.addTab(tabLayoutLeaderboard.newTab().setText(R.string.tab_hard));
        tabLayoutLeaderboard.addTab(tabLayoutLeaderboard.newTab().setText(R.string.tab_easy));

        tabLayoutLeaderboard.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTier = "all"; break;
                    case 1: currentTier = "medium"; break;
                    case 2: currentTier = "hard"; break;
                    case 3: currentTier = "easy"; break;
                }
                fetchLeaderboard();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupChips() {
        chipGroupTimeframe.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) {
                currentTimeframe = "today";
            } else if (checkedId == R.id.chipAllTime) {
                currentTimeframe = "all_time";
            } else {
                currentTimeframe = "weekly";
            }
            fetchLeaderboard();
        });
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(this::openUserProfile);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(adapter);
    }

    private void fetchLeaderboard() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getLeaderboard(currentTier, 50)
                .enqueue(new Callback<ApiResponse<LeaderboardResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<LeaderboardResponse>> call, Response<ApiResponse<LeaderboardResponse>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            bindData(response.body().getData());
                        } else {
                            layoutLeaderboardEmpty.setVisibility(View.VISIBLE);
                            cardPodium.setVisibility(View.GONE);
                            adapter.submitList(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LeaderboardResponse>> call, Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindData(LeaderboardResponse data) {
        List<LeaderboardEntry> podium = data.getTopPodium();
        List<LeaderboardEntry> allList = data.getLeaderboard();

        if ((podium == null || podium.isEmpty()) && (allList == null || allList.isEmpty())) {
            layoutLeaderboardEmpty.setVisibility(View.VISIBLE);
            cardPodium.setVisibility(View.GONE);
            cardCurrentUserStanding.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
            return;
        }

        layoutLeaderboardEmpty.setVisibility(View.GONE);

        // Bind Top 3 Podium
        if (podium != null && !podium.isEmpty()) {
            cardPodium.setVisibility(View.VISIBLE);
            bindPodium(podium);
        } else if (allList != null && allList.size() >= 3) {
            cardPodium.setVisibility(View.VISIBLE);
            bindPodium(allList.subList(0, Math.min(3, allList.size())));
        } else {
            cardPodium.setVisibility(View.GONE);
        }

        // Bind Current User Standing
        LeaderboardEntry myEntry = data.getCurrentUserEntry();
        if (myEntry != null && myEntry.getRank() > 0) {
            cardCurrentUserStanding.setVisibility(View.VISIBLE);
            tvMyRank.setText(String.format(Locale.getDefault(), "#%d", myEntry.getRank()));
            tvMyTier.setText(String.format("%s • %d exams", myEntry.getTierBadge(), myEntry.getExamsAttendedCount()));
            tvMyScore.setText(String.format(Locale.US, "%.1f pts", myEntry.getScore()));

            if (myEntry.getAvatarUrl() != null && !myEntry.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(myEntry.getAvatarUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .placeholder(R.drawable.ic_app_logo)
                        .into(imgMyAvatar);
            }
        } else {
            cardCurrentUserStanding.setVisibility(View.GONE);
        }

        // Bind RecyclerView (Entries rank 4 and beyond, or full list if no separate podium)
        List<LeaderboardEntry> recyclerEntries = new ArrayList<>();
        if (allList != null) {
            if (podium != null && !podium.isEmpty() && allList.size() > 3) {
                recyclerEntries.addAll(allList.subList(3, allList.size()));
            } else {
                recyclerEntries.addAll(allList);
            }
        }
        adapter.submitList(recyclerEntries);
    }

    private void bindPodium(List<LeaderboardEntry> list) {
        // Rank 1
        if (list.size() > 0) {
            LeaderboardEntry rank1 = list.get(0);
            layoutRank1.setVisibility(View.VISIBLE);
            tvPodiumName1.setText(rank1.getName());
            tvPodiumScore1.setText(String.format(Locale.US, "%.1f pts", rank1.getScore()));
            loadAvatar(rank1.getAvatarUrl(), imgPodiumAvatar1);
            layoutRank1.setOnClickListener(v -> openUserProfile(rank1));
        } else {
            layoutRank1.setVisibility(View.INVISIBLE);
        }

        // Rank 2
        if (list.size() > 1) {
            LeaderboardEntry rank2 = list.get(1);
            layoutRank2.setVisibility(View.VISIBLE);
            tvPodiumName2.setText(rank2.getName());
            tvPodiumScore2.setText(String.format(Locale.US, "%.1f pts", rank2.getScore()));
            loadAvatar(rank2.getAvatarUrl(), imgPodiumAvatar2);
            layoutRank2.setOnClickListener(v -> openUserProfile(rank2));
        } else {
            layoutRank2.setVisibility(View.INVISIBLE);
        }

        // Rank 3
        if (list.size() > 2) {
            LeaderboardEntry rank3 = list.get(2);
            layoutRank3.setVisibility(View.VISIBLE);
            tvPodiumName3.setText(rank3.getName());
            tvPodiumScore3.setText(String.format(Locale.US, "%.1f pts", rank3.getScore()));
            loadAvatar(rank3.getAvatarUrl(), imgPodiumAvatar3);
            layoutRank3.setOnClickListener(v -> openUserProfile(rank3));
        } else {
            layoutRank3.setVisibility(View.INVISIBLE);
        }
    }

    private void loadAvatar(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_app_logo);
        }
    }

    private void openUserProfile(LeaderboardEntry entry) {
        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra("username", entry.getUsername());
        startActivity(intent);
    }
}
