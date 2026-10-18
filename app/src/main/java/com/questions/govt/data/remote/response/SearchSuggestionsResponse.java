package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions for live search dropdown typing (debounced 300ms).
 */
public class SearchSuggestionsResponse implements Serializable {

    @SerializedName("popular_searches")
    private List<String> popularSearches = new ArrayList<>();

    @SerializedName("tags")
    private List<String> tags = new ArrayList<>();

    public List<String> getPopularSearches() {
        return popularSearches != null ? popularSearches : new ArrayList<>();
    }

    public void setPopularSearches(List<String> popularSearches) {
        this.popularSearches = popularSearches;
    }

    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
