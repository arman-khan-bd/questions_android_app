package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.questions.govt.data.model.TaxonomyItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for GET /api/v1/subjects/{id}.
 * Encapsulates the subject with its child topics, related exam categories, and trending tags.
 */
public class SubjectDetailsResponse implements Serializable {

    @SerializedName("subject")
    private TaxonomyItem subject;

    @SerializedName("topics")
    private List<TaxonomyItem> topics = new ArrayList<>();

    @SerializedName("categories")
    private List<TaxonomyItem> categories = new ArrayList<>();

    @SerializedName("tags")
    private List<TaxonomyItem> tags = new ArrayList<>();

    public TaxonomyItem getSubject() {
        return subject;
    }

    public void setSubject(TaxonomyItem subject) {
        this.subject = subject;
    }

    public List<TaxonomyItem> getTopics() {
        return topics != null ? topics : new ArrayList<>();
    }

    public void setTopics(List<TaxonomyItem> topics) {
        this.topics = topics;
    }

    public List<TaxonomyItem> getCategories() {
        return categories != null ? categories : new ArrayList<>();
    }

    public void setCategories(List<TaxonomyItem> categories) {
        this.categories = categories;
    }

    public List<TaxonomyItem> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    public void setTags(List<TaxonomyItem> tags) {
        this.tags = tags;
    }
}
