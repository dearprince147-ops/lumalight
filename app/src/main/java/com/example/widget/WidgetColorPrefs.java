package com.example.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Stores the color chosen for each individual home screen widget instance.
 * Keyed by the widget's AppWidgetManager-assigned id, which is globally unique
 * across every widget on the device - so both the Flashlight widget and the
 * Strobe widget can safely share one preference file without collisions.
 */
public final class WidgetColorPrefs {
    private static final String PREFS_NAME = "lumalight_widget_colors";

    private WidgetColorPrefs() {}

    public static void setColor(Context context, int appWidgetId, int color) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putInt(key(appWidgetId), color);
        editor.apply();
    }

    /** Returns the stored color for a widget, or white if none has been chosen yet. */
    public static int getColor(Context context, int appWidgetId) {
        return prefs(context).getInt(key(appWidgetId), Color.WHITE);
    }

    public static void remove(Context context, int appWidgetId) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.remove(key(appWidgetId));
        editor.apply();
    }

    private static String key(int appWidgetId) {
        return "color_" + appWidgetId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
