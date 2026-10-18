package com.questions.govt.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.questions.govt.R;
import com.questions.govt.data.model.BannerItem;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2 adapter for dynamic .webp hero carousel banners with deep-linking.
 */
public class BannerCarouselAdapter extends RecyclerView.Adapter<BannerCarouselAdapter.BannerViewHolder> {

    private final List<BannerItem> banners = new ArrayList<>();
    private final OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem banner);
    }

    public BannerCarouselAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setBanners(List<BannerItem> newBanners) {
        banners.clear();
        if (newBanners != null) {
            banners.addAll(newBanners);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem banner = banners.get(position);

        Glide.with(holder.itemView.getContext())
                .load(banner.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.bg_splash_gradient)
                .error(R.drawable.bg_splash_gradient)
                .into(holder.ivBanner);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;

        BannerViewHolder(View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
        }
    }
}
