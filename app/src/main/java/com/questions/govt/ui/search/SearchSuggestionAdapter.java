package com.questions.govt.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.questions.govt.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for live search suggestions and top popular searches.
 */
public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    private final List<String> suggestions = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String query);
    }

    public SearchSuggestionAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<String> newSuggestions) {
        suggestions.clear();
        if (newSuggestions != null) {
            suggestions.addAll(newSuggestions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = suggestions.get(position);
        holder.tvSuggestionText.setText(query);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(query);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSuggestionText;

        ViewHolder(View itemView) {
            super(itemView);
            tvSuggestionText = itemView.findViewById(R.id.tvSuggestionText);
        }
    }
}
