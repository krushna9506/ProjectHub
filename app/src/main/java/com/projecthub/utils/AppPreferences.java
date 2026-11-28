package com.projecthub.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Stores and applies app-wide theme, notification, and reminder preferences.
 */
public final class AppPreferences {

    private static final String PREFS_NAME = "projecthub_app_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_SYSTEM_NOTIFICATIONS = "system_notifications_enabled";
    private static final String KEY_DEADLINE_REMINDERS = "deadline_reminders_enabled";

    public static final String DEADLINE_WORK_NAME = "projecthub_deadline_alerts";
    public static final String THEME_SYSTEM = "SYSTEM";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";

    private AppPreferences() {
    }

    /** Normalizes any raw theme string to LIGHT, DARK, or SYSTEM. */
    public static String normalizeThemeMode(String raw) {
        if (raw == null) return THEME_SYSTEM;
        String normalized = raw.trim().toUpperCase(Locale.US);
        if (THEME_LIGHT.equals(normalized)) return THEME_LIGHT;
        if (THEME_DARK.equals(normalized)) return THEME_DARK;
        return THEME_SYSTEM;
    }

    /** Maps a normalized theme preference to AppCompatDelegate night mode constants. */
    public static int toAppCompatNightMode(String mode) {
        String normalized = normalizeThemeMode(mode);
        if (THEME_LIGHT.equals(normalized)) return AppCompatDelegate.MODE_NIGHT_NO;
        if (THEME_DARK.equals(normalized)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /** Maps a normalized theme preference to UiModeManager night mode constants (API 31+). */
    public static int toUiModeNightMode(String mode) {
        String normalized = normalizeThemeMode(mode);
        if (THEME_LIGHT.equals(normalized)) return UiModeManager.MODE_NIGHT_NO;
        if (THEME_DARK.equals(normalized)) return UiModeManager.MODE_NIGHT_YES;
        return UiModeManager.MODE_NIGHT_AUTO;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getThemeMode(Context context) {
        if (context == null) return THEME_SYSTEM;
        return normalizeThemeMode(prefs(context).getString(KEY_THEME_MODE, THEME_SYSTEM));
    }

    /** Persists the selected theme synchronously before applying it to prevent stale reloads. */
    public static void setThemeMode(Context context, String mode) {
        if (context == null) return;
        String normalized = normalizeThemeMode(mode);
        prefs(context).edit().putString(KEY_THEME_MODE, normalized).commit();
        applyTheme(context, normalized);
    }

    public static void applySavedTheme(Context context) {
        applyTheme(context, getThemeMode(context));
    }

    public static void applyTheme(Context context, String mode) {
        String normalized = normalizeThemeMode(mode);
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager uiModeManager =
                (UiModeManager) context.getApplicationContext().getSystemService(Context.UI_MODE_SERVICE);
            if (uiModeManager != null) {
                uiModeManager.setApplicationNightMode(toUiModeNightMode(normalized));
            }
        }
        AppCompatDelegate.setDefaultNightMode(toAppCompatNightMode(normalized));
    }

    public static boolean isSystemNotificationsEnabled(Context context) {
        if (context == null) return true;
        return prefs(context).getBoolean(KEY_SYSTEM_NOTIFICATIONS, true);
    }

    public static void setSystemNotificationsEnabled(Context context, boolean enabled) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_SYSTEM_NOTIFICATIONS, enabled).apply();
    }

    public static boolean isDeadlineRemindersEnabled(Context context) {
        if (context == null) return true;
        return prefs(context).getBoolean(KEY_DEADLINE_REMINDERS, true);
    }

    public static void setDeadlineRemindersEnabled(Context context, boolean enabled) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_DEADLINE_REMINDERS, enabled).apply();
        syncDeadlineReminderSchedule(context);
    }

    public static void syncDeadlineReminderSchedule(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);
        if (isDeadlineRemindersEnabled(appContext)) {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DeadlineReminderWorker.class, 12, TimeUnit.HOURS)
                .build();
            workManager.enqueueUniquePeriodicWork(
                DEADLINE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            );
        } else {
            workManager.cancelUniqueWork(DEADLINE_WORK_NAME);
        }
    }
}
