package com.govtexam.prep.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.govtexam.prep.R;
import com.govtexam.prep.data.model.TaxonomyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for taxonomy items displayed in the left navigation drawer.
 */
public class DrawerTaxonomyAdapter extends RecyclerView.Adapter<DrawerTaxonomyAdapter.ViewHolder> {

    private final List<TaxonomyItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TaxonomyItem item);
    }

    public DrawerTaxonomyAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<TaxonomyItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawer_taxonomy, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaxonomyItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvCount.setText(String.valueOf(item.getQuestionsCount()));

        Glide.with(holder.itemView.getContext())
                .load(item.getIconUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(holder.ivIcon);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvCount;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivDrawerItemIcon);
            tvTitle = itemView.findViewById(R.id.tvDrawerItemTitle);
            tvCount = itemView.findViewById(R.id.tvDrawerItemCount);
        }
    }
}
