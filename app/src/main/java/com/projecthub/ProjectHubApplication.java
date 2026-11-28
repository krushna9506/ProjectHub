package com.projecthub;

import android.app.Application;

import com.projecthub.utils.AppPreferences;

/**
 * Application entry point that applies saved theme preferences on cold start.
 */
public class ProjectHubApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppPreferences.applySavedTheme(this);
    }
}
