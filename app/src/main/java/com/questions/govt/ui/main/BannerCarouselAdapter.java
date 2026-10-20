package com.questions.govt.ui.main;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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

        // Title overlay visibility & position
        if (holder.tvBannerTitle != null) {
            if (banner.isShowTitle() && !TextUtils.isEmpty(banner.getTitle())) {
                holder.tvBannerTitle.setVisibility(View.VISIBLE);
                holder.tvBannerTitle.setText(banner.getTitle());

                ViewGroup.LayoutParams lp = holder.tvBannerTitle.getLayoutParams();
                if (lp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
                    params.gravity = getGravityFromPosition(banner.getTitlePosition());
                    holder.tvBannerTitle.setLayoutParams(params);
                }
            } else {
                holder.tvBannerTitle.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    private int getGravityFromPosition(String pos) {
        if (pos == null) return Gravity.BOTTOM | Gravity.START;
        switch (pos.toLowerCase().trim().replace('-', '_')) {
            case "bottom_center":
                return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            case "bottom_right":
                return Gravity.BOTTOM | Gravity.END;
            case "top_left":
                return Gravity.TOP | Gravity.START;
            case "top_center":
                return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            case "top_right":
                return Gravity.TOP | Gravity.END;
            case "center":
                return Gravity.CENTER;
            case "bottom_left":
            default:
                return Gravity.BOTTOM | Gravity.START;
        }
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;
        TextView tvBannerTitle;

        BannerViewHolder(View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);
        }
    }
}
