package com.govtexam.prep.ui.profile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.govtexam.prep.data.remote.response.UserProfileResponse;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * ViewPager2 adapter for the 6 Facebook-style profile tabs.
 */
public class ProfilePagerAdapter extends FragmentStateAdapter {

    private UserProfileResponse profileData;

    public ProfilePagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    public void setProfileData(UserProfileResponse data) {
        this.profileData = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Serializable dataList = new ArrayList<>();
        if (profileData != null) {
            switch (position) {
                case 0: dataList = (Serializable) profileData.getMyExams(); break;
                case 1: dataList = (Serializable) profileData.getCreatedQuestions(); break;
                case 2: dataList = (Serializable) profileData.getCustomExams(); break;
                case 3: dataList = (Serializable) profileData.getStarredQuestions(); break;
                case 4: dataList = (Serializable) profileData.getReportedQuestions(); break;
                case 5: dataList = (Serializable) profileData.getMistakeNotebook(); break;
            }
        }
        return ProfileTabContentFragment.newInstance(position, dataList);
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
