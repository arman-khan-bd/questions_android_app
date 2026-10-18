package com.questions.govt.ui.question;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.questions.govt.R;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DialogFragment for reporting questions directly to the Admin Report Manager & Creator profile.
 */
public class ReportDialogFragment extends DialogFragment {

    public static final String ARG_QUESTION_ID = "arg_question_id";
    private long questionId;

    public static ReportDialogFragment newInstance(long questionId) {
        ReportDialogFragment fragment = new ReportDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_QUESTION_ID, questionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            questionId = getArguments().getLong(ARG_QUESTION_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_question, null);

        RadioGroup rgReportReasons = view.findViewById(R.id.rgReportReasons);
        EditText etReportNote = view.findViewById(R.id.etReportNote);
        Button btnCancel = view.findViewById(R.id.btnCancelReport);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReport);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            int selectedId = rgReportReasons.getCheckedRadioButtonId();
            RadioButton selectedRb = view.findViewById(selectedId);
            String reason = selectedRb != null ? selectedRb.getText().toString() : "Wrong Answer Key";
            String note = etReportNote.getText() != null ? etReportNote.getText().toString().trim() : "";

            btnSubmit.setEnabled(false);

            Map<String, String> body = new HashMap<>();
            body.put("reason", reason);
            body.put("note", note);

            ApiClient.getApiService().reportQuestion(questionId, body).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (isAdded()) {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Thank you! Your report has been submitted for review.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    if (isAdded()) {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        return dialog;
    }
}
