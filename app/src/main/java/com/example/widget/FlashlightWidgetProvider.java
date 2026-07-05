package com.example.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.R;

public class FlashlightWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetColorPrefs.remove(context, appWidgetId);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        int color = WidgetColorPrefs.getColor(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_flashlight);
        views.setInt(R.id.widgetIcon, "setColorFilter", color);

        Intent launchIntent = new Intent(context, QuickFlashlightActivity.class);
        launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // requestCode uses the widget id so every widget instance gets its own
        // distinct PendingIntent (otherwise taps on different widgets could be
        // conflated into the same extras bundle).
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
