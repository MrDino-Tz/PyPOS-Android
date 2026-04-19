package com.dtcteam.pypos;

import android.content.Context;
import android.content.res.Resources;

public class AppConfig {
    private static final String SUPABASE_URL = "https://dbocluzncuhhlrkeggez.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5";

    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }
}
