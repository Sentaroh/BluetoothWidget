package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetServiceBtAdapter {
    static final public void initialyzeAdapterWidget(final GlobalParameters gp) {
    	gp.log.addDebugMsg(1, "I", "initialyzeAdapterWidget entered");
    	if (gp.btAdapter.isEnabled()) {
    		if (WidgetUtil.isBluetoothTetherAvailable(gp.appContext, gp.log)) {
    			if (gp.settingsBtAutoEnabledAdapterOn) {
    				boolean cs=WidgetUtil.panIsTeringOn(gp);
    				if (!cs) {
    					gp.setBluetoothTetheringEnabled(gp.appContext, true);
    					WidgetUtil.panSetBluetoothTering(gp, true);
    				}
    			}
    		}
    	}
    };

	public static final void removeAdapterWidget(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "removeAdapterWidget entered");
		removeAdapterRemoteView(gp);
	};
	
	public static final void processAdapterWidgetIntent(
			GlobalParameters gp,
			String action, Intent in) {
    	if (action.equals(WIDGET_ADAPTER_ENABLE)) {
    	} else if (action.equals(WIDGET_ADAPTER_UPDATE)) {
    		createAdapterRemoteView(gp);
    		setAdapterButtonIntent(gp);
    		setAdapterButtonIcon(gp,false);
    		updateAdapterWidget(gp);
    	} else if (action.equals(WIDGET_ADAPTER_DISABLE)) {
    	} else if (action.equals(WIDGET_ADAPTER_DELETED)) {
    		removeAdapterRemoteView(gp);
    		setAdapterButtonIntent(gp);
    	}
    };

    public static final void processAdapterButtonIntent(
    		final GlobalParameters gp,
    		String action, Intent in) {
	    gp.log.addDebugMsg(1, "I", "processAdapterButton entered,"+
	    		" enabled="+gp.btAdapter.isEnabled());
    	if(HOME_SCREEN_ADAPTER_TOGGLE_BTN.equals(action) && gp.adapterBtnEnabled){
    		gp.adapterBtnEnabled=false;
    		if (gp.btAdapter.isEnabled()) {
    			setAdapterButtonIcon(gp,true);
    			gp.btAdapter.disable();
    		} else {
    			setAdapterButtonIcon(gp,true);
    			gp.btAdapter.enable();
    		}
    		updateAdapterWidget(gp);
    	}
    };
    
    public static void createAdapterRemoteView(GlobalParameters gp) { 
    	gp.log.addDebugMsg(1, "I", "createAdapterRemoteView entered");
    	if (gp.adapterRemoteViews==null) {
    		gp.adapterRemoteViews=new RemoteViews(gp.appContext.getPackageName(), 
        			R.layout.widget_layout_adapter);
    		gp.adapterComponentName=
    				new ComponentName(gp.appContext, WidgetProviderBluetoothAdapter.class);
    	}
    };

    private static void removeAdapterRemoteView(GlobalParameters gp) { 
    	if (gp.adapterRemoteViews!=null) {
        	Intent intent = new Intent();
        	intent.setAction(HOME_SCREEN_PAN_TOGGLE_BTN);
        	PendingIntent pi = 
        		PendingIntent.getBroadcast(gp.appContext, 
        				0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
        	gp.adapterRemoteViews.setOnClickPendingIntent(R.id.widget_layout_bt_btn, pi);

        	if (!isAdapterRemoteViewExists(gp)) {
            	gp.adapterRemoteViews=null;
            	gp.adapterComponentName=null;
        	}
    	}
    };
    
    public static void setAdapterButtonIcon(GlobalParameters gp, boolean on_off) {
    	if (gp.adapterRemoteViews!=null) {
        	if (on_off) {
        		gp.adapterRemoteViews.setImageViewBitmap(R.id.widget_layout_bt_btn, gp.mBmAdapterOnOff);
        	} else {
        		if (gp.btAdapter.isEnabled()) {
        			gp.adapterRemoteViews.setImageViewBitmap(R.id.widget_layout_bt_btn, gp.mBmAdapterOn);
        		} else {
        			gp.adapterRemoteViews.setImageViewBitmap(R.id.widget_layout_bt_btn, gp.mBmAdapterOff);
        		}
        	}
       	  	updateAdapterWidget(gp);
    	}
	};

	public static void updateAdapterWidget(GlobalParameters gp) {
    	if (gp.adapterRemoteViews!=null) {
    		gp.widgetManager.updateAppWidget(gp.adapterComponentName, gp.adapterRemoteViews);
      	}
	};

	public static boolean isAdapterRemoteViewExists(GlobalParameters gp) {
		boolean result=false;
		String str_id="", sep="";
		if (gp.adapterComponentName!=null) {
			int[] wids =gp.widgetManager.getAppWidgetIds(gp.adapterComponentName);
			if (wids!=null && wids.length>0) result=true;
			for (int i=0;i<wids.length;i++) {
				str_id+=sep+wids[i];
				sep=",";
			}
		}
		gp.log.addDebugMsg(1, "I", "isAdapterRemoteViewExists result="+result+", ids="+str_id);
		return result;
	};
	
	private static void setAdapterButtonIntent(GlobalParameters gp) { 
    	gp.log.addDebugMsg(1, "I", "setAdapterButtonIntent entered");
    	if (gp.adapterRemoteViews==null) return;
    	Intent in_toggle = new Intent(gp.appContext, WidgetService.class);
    	in_toggle.setAction(HOME_SCREEN_ADAPTER_TOGGLE_BTN);
//    	in_toggle.putExtra(WIDGET_ID_KEY, wids[i]);
    	PendingIntent pending_in_toggle = PendingIntent.getService(
    			gp.appContext, 0, in_toggle, 0);
    	gp.adapterRemoteViews.setOnClickPendingIntent(R.id.widget_layout_bt_btn, 
    			pending_in_toggle);

    };
    
    

}
