package com.dtcteam.pypos;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class PyposApplication extends Application {
    
    private ThemeManager themeManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        themeManager = ThemeManager.getInstance(this);
        
        // Apply saved theme on app start
        int savedMode = themeManager.getThemeMode();
        switch (savedMode) {
            case ThemeManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case ThemeManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case ThemeManager.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
