package com.example.WalletSIOPM;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class CategoryStore {

    private static final String PREFERENCES_NAME = "AppSettings";
    private static final String CATEGORIES_KEY = "custom_subscription_categories";
    private static final String ADD_CATEGORY_OPTION = "➕ Add Custom Category...";
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "Music", "Movies & TV", "Software & Cloud", "Gaming",
            "Education & Books", "Sports & Fitness", "Other"
    );

    private CategoryStore() {
    }

    public static synchronized List<String> getCategories(Context context) {
        return getCategories(context, Collections.emptyList());
    }

    public static synchronized List<String> getCategories(Context context, List<String> additionalCategories) {
        List<String> categories = new ArrayList<>(DEFAULT_CATEGORIES);
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> savedCategories = preferences.getStringSet(CATEGORIES_KEY, Collections.emptySet());
        TreeSet<String> sortedSavedCategories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sortedSavedCategories.addAll(savedCategories);

        for (String category : sortedSavedCategories) {
            addIfMissing(categories, category);
        }
        if (additionalCategories != null) {
            for (String category : additionalCategories) {
                addIfMissing(categories, category);
            }
        }
        return categories;
    }

    public static synchronized void addCategory(Context context, String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }

        String normalizedCategory = category.trim();
        if (ADD_CATEGORY_OPTION.equals(normalizedCategory)) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> savedCategories = preferences.getStringSet(CATEGORIES_KEY, Collections.emptySet());
        Set<String> updatedCategories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        updatedCategories.addAll(savedCategories);
        updatedCategories.add(normalizedCategory);
        preferences.edit().putStringSet(CATEGORIES_KEY, updatedCategories).apply();
    }

    private static void addIfMissing(List<String> categories, String category) {
        if (category == null) {
            return;
        }
        String normalizedCategory = category.trim();
        if (normalizedCategory.isEmpty() || ADD_CATEGORY_OPTION.equals(normalizedCategory)) {
            return;
        }
        for (String existingCategory : categories) {
            if (existingCategory.equalsIgnoreCase(normalizedCategory)) {
                return;
            }
        }
        categories.add(normalizedCategory);
    }
}
