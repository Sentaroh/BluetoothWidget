package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.util.ArrayList;
import java.util.List;

import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

public class WidgetServicePan {
	
    static final public void initialyzePanWidget(final GlobalParameters gp) {
    	gp.log.addDebugMsg(1, "I", "initialyzePanWidget entered");

		gp.panWidgetTableList=WidgetUtil.loadPanWidgetTable(gp,gp.log);
    	int[] wids =gp.widgetManager.getAppWidgetIds(gp.panComponentName);
    	ArrayList<WidgetListItem> nwt=new ArrayList<WidgetListItem>();
		for(int i=0;i<wids.length;i++) {
			WidgetListItem cwli=
					WidgetUtil.getWidgetItemByWidgetId(gp.panWidgetTableList,wids[i]);
			WidgetListItem wli=new WidgetListItem();
			wli.widget_id=wids[i];
			wli.widget_type=WidgetListItem.WIDGET_TABLE_TYPE_PAN;
			if (cwli!=null) {
				wli.device_name=cwli.device_name;
				wli.auto_connect_adapter_on=cwli.auto_connect_adapter_on;
			}
			nwt.add(wli);
			Log.v("","wli.name="+wli.device_name+", id="+wli.widget_id);
		}
		gp.panWidgetTableList=nwt;
		WidgetUtil.savePanWidgetTable(gp,gp.panWidgetTableList,gp.log); 
		for (int i=0;i<gp.panWidgetTableList.size();i++){
			WidgetListItem wli=gp.panWidgetTableList.get(i);
			createPanRemoteView(gp, wli.widget_id);
//			if (wli.device_name.equals(DEVICE_NAME_UNSELECTED)) {
//				AppWidgetHost wh=new AppWidgetHost(gp.appContext,R.string.app_name);
//				wh.deleteAppWidgetId(wli.widget_id);
//			}
		}
		setPanButtonIntent(gp);
		if (gp.btAdapter.isEnabled()) {
			Thread th=new Thread() {
				@Override
				public void run() {
					for (int i=0;i<100;i++) {
						if (gp.isPanAvailable &&
								gp.isHspAvailable) break;
						SystemClock.sleep(50);
					}
					gp.log.addDebugMsg(1, "I", "PAN Set initial status");
					for (int i=0;i<gp.panWidgetTableList.size();i++) {
						WidgetListItem cwt=gp.panWidgetTableList.get(i);
						BluetoothDevice bd=WidgetUtil.getBluetoothDeviceByName(gp,cwt.device_name);
						int cs_pan=0;
						if (bd!=null) {
							cs_pan=WidgetUtil.panGetConnectionState(gp,bd);
						}
						cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
						if (cs_pan==BluetoothProfile.STATE_CONNECTED) 
							cwt.connection_status=BluetoothProfile.STATE_CONNECTED;
					}
					setPanButtonIcon(gp);
					updatePanWidget(gp);
				}
			};
			th.start();
		}
    };

	public static final void removePanWidget(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "removePanWidget entered");
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.panComponentName);
		for(int i=0;i<wids.length;i++) removePanRemoteView(gp,wids[i]);
		WidgetUtil.savePanWidgetTable(gp, gp.panWidgetTableList,gp.log);
	};
	
//	static final private SharedPreferences getPrefMgr() {
//    	return WidgetUtil.getPrefMgr(gp.appContext);
//    }

	public static final void processPanWidgetIntent(GlobalParameters gp,
			String action, int[] wids) {
    	if (action.equals(WIDGET_PAN_DEV_ENABLE)) {
//    		createBtPanSwRemoteView();
    	} else if (action.equals(WIDGET_PAN_DEV_UPDATE)) {
    		if (wids!=null) {
        		for (int i=0;i<wids.length;i++)createPanRemoteView(gp,wids[i]); 
    		}
    		setPanButtonIntent(gp);
    		setPanButtonIcon(gp);
    		updatePanWidget(gp);
    	} else if (action.equals(WIDGET_PAN_DEV_DISABLE)) {
//    		removeBtPanSwRemoteView();
    	} else if (action.equals(WIDGET_PAN_DEV_DELETED)) {
    		if (wids!=null) {
        		for (int i=0;i<wids.length;i++)removePanRemoteView(gp,wids[i]); 
    		}
    		setPanButtonIntent(gp);
    	}
    };

    public static final void processPanButtonIntent(final GlobalParameters gp,
    		String action, Intent in, int wid) {
	    gp.log.addDebugMsg(1, "I", "processPanButton entered,"+
	    		" widget="+wid+", enabled="+gp.btAdapter.isEnabled()); 
    	if (wid>0) {
    		WidgetListItem cwt=WidgetUtil.getWidgetItemByWidgetId(gp.panWidgetTableList,wid);
    	    final BluetoothDevice device = WidgetUtil.getBluetoothDeviceByName(gp,cwt.device_name);
        	if(HOME_SCREEN_PAN_TOGGLE_BTN.equals(action) && gp.panBtnEnabled){
        	    if (device != null && gp.btAdapter.isEnabled() && 
        	    		!cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
    				int ret=cwt.connection_status;
    				final String c_dev_name=device.getName();
    				if (ret==BluetoothProfile.STATE_CONNECTED) {
    					if (WidgetUtil.panGetConnectionState(gp, device)==BluetoothProfile.STATE_CONNECTED) {
    						if (WidgetUtil.panDisconnect(gp, device)) {
            					for (int i=0;i<gp.panWidgetTableList.size();i++) {
            						if (gp.panWidgetTableList.get(i).device_name.equals(c_dev_name)) {
            							gp.panWidgetTableList.get(i).connection_status=
            									BluetoothProfile.STATE_DISCONNECTING;
            						}
            					}
            					gp.panBtnEnabled=false;
            					updatePanWidget(gp);
            				    WidgetUtil.setRefreshDeviceStatusTimer(gp);
    						} else {
    							putToastMessage(gp,gp.appContext.getString(R.string.msgs_pan_device_disc_error));
    						}
    					}
    				} else if (ret==BluetoothProfile.STATE_CONNECTING) { 
    				} else if (ret==BluetoothProfile.STATE_DISCONNECTED) {
    					//Disconnect if other device was connected
//    					List<BluetoothDevice>bd_list=WidgetUtil.panGetConnectedDevices(gp);
//    					if (bd_list.size()>0) {
//    						for (int i=0;i<bd_list.size();i++){
//    							WidgetUtil.panDisconnect(gp, bd_list.get(i));
//    							waitPanDeviceDisconnect(gp, bd_list.get(i));
//    						}
//    					} else {
//    						for (int i=0;i<gp.panWidgetTableList.size();i++) {
//    							BluetoothDevice bd=WidgetUtil.getBluetoothDeviceByName(gp, 
//    									gp.panWidgetTableList.get(i).device_name);
//    							if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_CONNECTED) {
//        							WidgetUtil.panDisconnect(gp, bd);
//        							waitPanDeviceDisconnect(gp, bd);
//    							} else if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_CONNECTING) {
//    								waitPanDeviceConnect(gp, bd);
//        							WidgetUtil.panDisconnect(gp, bd);
//        							waitPanDeviceDisconnect(gp, bd);
//    							} else if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_DISCONNECTED) {
//    								
//    							} else if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_DISCONNECTING) {
//    								waitPanDeviceDisconnect(gp, bd);
//    							}
//    						}
//    					}
//    					for (int i=0;i<gp.panWidgetTableList.size();i++) {
//    						BluetoothDevice bd = WidgetUtil.getBluetoothDeviceByName(gp,gp.panWidgetTableList.get(i).device_name);
//    						if (bd!=null) WidgetUtil.panGetConnectionState(gp, bd);
//						}
////    					for (int i=0;i<gp.panWidgetTableList.size();i++) {
////    						BluetoothDevice bd = WidgetUtil.getBluetoothDeviceByName(gp,gp.panWidgetTableList.get(i).device_name);
////    						if (bd!=null) WidgetUtil.panDisconnect(gp, bd);
////   						}
//    					WidgetUtil.panConnect(gp, device);
//    					int cs=WidgetUtil.panGetConnectionState(gp, device);
//    					if (cs==BluetoothProfile.STATE_CONNECTING) {
//    					}
//    					for (int i=0;i<gp.panWidgetTableList.size();i++) {
//    						if (gp.panWidgetTableList.get(i).device_name.equals(c_dev_name)) {
//    							gp.panWidgetTableList.get(i).connection_status=
//    									BluetoothProfile.STATE_CONNECTING;
//    							gp.panWidgetTableList.get(i).connect_required_if_bt_off=true;
//    						}
//    					}
//    				    gp.panBtnEnabled=false;
//    				    WidgetUtil.setRefreshDeviceStatusTimer(gp);
//    				    waitPanDeviceConnect(gp, device);

    					List<BluetoothDevice>bd_list=WidgetUtil.panGetConnectedDevices(gp);
    					if (bd_list.size()==0) {
    						WidgetUtil.panGetConnectionState(gp, device);
        					if (WidgetUtil.panConnect(gp, device)) {
            					int cs=WidgetUtil.panGetConnectionState(gp, device);
            					if (cs==BluetoothProfile.STATE_CONNECTING) {
            					}
            					for (int i=0;i<gp.panWidgetTableList.size();i++) {
            						if (gp.panWidgetTableList.get(i).device_name.equals(c_dev_name)) {
            							gp.panWidgetTableList.get(i).connection_status=
            									BluetoothProfile.STATE_CONNECTING;
            							gp.panWidgetTableList.get(i).connect_required_if_bt_off=true;
            						}
            					}
            					updatePanWidget(gp);
            				    gp.panBtnEnabled=false;
            				    WidgetUtil.setRefreshDeviceStatusTimer(gp);
        					} else {
        						WidgetUtil.panDisconnect(gp, device);
        						WidgetUtil.setRefreshDeviceStatusTimer(gp);
        						putToastMessage(gp,gp.appContext.getString(R.string.msgs_pan_device_conn_error)); 
        					}
    					} else {
    						String m_txt=String.format(
    								gp.appContext.getString(R.string.msgs_pan_turn_off_another_device), 
    								bd_list.get(0).getName());
    						putToastMessage(gp, m_txt);
    					}
    				} else if (ret==BluetoothProfile.STATE_DISCONNECTING) { 
    				}
    				setPanButtonIcon(gp);
    				updatePanWidget(gp);
        	    } else {
        	    	if (cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
        	    		WidgetUtil.startPanConfigActivity(gp,wid);
//    	    			Toast.makeText(gp.appContext, 
//    	    					gp.appContext.getString(R.string.msgs_pan_dev_device_not_found), 
//    							Toast.LENGTH_SHORT)
//    							.show();
        	    	} else if (!gp.btAdapter.isEnabled()) {
//        	    		Log.v("","flag="+gp.settingsPanBtOnIfBtOff);
        	    		if (gp.settingsPanOnAdapterOff) {
        	    			cwt.connect_required_if_bt_off=true;
        	    			cwt.connection_status=BluetoothProfile.STATE_CONNECTING;
          	    			gp.btAdapter.enable();
          	      	    	setPanButtonIcon(gp);
          	      	    	updatePanWidget(gp);
          	    		} else {
    						putToastMessage(gp,gp.appContext.getString(R.string.msgs_pan_disable_bt_adapter)); 
          	    		}
        	    	} else {
						putToastMessage(gp,gp.appContext.getString(R.string.msgs_pan_device_not_found)); 
        	    	}
        	    }
        	    setPanButtonIntent(gp);
    		}
    		
    	}
    };

    final static private void putToastMessage(
    		final GlobalParameters gp, final String msg_txt) {
    	gp.svcHandler.post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(gp.appContext,msg_txt, Toast.LENGTH_SHORT)
				.show();
			}
    	});
    }
//    private static boolean waitPanDeviceDisconnect(
//    		GlobalParameters gp, BluetoothDevice bd) {
//    	boolean result=false;
//    	
//    	for (int i=0;i<100;i++) {
//    		if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_DISCONNECTED) {
//    			result=true;
//    			break;
//    		}
//    		SystemClock.sleep(100);
//    	}
//    	return result;
//    };
//    private static boolean waitPanDeviceConnect(
//    		GlobalParameters gp, BluetoothDevice bd) {
//    	boolean result=false;
//    	
//    	for (int i=0;i<100;i++) {
//    		if (WidgetUtil.panGetConnectionState(gp, bd)==BluetoothProfile.STATE_CONNECTED) {
//    			result=true;
//    			break;
//    		}
//    		SystemClock.sleep(100);
//    	}
//    	return result;
//    };
    
    public static void createPanRemoteView(GlobalParameters gp, int wid) { 
    	gp.log.addDebugMsg(1, "I", "createPanRemoteView entered, wid="+wid);
    	WidgetListItem cwt=WidgetUtil.getWidgetItemByWidgetId(gp.panWidgetTableList,wid);
    	if (cwt==null) {
    		WidgetListItem nwt=new WidgetListItem();
    		nwt.widget_id=wid;
    		nwt.remote_view=new RemoteViews(gp.appContext.getPackageName(), 
        			R.layout.widget_layout_pan);
    		gp.panWidgetTableList.add(nwt);
    		WidgetUtil.savePanWidgetTable(gp, gp.panWidgetTableList, gp.log);
//    		Log.v("","Create new id="+nwt.widget_id+", name="+nwt.device_name+", rv="+nwt.remote_view);
//   			startPanConfigActivity(gp,wid);
    	} else {
    		if (cwt.remote_view==null) {
    			cwt.remote_view=new RemoteViews(gp.appContext.getPackageName(), 
            			R.layout.widget_layout_pan);
    		}
    	}
    };

    private static void removePanRemoteView(GlobalParameters gp,
    		int widget_id) { 
    	if (widget_id!=-1) {
    		WidgetListItem nwt=WidgetUtil.getWidgetItemByWidgetId(gp.panWidgetTableList,widget_id);
    		if (nwt!=null) {
            	Intent intent = new Intent();
            	intent.setAction(HOME_SCREEN_PAN_TOGGLE_BTN);
            	PendingIntent pi = 
            		PendingIntent.getBroadcast(gp.appContext, widget_id, intent,PendingIntent.FLAG_CANCEL_CURRENT);
            	nwt.remote_view.setOnClickPendingIntent(R.id.device_layout_pan_toggle_btn, pi);

            	gp.panWidgetTableList.remove(nwt);
            	nwt.remote_view=null;
            	nwt.device_name=null;
            	WidgetUtil.savePanWidgetTable(gp, gp.panWidgetTableList, gp.log);
    		}
    	}
    };
    
    public static void setPanButtonIcon(GlobalParameters gp) {
    	for (int i=0;i<gp.panWidgetTableList.size();i++) {
    		WidgetListItem cwt=gp.panWidgetTableList.get(i);
        	if (cwt.remote_view!=null) {
        		int ret=cwt.connection_status;
            	gp.log.addDebugMsg(1, "I", "setPanButtonIcon connection "+
            		"name="+cwt.device_name+", id="+cwt.widget_id+
            		", status="+ret);
            	if (cwt.identify) {
            		cwt.remote_view.setTextViewText(R.id.device_layout_pan_devname,
            				""+cwt.widget_id);
            	} else {
            		cwt.remote_view.setTextViewText(R.id.device_layout_pan_devname,
            				cwt.device_name);
            	}
        		if (cwt.auto_connect_adapter_on)
        			cwt.remote_view.setViewVisibility(R.id.device_layout_pan_auto_conn, ImageView.VISIBLE);
        		else cwt.remote_view.setViewVisibility(R.id.device_layout_pan_auto_conn, ImageView.GONE);
          	  	if (ret==BluetoothProfile.STATE_CONNECTED) { 
          	  		cwt.remote_view.setImageViewBitmap(R.id.device_layout_pan_toggle_btn, gp.mBmPanOn);
          	  	} else if (ret==BluetoothProfile.STATE_CONNECTING) {
          	  		cwt.remote_view.setImageViewBitmap(R.id.device_layout_pan_toggle_btn, gp.mBmPanOnOff);
          	  	} else if (ret==BluetoothProfile.STATE_DISCONNECTED) {
          	  		cwt.remote_view.setImageViewBitmap(R.id.device_layout_pan_toggle_btn, gp.mBmPanOff);
          	  	} else if (ret==BluetoothProfile.STATE_DISCONNECTING) {
          	  		cwt.remote_view.setImageViewBitmap(R.id.device_layout_pan_toggle_btn, gp.mBmPanOnOff);
          	  	}
          	}
    	}
   	  	updatePanWidget(gp);
	};

	public static void updatePanWidget(GlobalParameters gp) {
    	for (int i=0;i<gp.panWidgetTableList.size();i++) {
    		WidgetListItem cwt=gp.panWidgetTableList.get(i);
        	if (cwt.remote_view!=null) {
        		gp.widgetManager.updateAppWidget(cwt.widget_id, cwt.remote_view);
          	}
    	}
	};

	public static boolean isPanRemoteViewExists(GlobalParameters gp) {
		boolean result=false;
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.panComponentName);
		if (wids!=null && wids.length>0) result=true;
		String str_id="", sep="";
		for (int i=0;i<wids.length;i++) {
			str_id+=sep+wids[i];
			sep=",";
		}
		gp.log.addDebugMsg(1, "I", "isPanRemoteViewExists result="+result+", ids="+str_id);
		return result;
	};
	
	private static void setPanButtonIntent(GlobalParameters gp) { 
    	gp.log.addDebugMsg(1, "I", "setPanButtonIntent entered");
		int[] wids =gp.widgetManager.getAppWidgetIds(gp.panComponentName);
		for (int i=0;i<wids.length;i++) {
	    	WidgetListItem cwt=gp.panWidgetTableList.get(i);
//	    	Log.v("","set intent id="+cwt.widget_id+", name="+cwt.device_name+", rv="+cwt.remote_view);
	    	Intent in_toggle = new Intent(gp.appContext, WidgetService.class);
	    	in_toggle.setAction(HOME_SCREEN_PAN_TOGGLE_BTN);
	    	in_toggle.putExtra(WIDGET_ID_KEY, wids[i]);
	    	PendingIntent pending_in_toggle = PendingIntent.getService(
	    			gp.appContext, wids[i], in_toggle, 0);
	    	cwt.remote_view.setOnClickPendingIntent(R.id.device_layout_pan_toggle_btn, 
	    			pending_in_toggle);
		}
    };
    
    
}
