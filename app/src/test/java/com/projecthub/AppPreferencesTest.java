package com.projecthub;

import static org.junit.Assert.assertEquals;

import android.app.UiModeManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.projecthub.utils.AppPreferences;

import org.junit.Test;

public class AppPreferencesTest {

    @Test
    public void normalizeThemeMode_mapsValidAndFallbackValues() {
        assertEquals(AppPreferences.THEME_LIGHT, AppPreferences.normalizeThemeMode("light"));
        assertEquals(AppPreferences.THEME_LIGHT, AppPreferences.normalizeThemeMode("  LIGHT "));
        assertEquals(AppPreferences.THEME_DARK, AppPreferences.normalizeThemeMode("dark"));
        assertEquals(AppPreferences.THEME_SYSTEM, AppPreferences.normalizeThemeMode("system"));
        assertEquals(AppPreferences.THEME_SYSTEM, AppPreferences.normalizeThemeMode(null));
        assertEquals(AppPreferences.THEME_SYSTEM, AppPreferences.normalizeThemeMode("invalid"));
    }

    @Test
    public void toAppCompatNightMode_mapsLightDarkAndSystem() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppPreferences.toAppCompatNightMode(AppPreferences.THEME_LIGHT)
        );
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppPreferences.toAppCompatNightMode(AppPreferences.THEME_DARK)
        );
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppPreferences.toAppCompatNightMode(AppPreferences.THEME_SYSTEM)
        );
    }

    @Test
    public void toUiModeNightMode_mapsLightDarkAndSystem() {
        assertEquals(
            UiModeManager.MODE_NIGHT_NO,
            AppPreferences.toUiModeNightMode(AppPreferences.THEME_LIGHT)
        );
        assertEquals(
            UiModeManager.MODE_NIGHT_YES,
            AppPreferences.toUiModeNightMode(AppPreferences.THEME_DARK)
        );
        assertEquals(
            UiModeManager.MODE_NIGHT_AUTO,
            AppPreferences.toUiModeNightMode(AppPreferences.THEME_SYSTEM)
        );
    }
}
