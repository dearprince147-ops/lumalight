package com.azlan.lumalight.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.azlan.lumalight.R;

public class StrobeWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) WidgetColorPrefs.remove(context, id);
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int id) {
        int color1 = WidgetColorPrefs.getColor1(context, id);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_strobe);
        views.setInt(R.id.widgetIcon, "setColorFilter", color1);

        Intent launchIntent = new Intent(context, QuickStrobeActivity.class);
        launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        manager.updateAppWidget(id, views);
    }
}
