package com.dtcteam.pypos;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    private static ThemeManager instance;
    private SharedPreferences prefs;
    
    private ThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    public void applyTheme() {
        int themeMode = getThemeMode();
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }
    
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme();
    }
    
    public boolean isDarkMode() {
        int mode = getThemeMode();
        if (mode == THEME_DARK) return true;
        if (mode == THEME_SYSTEM) {
            // For system, check actual system preference
            return (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        }
        return false;
    }
    
    public void toggleTheme() {
        int current = getThemeMode();
        if (current == THEME_LIGHT) {
            setThemeMode(THEME_DARK);
        } else {
            setThemeMode(THEME_LIGHT);
        }
    }
    
    public String getThemeName() {
        switch (getThemeMode()) {
            case THEME_LIGHT: return "Light";
            case THEME_DARK: return "Dark";
            case THEME_SYSTEM: return "System Default";
            default: return "System Default";
        }
    }
}