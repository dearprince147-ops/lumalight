package com.azlan.lumalight.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/** Stores the two colors chosen for each individual widget instance. */
public final class WidgetColorPrefs {
    private static final String PREFS_NAME = "lumalight_widget_colors";

    private WidgetColorPrefs() {}

    public static void setColor(Context context, int appWidgetId, int color) {
        prefs(context).edit().putInt(key(appWidgetId, 1), color).apply();
    }

    public static int getColor(Context context, int appWidgetId) {
        return getColor1(context, appWidgetId);
    }

    public static void setColor1(Context context, int appWidgetId, int color) {
        prefs(context).edit().putInt(key(appWidgetId, 1), color).apply();
    }

    public static int getColor1(Context context, int appWidgetId) {
        return prefs(context).getInt(key(appWidgetId, 1), Color.WHITE);
    }

    public static void setColor2(Context context, int appWidgetId, int color) {
        prefs(context).edit().putInt(key(appWidgetId, 2), color).apply();
    }

    public static int getColor2(Context context, int appWidgetId) {
        return prefs(context).getInt(key(appWidgetId, 2), Color.BLACK);
    }

    public static void remove(Context context, int appWidgetId) {
        prefs(context).edit()
                .remove(key(appWidgetId, 1))
                .remove(key(appWidgetId, 2))
                .apply();
    }

    private static String key(int appWidgetId, int colorNumber) {
        return "color_" + colorNumber + "_" + appWidgetId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
