package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import com.sentaroh.android.BluetoothWidget.R;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProviderA2dp extends AppWidgetProvider {
	@Override
    public void onUpdate(Context c, AppWidgetManager awm, int[] awi) {
		String wids="";
		if (awi!=null && awi.length>0) {
			String sep="";
			for (int i=0;i<awi.length;i++) {
				wids+=sep+awi[i];
				sep=",";
			}
		} else wids="none";
		if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, 
				"onUpdate(A2dp) widgetId="+wids);
        for (int i=0;i<awi.length;i++) {
    		RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_layout_a2dp);
        	awm.updateAppWidget(awi[i], rv);
        }
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_A2DP_DEV_UPDATE);
    	in.putExtra(WIDGET_ARRAY_ID_KEY, awi);
        c.startService(in);
    };

    @Override
    public void onDisabled(Context c) {
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onDisabled(A2dp)");
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_A2DP_DEV_DISABLE);
        c.startService(in);
    };

    @Override
    public void onEnabled(Context c) {
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onEnabled(A2dp)");
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_A2DP_DEV_ENABLE);
        c.startService(in);
    };
    
    @Override
    public void onDeleted(Context c, int[] awi) {
		String wids="";
		if (awi!=null && awi.length>0) {
			String sep="";
			for (int i=0;i<awi.length;i++) {
				wids+=sep+awi[i];
				sep=",";
			}
		} else wids="none";
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onDeleted(A2dp) widgetId="+wids);
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_A2DP_DEV_DELETED);
    	in.putExtra(WIDGET_ARRAY_ID_KEY, awi);
        c.startService(in);
    };
}