package com.questions.govt.ui.profile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.questions.govt.data.remote.response.UserProfileResponse;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ViewPager2 adapter for the 6 Facebook-style profile tabs.
 */
public class ProfilePagerAdapter extends FragmentStateAdapter {

    private UserProfileResponse profileData;
    private final android.util.SparseArray<ProfileTabContentFragment> activeFragments = new android.util.SparseArray<>();

    public ProfilePagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    public void setProfileData(UserProfileResponse data) {
        this.profileData = data;
        for (int i = 0; i < activeFragments.size(); i++) {
            int pos = activeFragments.keyAt(i);
            ProfileTabContentFragment fragment = activeFragments.valueAt(i);
            if (fragment != null) {
                fragment.updateData(getDataForPosition(pos));
            }
        }
        notifyDataSetChanged();
    }

    public Serializable getDataForPosition(int position) {
        if (profileData == null) return new ArrayList<>();
        switch (position) {
            case 0: return (Serializable) profileData.getMyExams();
            case 1: return (Serializable) profileData.getCreatedQuestions();
            case 2: return (Serializable) profileData.getCustomExams();
            case 3: return (Serializable) profileData.getStarredQuestions();
            case 4: return (Serializable) profileData.getReportedQuestions();
            case 5: return (Serializable) profileData.getMistakeNotebook();
            default: return new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Serializable dataList = getDataForPosition(position);
        ProfileTabContentFragment fragment = ProfileTabContentFragment.newInstance(position, dataList);
        activeFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    public String getTabTitle(int position) {
        if (profileData == null) {
            switch (position) {
                case 0: return "Exams";
                case 1: return "Created";
                case 2: return "Custom";
                case 3: return "Starred";
                case 4: return "Reported";
                case 5: return "Mistakes";
                default: return "";
            }
        }

        switch (position) {
            case 0: return "Exams (" + profileData.getMyExams().size() + ")";
            case 1: return "Created (" + profileData.getCreatedQuestions().size() + ")";
            case 2: return "Custom (" + profileData.getCustomExams().size() + ")";
            case 3: return "Starred (" + profileData.getStarredQuestions().size() + ")";
            case 4: return "Reported (" + profileData.getReportedQuestions().size() + ")";
            case 5: return "Mistakes (" + profileData.getMistakeNotebook().size() + ")";
            default: return "";
        }
    }
}
