package com.questions.govt.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.questions.govt.R;
import com.questions.govt.data.model.ReportItem;
import com.questions.govt.ui.search.SearchResultActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for student's reported questions tab.
 */
public class ProfileReportedAdapter extends RecyclerView.Adapter<ProfileReportedAdapter.ReportedViewHolder> {

    private final List<ReportItem> items = new ArrayList<>();

    public void submitList(List<ReportItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_reported, parent, false);
        return new ReportedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportedViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ReportedViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvReportedQuestionText;
        private final TextView tvReportStatusBadge;
        private final TextView tvReportReason;
        private final TextView tvReportNote;
        private final TextView tvReportDate;
        private final MaterialButton btnFixReportedQuestion;

        public ReportedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportedQuestionText = itemView.findViewById(R.id.tvReportedQuestionText);
            tvReportStatusBadge = itemView.findViewById(R.id.tvReportStatusBadge);
            tvReportReason = itemView.findViewById(R.id.tvReportReason);
            tvReportNote = itemView.findViewById(R.id.tvReportNote);
            tvReportDate = itemView.findViewById(R.id.tvReportDate);
            btnFixReportedQuestion = itemView.findViewById(R.id.btnFixReportedQuestion);
        }

        public void bind(ReportItem item) {
            Context context = itemView.getContext();

            tvReportedQuestionText.setText(item.getQuestionText().isEmpty() ?
                    ("Question #" + item.getQuestionId()) : item.getQuestionText());
            tvReportReason.setText("Reason: " + item.getReason());

            if (item.getNote() != null && !item.getNote().isEmpty()) {
                tvReportNote.setVisibility(View.VISIBLE);
                tvReportNote.setText("Note: " + item.getNote());
            } else {
                tvReportNote.setVisibility(View.GONE);
            }

            tvReportDate.setText(item.getCreatedAt());

            String status = item.getStatus().toUpperCase();
            tvReportStatusBadge.setText(status);
            if ("RESOLVED".equalsIgnoreCase(status)) {
                tvReportStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                tvReportStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.success_light));
            } else if ("DISMISSED".equalsIgnoreCase(status)) {
                tvReportStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                tvReportStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant));
            } else {
                tvReportStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning));
                tvReportStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.warning_light));
            }

            btnFixReportedQuestion.setOnClickListener(v -> {
                Intent intent = new Intent(context, SearchResultActivity.class);
                intent.putExtra("type", "question_id");
                intent.putExtra("id", item.getQuestionId());
                intent.putExtra("title", "Question #" + item.getQuestionId());
                context.startActivity(intent);
            });
        }
    }
}
