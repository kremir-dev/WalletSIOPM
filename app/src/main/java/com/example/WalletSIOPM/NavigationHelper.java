package com.example.WalletSIOPM;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public final class NavigationHelper {

    private NavigationHelper() {
    }

    public static void setup(Activity activity, int selectedItemId) {
        View navParent = activity.findViewById(R.id.bottomNavigationView);
        if (navParent == null) {
            return;
        }

        int[] navIds = {
                R.id.nav_subscriptions,
                R.id.nav_income,
                R.id.nav_expenses,
                R.id.nav_analytics,
                R.id.nav_settings
        };

        for (int navId : navIds) {
            View navItem = activity.findViewById(navId);
            if (navItem == null) {
                continue;
            }

            boolean selected = navId == selectedItemId;
            applySelectionStyle(activity, navItem, selected);
            navItem.setOnClickListener(v -> {
                if (v.getId() == selectedItemId) {
                    return;
                }
                navigate(activity, v.getId());
            });
        }
    }

    private static void applySelectionStyle(Activity activity, View navItem, boolean selected) {
        navItem.setSelected(selected);
        navItem.setActivated(selected);

        if (navItem instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) navItem;
            int selectedColor = Color.parseColor("#4F46E5");
            int defaultBg = ContextCompat.getColor(activity, R.color.bg_card);
            int defaultText = ContextCompat.getColor(activity, R.color.text_primary);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    selected ? selectedColor : defaultBg
            ));
            button.setTextColor(selected ? Color.WHITE : defaultText);
            button.setAlpha(selected ? 1f : 0.8f);
        }
    }

    private static void navigate(Activity activity, int targetId) {
        Class<?> targetActivity = null;
        if (targetId == R.id.nav_subscriptions) {
            targetActivity = MainActivity.class;
        } else if (targetId == R.id.nav_income) {
            targetActivity = IncomeActivity.class;
        } else if (targetId == R.id.nav_expenses) {
            targetActivity = ExpenseActivity.class;
        } else if (targetId == R.id.nav_analytics) {
            targetActivity = AnalyticsActivity.class;
        } else if (targetId == R.id.nav_settings) {
            targetActivity = SettingsActivity.class;
        }

        if (targetActivity == null) {
            return;
        }

        Intent intent = new Intent(activity, targetActivity);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        activity.finish();
    }
}
