package com.questions.govt.ui.notification;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.questions.govt.R;
import com.questions.govt.data.local.entity.NotificationEntity;

import java.util.Objects;

/**
 * Adapter for displaying student notifications with actor avatars and event badges.
 */
public class NotificationAdapter extends ListAdapter<NotificationEntity, NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationEntity notification);
    }

    private final OnNotificationClickListener clickListener;

    public NotificationAdapter(OnNotificationClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<NotificationEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NotificationEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotificationEntity oldItem, @NonNull NotificationEntity newItem) {
                    if (oldItem.getId() > 0 && newItem.getId() > 0) {
                        return oldItem.getId() == newItem.getId();
                    }
                    return oldItem.getLocalId() == newItem.getLocalId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull NotificationEntity oldItem, @NonNull NotificationEntity newItem) {
                    return oldItem.isRead() == newItem.isRead() &&
                            Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                            Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                            Objects.equals(oldItem.getType(), newItem.getType());
                }
            };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationEntity item = getItem(position);
        holder.bind(item, clickListener);
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardNotification;
        private final ImageView imgActorAvatar;
        private final ImageView imgTypeBadge;
        private final TextView tvNotificationTitle;
        private final TextView tvNotificationMessage;
        private final TextView tvNotificationTime;
        private final View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            imgActorAvatar = itemView.findViewById(R.id.imgActorAvatar);
            imgTypeBadge = itemView.findViewById(R.id.imgTypeBadge);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        public void bind(NotificationEntity item, OnNotificationClickListener listener) {
            Context context = itemView.getContext();

            tvNotificationTitle.setText(item.getTitle());
            tvNotificationMessage.setText(item.getMessage());
            tvNotificationTime.setText(item.getCreatedAt());

            // Unread dot & background differentiation
            if (!item.isRead()) {
                viewUnreadDot.setVisibility(View.VISIBLE);
                cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_surface));
            } else {
                viewUnreadDot.setVisibility(View.GONE);
                cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
            }

            // Actor avatar
            if (item.getActorAvatarUrl() != null && !item.getActorAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getActorAvatarUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .placeholder(R.drawable.ic_app_logo)
                        .error(R.drawable.ic_app_logo)
                        .into(imgActorAvatar);
            } else {
                imgActorAvatar.setImageResource(R.drawable.ic_app_logo);
            }

            // Type badge styling
            configureTypeBadge(item.getType(), context);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }

        private void configureTypeBadge(String type, Context context) {
            if (type == null) type = "";

            switch (type.toLowerCase()) {
                case "star":
                    imgTypeBadge.setImageResource(R.drawable.ic_star);
                    imgTypeBadge.setColorFilter(ContextCompat.getColor(context, R.color.gold));
                    break;
                case "comment":
                case "reply":
                    imgTypeBadge.setImageResource(R.drawable.ic_comment);
                    imgTypeBadge.setColorFilter(ContextCompat.getColor(context, R.color.primary_light));
                    break;
                case "report":
                    imgTypeBadge.setImageResource(R.drawable.ic_flag);
                    imgTypeBadge.setColorFilter(ContextCompat.getColor(context, R.color.error));
                    break;
                case "answer_click":
                    imgTypeBadge.setImageResource(R.drawable.ic_visibility);
                    imgTypeBadge.setColorFilter(ContextCompat.getColor(context, R.color.info));
                    break;
                case "admin_broadcast":
                default:
                    imgTypeBadge.setImageResource(R.drawable.ic_notifications);
                    imgTypeBadge.setColorFilter(ContextCompat.getColor(context, R.color.secondary));
                    break;
            }
        }
    }
}
