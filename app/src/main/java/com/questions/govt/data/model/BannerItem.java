package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Promotional hero banner item rendered on HomeFragment ViewPager2 carousel.
 */
public class BannerItem implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("image_url")
    private String imageUrl; // .webp format

    @SerializedName("title")
    private String title;

    @SerializedName("target_type")
    private String targetType; // "exam", "subject", "category", "url"

    @SerializedName("target_id")
    private long targetId;

    @SerializedName("target_url")
    private String targetUrl;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getTargetType() { return targetType != null ? targetType : "none"; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public long getTargetId() { return targetId; }
    public void setTargetId(long targetId) { this.targetId = targetId; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
}
