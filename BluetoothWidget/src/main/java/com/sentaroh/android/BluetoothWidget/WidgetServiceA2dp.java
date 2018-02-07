package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.util.ArrayList;

import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

public class WidgetServiceA2dp {
	
    static final public void initialyzeA2dpWidget(final GlobalParameters gp) {
    	gp.log.addDebugMsg(1, "I", "initialyzeA2dpWidget entered");

		gp.a2dpWidgetTableList=WidgetUtil.loadA2dpWidgetTable(gp,gp.log);
//		Log.v("","tbl="+gp.a2dpWidgetTableList);
//		WidgetListItem wli2=new WidgetListItem();
//		wli2.widget_id=1000;
//		gp.a2dpWidgetTableList.add(wli2);
    	int[] wids =gp.widgetManager.getAppWidgetIds(gp.a2dpComponentName);
    	ArrayList<WidgetListItem> nwt=new ArrayList<WidgetListItem>();
		for(int i=0;i<wids.length;i++) {
//			Log.v("","id="+wids[i]);
			WidgetListItem cwli=
					WidgetUtil.getWidgetItemByWidgetId(gp.a2dpWidgetTableList,wids[i]);
			WidgetListItem wli=new WidgetListItem();
			wli.widget_id=wids[i];
			wli.widget_type=WidgetListItem.WIDGET_TABLE_TYPE_A2DP;
			if (cwli!=null) {
				wli.device_name=cwli.device_name;
				wli.auto_connect_adapter_on=cwli.auto_connect_adapter_on;
			}

			nwt.add(wli); 
		}
		gp.a2dpWidgetTableList=nwt;
		WidgetUtil.saveA2dpWidgetTable(gp,gp.a2dpWidgetTableList,gp.log); 
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++){
			WidgetListItem wli=gp.a2dpWidgetTableList.get(i);
			createA2dpRemoteView(gp, wli.widget_id);
//			if (wli.device_name.equals(DEVICE_NAME_UNSELECTED)) {
//				AppWidgetHost wh=new AppWidgetHost(gp.appContext,R.string.app_name);
//				wh.deleteAppWidgetId(wli.widget_id);
//			}
		}
		setA2dpButtonIntent(gp);
		if (gp.btAdapter.isEnabled()) {
			Thread th=new Thread() {
				@Override
				public void run() {
					for (int i=0;i<100;i++) {
						if (gp.isA2dpAvailable &&
								gp.isHspAvailable) break;
						SystemClock.sleep(50);
					}
					gp.log.addDebugMsg(1, "I", "A2DP Set initial status");
					for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
						WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
						BluetoothDevice bd=WidgetUtil.getBluetoothDeviceByName(gp,cwt.device_name);
						int cs_a2dp=0, cs_hsp=0;
						if (bd!=null) {
							cs_a2dp=WidgetUtil.a2dpGetConnectionState(gp,bd);
							cs_hsp=WidgetUtil.hspGetConnectionState(gp,bd);
						}
						cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
						if (cs_a2dp==BluetoothProfile.STATE_CONNECTED || 
								cs_hsp==BluetoothProfile.STATE_CONNECTED) {
							cwt.connection_status=BluetoothProfile.STATE_CONNECTED;
							if (!WidgetUtil.a2dpIsA2dpPlaying(gp, bd)) {
								WidgetUtil.addUnplayingList(gp,bd.getName(),gp.unplayingDeviceList);
								WidgetUtil.cancelA2dpDisconnectTimer(gp);
								WidgetUtil.scheduleA2dpDisconnectTimer(gp, gp.unplayingDeviceList);
							}
						}
					}
					setA2dpButtonIcon(gp);
					updateA2dpWidget(gp);
				}
			};
			th.start();
		}
    };

	public static final void removeA2dpWidget(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "removeA2dpWidget entered");
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.a2dpComponentName);
		for(int i=0;i<wids.length;i++) removeA2dpRemoteView(gp,wids[i]);
		WidgetUtil.saveA2dpWidgetTable(gp, gp.a2dpWidgetTableList,gp.log);
	};
	
//	static final private SharedPreferences getPrefMgr() {
//    	return WidgetUtil.getPrefMgr(gp.appContext);
//    }

	public static final void processA2dpWidgetIntent(GlobalParameters gp,
			String action, int[] wids) {
    	if (action.equals(WIDGET_A2DP_DEV_ENABLE)) {
//    		createBtPanSwRemoteView();
    	} else if (action.equals(WIDGET_A2DP_DEV_UPDATE)) {
    		if (wids!=null) {
        		for (int i=0;i<wids.length;i++)createA2dpRemoteView(gp,wids[i]); 
    		}
    		setA2dpButtonIntent(gp);
    		setA2dpButtonIcon(gp);
    		updateA2dpWidget(gp);
//    		WidgetListItem cwt=WidgetUtil.getWidgetItemByWidgetId(gp.a2dpWidgetTableList,wid);
//    		if (cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) 
//    			startA2dpConfigActivity(gp,wid);
    	} else if (action.equals(WIDGET_A2DP_DEV_DISABLE)) {
//    		removeBtPanSwRemoteView();
    	} else if (action.equals(WIDGET_A2DP_DEV_DELETED)) {
    		if (wids!=null) {
    			for (int i=0;i<wids.length;i++)removeA2dpRemoteView(gp,wids[i]);
    		}
    		setA2dpButtonIntent(gp);
    	}
    };

    public static final void processA2dpButtonIntent(final GlobalParameters gp,
    		String action, Intent in, int wid) {
	    gp.log.addDebugMsg(1, "I", "processA2dpButton entered,"+
	    		" widget="+wid+", enabled="+gp.btAdapter.isEnabled()); 
    	if (wid>0) {
    		WidgetListItem cwt=WidgetUtil.getWidgetItemByWidgetId(gp.a2dpWidgetTableList,wid);
    	    final BluetoothDevice device = WidgetUtil.getBluetoothDeviceByName(gp,cwt.device_name);
        	if(HOME_SCREEN_A2DP_DEV_TOGGLE_BTN.equals(action)  && gp.a2dpBtnEnabled){
        	    if (device != null && gp.btAdapter.isEnabled() && 
        	    		!cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
    				int ret=cwt.connection_status;
    				String c_dev_name=device.getName();
    				if (ret==BluetoothProfile.STATE_CONNECTED) {
    					WidgetUtil.a2dpDisconnect(gp, device);
    					WidgetUtil.hspDisconnect(gp, device);
    					for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
    						if (gp.a2dpWidgetTableList.get(i).device_name.equals(c_dev_name)) {
    							gp.a2dpWidgetTableList.get(i).connection_status=
    									BluetoothProfile.STATE_DISCONNECTING;
    						}
    					}
    				    gp.a2dpBtnEnabled=false;
    				    WidgetUtil.setRefreshDeviceStatusTimer(gp);
    				} else if (ret==BluetoothProfile.STATE_CONNECTING) { 
    				} else if (ret==BluetoothProfile.STATE_DISCONNECTED) {
    					WidgetUtil.a2dpConnect(gp, device);
//    				    hspDevConnect(gp, device);
    					for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
    						if (gp.a2dpWidgetTableList.get(i).device_name.equals(c_dev_name)) {
    							gp.a2dpWidgetTableList.get(i).connection_status=
    									BluetoothProfile.STATE_CONNECTING;
    							gp.a2dpWidgetTableList.get(i).connect_required_if_bt_off=true;
    						}
    					}
    				    gp.a2dpBtnEnabled=false;
    				    WidgetUtil.setRefreshDeviceStatusTimer(gp);
    				} else if (ret==BluetoothProfile.STATE_DISCONNECTING) { 
    				}
    				setA2dpButtonIcon(gp);
    				updateA2dpWidget(gp);
        	    } else {
        	    	if (cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
        	    		WidgetUtil.startA2dpConfigActivity(gp,wid);
//    	    			Toast.makeText(gp.appContext, 
//    	    					gp.appContext.getString(R.string.msgs_pan_dev_device_not_found), 
//    							Toast.LENGTH_SHORT)
//    							.show();
        	    	} else if (!gp.btAdapter.isEnabled()) {
//        	    		Log.v("","flag="+gp.settingsA2dpBtOnIfBtOff);
        	    		if (gp.settingsA2dpOnAdapterOff) {
        	    			cwt.connect_required_if_bt_off=true;
        	    			cwt.connection_status=BluetoothProfile.STATE_CONNECTING;
          	    			gp.btAdapter.enable();
          	      	    	setA2dpButtonIcon(gp);
          	      	    	updateA2dpWidget(gp);
          	    		} else {
          	    			Toast.makeText(gp.appContext, 
          	    				gp.appContext.getString(R.string.msgs_pan_disable_bt_adapter), 
        						Toast.LENGTH_SHORT)
        						.show();
          	    		}
        	    	} else {
    					Toast.makeText(gp.appContext, 
    							gp.appContext.getString(R.string.msgs_pan_device_not_found), 
    							Toast.LENGTH_SHORT)
    							.show();
        	    	}
        	    }
        	    setA2dpButtonIntent(gp);
    		}
    		
    	}
    };
    
    public static void createA2dpRemoteView(GlobalParameters gp, int wid) { 
    	gp.log.addDebugMsg(1, "I", "createA2dpRemoteView entered, wid="+wid);
    	WidgetListItem cwt=WidgetUtil.getWidgetItemByWidgetId(gp.a2dpWidgetTableList,wid);
    	if (cwt==null) {
    		WidgetListItem nwt=new WidgetListItem();
    		nwt.widget_id=wid;
    		nwt.remote_view=new RemoteViews(gp.appContext.getPackageName(), 
        			R.layout.widget_layout_a2dp);
    		gp.a2dpWidgetTableList.add(nwt);
    		WidgetUtil.saveA2dpWidgetTable(gp,gp.a2dpWidgetTableList,gp.log);
//    		Log.v("","Create new id="+nwt.widget_id+", name="+nwt.device_name+", rv="+nwt.remote_view);
//    		startA2dpConfigActivity(gp, wid);
    	} else {
    		if (cwt.remote_view==null) {
    			cwt.remote_view=new RemoteViews(gp.appContext.getPackageName(), 
            			R.layout.widget_layout_a2dp);
    		}
    	}
    };

    private static void removeA2dpRemoteView(GlobalParameters gp,
    		int widget_id) { 
    	if (widget_id!=-1) {
    		WidgetListItem nwt=WidgetUtil.getWidgetItemByWidgetId(gp.a2dpWidgetTableList,widget_id);
    		if (nwt!=null) {
            	Intent intent = new Intent();
            	intent.setAction(HOME_SCREEN_A2DP_DEV_TOGGLE_BTN);
            	PendingIntent pi = 
            		PendingIntent.getBroadcast(gp.appContext, widget_id, intent,PendingIntent.FLAG_CANCEL_CURRENT);
            	nwt.remote_view.setOnClickPendingIntent(R.id.device_layout_a2dp_toggle_btn, pi);

            	gp.a2dpWidgetTableList.remove(nwt);
            	nwt.remote_view=null;
            	nwt.device_name=null;
            	WidgetUtil.saveA2dpWidgetTable(gp, gp.a2dpWidgetTableList, gp.log);
    		}
    	}
    };
    
    public static void setA2dpButtonIcon(GlobalParameters gp) {
    	for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
    		WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
        	if (cwt.remote_view!=null) {
        		int ret=cwt.connection_status;
            	gp.log.addDebugMsg(1, "I", "setA2dpButtonIcon connection "+
            		"name="+cwt.device_name+", id="+cwt.widget_id+
            		", status="+ret);
            	if (cwt.identify) {
            		cwt.remote_view.setTextViewText(R.id.device_layout_a2dp_devname,
            				""+cwt.widget_id); 
            	} else {
            		cwt.remote_view.setTextViewText(R.id.device_layout_a2dp_devname,
            				cwt.device_name); 
            	}
        		if (cwt.auto_connect_adapter_on)
        			cwt.remote_view.setViewVisibility(R.id.device_layout_a2dp_auto_conn, ImageView.VISIBLE);
        		else cwt.remote_view.setViewVisibility(R.id.device_layout_a2dp_auto_conn, ImageView.GONE);
        		if (ret==BluetoothProfile.STATE_CONNECTED) { 
        			cwt.remote_view.setImageViewBitmap(R.id.device_layout_a2dp_toggle_btn, gp.mBmA2dpOn);
        		} else if (ret==BluetoothProfile.STATE_CONNECTING) {
        			cwt.remote_view.setImageViewBitmap(R.id.device_layout_a2dp_toggle_btn, gp.mBmA2dpOnOff);
        		} else if (ret==BluetoothProfile.STATE_DISCONNECTED) {
        			cwt.remote_view.setImageViewBitmap(R.id.device_layout_a2dp_toggle_btn, gp.mBmA2dpOff);
        		} else if (ret==BluetoothProfile.STATE_DISCONNECTING) {
        			cwt.remote_view.setImageViewBitmap(R.id.device_layout_a2dp_toggle_btn, gp.mBmA2dpOnOff);
        		}
          	}
    	}
   	  	updateA2dpWidget(gp);
	};

	public static void updateA2dpWidget(GlobalParameters gp) {
    	for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
    		WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
        	if (cwt.remote_view!=null) {
        		gp.widgetManager.updateAppWidget(cwt.widget_id, cwt.remote_view);
          	}
    	}
	};

	public static boolean isA2dpRemoteViewExists(GlobalParameters gp) {
		boolean result=false;
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.a2dpComponentName);
		if (wids!=null && wids.length>0) result=true;
		String str_id="", sep="";
		for (int i=0;i<wids.length;i++) {
			str_id+=sep+wids[i];
			sep=",";
		}
		gp.log.addDebugMsg(1, "I", "isA2dpRemoteViewExists result="+result+", ids="+str_id);
		return result;
	};
	
	private static void setA2dpButtonIntent(GlobalParameters gp) { 
    	gp.log.addDebugMsg(1, "I", "setA2dpButtonIntent entered");
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.a2dpComponentName);
		for (int i=0;i<wids.length;i++) {
	    	WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
//	    	Log.v("","set intent id="+cwt.widget_id+", name="+cwt.device_name+", rv="+cwt.remote_view);
	    	Intent in_toggle = new Intent(gp.appContext, WidgetService.class);
	    	in_toggle.setAction(HOME_SCREEN_A2DP_DEV_TOGGLE_BTN);
	    	in_toggle.putExtra(WIDGET_ID_KEY, wids[i]);
	    	PendingIntent pending_in_toggle = PendingIntent.getService(
	    			gp.appContext, wids[i], in_toggle, 0);
	    	cwt.remote_view.setOnClickPendingIntent(R.id.device_layout_a2dp_toggle_btn, 
	    			pending_in_toggle);
		}
    };
    
}
