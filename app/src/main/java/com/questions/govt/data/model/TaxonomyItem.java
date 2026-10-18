package com.questions.govt.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents Subjects, Topics, Categories, or Tags in the taxonomy hierarchy.
 */
@Entity(tableName = "taxonomies", primaryKeys = {"id", "type"})
public class TaxonomyItem implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("color")
    private String color;

    @SerializedName("slug")
    private String slug;

    @NonNull
    @SerializedName("type")
    private String type = "subject"; // "subject", "topic", "category", "tag"

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    @SerializedName("questions_count")
    private int questionsCount;

    @SerializedName("description")
    private String description;

    @SerializedName("parent_id")
    private long parentId;

    public TaxonomyItem() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() {
        if (title != null && !title.isEmpty()) return title;
        return name != null ? name : "";
    }
    public void setTitle(String title) { this.title = title; }

    public String getName() { return name != null ? name : title; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color != null ? color : "#1E3A8A"; }
    public void setColor(String color) { this.color = color; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getThumbnailUrl() { return thumbnailUrl != null ? thumbnailUrl : iconUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public int getQuestionsCount() { return questionsCount; }
    public void setQuestionsCount(int questionsCount) { this.questionsCount = questionsCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getParentId() { return parentId; }
    public void setParentId(long parentId) { this.parentId = parentId; }
}
