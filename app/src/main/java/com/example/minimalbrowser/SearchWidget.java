package com.example.minimalbrowser;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class SearchWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);

            Intent intent = new Intent(context, SearchActivity.class);
            intent.putExtra("fromWidget", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );



            // Make both the text and icon open the app
            views.setOnClickPendingIntent(R.id.search_button, pendingIntent);
            views.setOnClickPendingIntent(R.id.search_placeholder, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
