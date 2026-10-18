package com.questions.govt.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.questions.govt.R;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.ReportItem;
import com.questions.govt.ui.question.QuestionCardAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment rendering the content for one of the 6 Facebook-style profile tabs:
 * 0: My Exams, 1: Created Questions, 2: Custom Exams, 3: Starred Questions,
 * 4: Reported Questions, 5: Mistake Notebook.
 */
public class ProfileTabContentFragment extends Fragment {

    public static final String ARG_TAB_INDEX = "tab_index";
    public static final String ARG_DATA_LIST = "data_list";

    private int tabIndex = 0;
    private Serializable dataList;

    private RecyclerView rvTabContent;
    private LinearLayout layoutTabEmpty;
    private ImageView imgTabEmpty;
    private TextView tvTabEmptyTitle;
    private TextView tvTabEmptySubtitle;

    public static ProfileTabContentFragment newInstance(int tabIndex, Serializable dataList) {
        ProfileTabContentFragment fragment = new ProfileTabContentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_INDEX, tabIndex);
        args.putSerializable(ARG_DATA_LIST, dataList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabIndex = getArguments().getInt(ARG_TAB_INDEX, 0);
            dataList = getArguments().getSerializable(ARG_DATA_LIST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile_tab_list, container, false);
        rvTabContent = root.findViewById(R.id.rvTabContent);
        layoutTabEmpty = root.findViewById(R.id.layoutTabEmpty);
        imgTabEmpty = root.findViewById(R.id.imgTabEmpty);
        tvTabEmptyTitle = root.findViewById(R.id.tvTabEmptyTitle);
        tvTabEmptySubtitle = root.findViewById(R.id.tvTabEmptySubtitle);

        rvTabContent.setLayoutManager(new LinearLayoutManager(requireContext()));
        bindTabAdapter();
        return root;
    }

    @SuppressWarnings("unchecked")
    private void bindTabAdapter() {
        switch (tabIndex) {
            case 0: { // My Exams
                List<ExamResult> exams = dataList instanceof List ? (List<ExamResult>) dataList : new ArrayList<>();
                if (exams.isEmpty()) {
                    showEmpty("No Exams Attended Yet", "Take model tests or custom exams to build your history.", R.drawable.ic_trophy);
                } else {
                    hideEmpty();
                    ProfileExamAdapter adapter = new ProfileExamAdapter();
                    adapter.submitList(exams);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
            case 1: { // Created Questions
                List<Question> questions = dataList instanceof List ? (List<Question>) dataList : new ArrayList<>();
                if (questions.isEmpty()) {
                    showEmpty("No Questions Created", "Submit questions to contribute to the community.", R.drawable.ic_edit);
                } else {
                    hideEmpty();
                    QuestionCardAdapter adapter = new QuestionCardAdapter(requireContext(), getChildFragmentManager());
                    adapter.submitList(questions);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
            case 2: { // Custom Exams
                List<CustomExamRequest> customExams = dataList instanceof List ? (List<CustomExamRequest>) dataList : new ArrayList<>();
                if (customExams.isEmpty()) {
                    showEmpty("No Custom Exams Saved", "Build your customized exam presets on the Home tab.", R.drawable.ic_nav_plus);
                } else {
                    hideEmpty();
                    ProfileCustomExamAdapter adapter = new ProfileCustomExamAdapter();
                    adapter.submitList(customExams);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
            case 3: { // Starred Questions
                List<Question> starred = dataList instanceof List ? (List<Question>) dataList : new ArrayList<>();
                if (starred.isEmpty()) {
                    showEmpty("No Starred Questions", "Bookmark tricky questions to quickly review them later.", R.drawable.ic_star);
                } else {
                    hideEmpty();
                    QuestionCardAdapter adapter = new QuestionCardAdapter(requireContext(), getChildFragmentManager());
                    adapter.submitList(starred);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
            case 4: { // Reported Questions
                List<ReportItem> reports = dataList instanceof List ? (List<ReportItem>) dataList : new ArrayList<>();
                if (reports.isEmpty()) {
                    showEmpty("No Reports Filed", "Reports you file against questionable items appear here.", R.drawable.ic_flag);
                } else {
                    hideEmpty();
                    ProfileReportedAdapter adapter = new ProfileReportedAdapter();
                    adapter.submitList(reports);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
            case 5: { // Mistake Notebook
                List<Question> mistakes = dataList instanceof List ? (List<Question>) dataList : new ArrayList<>();
                if (mistakes.isEmpty()) {
                    showEmpty("Mistake Notebook Clean!", "Any questions answered incorrectly in exams will appear here for practice.", R.drawable.ic_trophy);
                } else {
                    hideEmpty();
                    QuestionCardAdapter adapter = new QuestionCardAdapter(requireContext(), getChildFragmentManager());
                    adapter.submitList(mistakes);
                    rvTabContent.setAdapter(adapter);
                }
                break;
            }
        }
    }

    private void showEmpty(String title, String subtitle, int iconRes) {
        layoutTabEmpty.setVisibility(View.VISIBLE);
        rvTabContent.setVisibility(View.GONE);
        tvTabEmptyTitle.setText(title);
        tvTabEmptySubtitle.setText(subtitle);
        imgTabEmpty.setImageResource(iconRes);
    }

    private void hideEmpty() {
        layoutTabEmpty.setVisibility(View.GONE);
        rvTabContent.setVisibility(View.VISIBLE);
    }
}
