package com.govtexam.prep.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.govtexam.prep.data.model.BannerItem;
import com.govtexam.prep.data.model.TaxonomyItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for GET /api/v1/home-feed.
 */
public class HomeFeedResponse implements Serializable {

    @SerializedName("banners")
    private List<BannerItem> banners = new ArrayList<>();

    @SerializedName("popular_searches")
    private List<String> popularSearches = new ArrayList<>();

    @SerializedName("topics")
    private List<TaxonomyItem> topics = new ArrayList<>();

    @SerializedName("subjects")
    private List<TaxonomyItem> subjects = new ArrayList<>();

    @SerializedName("categories")
    private List<TaxonomyItem> categories = new ArrayList<>();

    @SerializedName("tags")
    private List<TaxonomyItem> tags = new ArrayList<>();

    public List<BannerItem> getBanners() { return banners != null ? banners : new ArrayList<>(); }
    public void setBanners(List<BannerItem> banners) { this.banners = banners; }

    public List<String> getPopularSearches() { return popularSearches != null ? popularSearches : new ArrayList<>(); }
    public void setPopularSearches(List<String> popularSearches) { this.popularSearches = popularSearches; }

    public List<TaxonomyItem> getTopics() { return topics != null ? topics : new ArrayList<>(); }
    public void setTopics(List<TaxonomyItem> topics) { this.topics = topics; }

    public List<TaxonomyItem> getSubjects() { return subjects != null ? subjects : new ArrayList<>(); }
    public void setSubjects(List<TaxonomyItem> subjects) { this.subjects = subjects; }

    public List<TaxonomyItem> getCategories() { return categories != null ? categories : new ArrayList<>(); }
    public void setCategories(List<TaxonomyItem> categories) { this.categories = categories; }

    public List<TaxonomyItem> getTags() { return tags != null ? tags : new ArrayList<>(); }
    public void setTags(List<TaxonomyItem> tags) { this.tags = tags; }
}
