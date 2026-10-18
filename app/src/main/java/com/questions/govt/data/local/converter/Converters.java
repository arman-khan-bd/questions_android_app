package com.questions.govt.data.local.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Type converters for Room database to handle lists and JSON fields.
 */
public class Converters {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromLongList(List<Long> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Long> toLongList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type listType = new TypeToken<List<Long>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromDoubleList(List<Double> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Double> toDoubleList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type listType = new TypeToken<List<Double>>() {}.getType();
        return gson.fromJson(value, listType);
    }
}
