package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.APPLICATION_TAG;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_ADAPTER_DELETED;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_ADAPTER_DISABLE;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_ADAPTER_ENABLE;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_ADAPTER_UPDATE;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_ARRAY_ID_KEY;
import static com.sentaroh.android.BluetoothWidget.Common.Constants.WIDGET_DEBUG_ENABLE;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProviderBluetoothAdapter extends AppWidgetProvider {
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
				"onUpdate(Adapter) widgetId="+wids);
        for (int i=0;i<awi.length;i++) {
    		RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_layout_adapter);
        	awm.updateAppWidget(awi[i], rv);
        }
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_ADAPTER_UPDATE);
    	in.putExtra(WIDGET_ARRAY_ID_KEY, awi);
        c.startService(in);
    };

    @Override
    public void onDisabled(Context c) {
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onDisabled(Adapter)");
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_ADAPTER_DISABLE);
        c.startService(in);
    };

    @Override
    public void onEnabled(Context c) {
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onEnabled(Adapter)");
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_ADAPTER_ENABLE);
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
        if (WIDGET_DEBUG_ENABLE) Log.v(APPLICATION_TAG, "onDeleted(Adapter) widgetId="+wids);
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_ADAPTER_DELETED);
    	in.putExtra(WIDGET_ARRAY_ID_KEY, awi);
        c.startService(in);
    };
}