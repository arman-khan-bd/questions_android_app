package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard Laravel pagination envelope.
 */
public class PaginatedList<T> implements Serializable {

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("data")
    private List<T> data = new ArrayList<>();

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total")
    private int total;

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public List<T> getData() { return data != null ? data : new ArrayList<>(); }
    public void setData(List<T> data) { this.data = data; }

    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }

    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public boolean hasMorePages() {
        return currentPage < lastPage;
    }
}
