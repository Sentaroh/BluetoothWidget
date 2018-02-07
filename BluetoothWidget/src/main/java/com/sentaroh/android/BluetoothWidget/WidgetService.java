package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sentaroh.android.BluetoothWidget.IServiceCallback;
import com.sentaroh.android.BluetoothWidget.IServiceServer;
import com.sentaroh.android.BluetoothWidget.Common.UnplayingDeviceListItem;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SerializeUtil;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class WidgetService extends Service {

	static private GlobalParameters mGp = null;
	static private Context mContext=null;
	static private LogUtil mLog=null;

	static private BroadcastReceiver mBluetoothReceiver = null;
	static private BroadcastReceiver mSleepReceiver = null;

	static private WifiManager mWifiMgr=null;
	
	@SuppressWarnings("unused")
	static private WifiReceiver mWifiReceiver=new WifiReceiver();
	
	@Override
	public void onCreate() {
//		mGp = new GlobalParameters();
		mGp=(GlobalParameters)getApplication();
		mGp.appContext=mContext=getApplicationContext();
		mGp.loadSettingParms(mContext);
		mGp.initialyzeRequired=false;
		mGp.widgetManager = AppWidgetManager.getInstance(mContext);
		mGp.btAdapter = BluetoothAdapter.getDefaultAdapter();
		mGp.log=mLog=new LogUtil(mContext, "Service", mGp);
		mLog.addDebugMsg(1, "I", "onCreate entered");
		
		mWifiMgr=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);

		mGp.panComponentName = new ComponentName(mContext, WidgetProviderPan.class);
		mGp.a2dpComponentName = new ComponentName(mContext, WidgetProviderA2dp.class);
		mGp.svcHandler = new Handler();
		mGp.callBackList = new RemoteCallbackList<IServiceCallback>();
		mBluetoothReceiver = new BluetoothReceiver();
		mSleepReceiver = new SleepReceiver();
		
		mGp.mBmA2dpOff=BitmapFactory.decodeResource(getResources(), R.drawable.a2dp_off);
		mGp.mBmA2dpOn=BitmapFactory.decodeResource(getResources(), R.drawable.a2dp_on);
		mGp.mBmA2dpOnOff=BitmapFactory.decodeResource(getResources(), R.drawable.a2dp_off_on);
		mGp.mBmPanOff=BitmapFactory.decodeResource(getResources(), R.drawable.pan_off);
		mGp.mBmPanOn=BitmapFactory.decodeResource(getResources(), R.drawable.pan_on);
		mGp.mBmPanOnOff=BitmapFactory.decodeResource(getResources(), R.drawable.pan_off_on);
		mGp.mBmAdapterOff=BitmapFactory.decodeResource(getResources(), R.drawable.device_bluetooth_off);
		mGp.mBmAdapterOn=BitmapFactory.decodeResource(getResources(), R.drawable.device_bluetooth_on);
		mGp.mBmAdapterOnOff=BitmapFactory.decodeResource(getResources(), R.drawable.device_bluetooth_on_off);

		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (WidgetUtil.isBluetoothTetherAvailable(mContext, mLog)) {
					if (mGp.settingsBtAutoEnabledAdapterOn) {
						boolean cs=WidgetUtil.panIsTeringOn(mGp);
						if (!cs) {
							mGp.setBluetoothTetheringEnabled(mGp.appContext, true);
							WidgetUtil.panSetBluetoothTering(mGp, true);
						}
					}
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		setProxyServiceListener(mGp, ntfy);
		WidgetServicePan.initialyzePanWidget(mGp);
		WidgetServiceHsp.initialyzeHspWidget(mGp);
		WidgetServiceA2dp.initialyzeA2dpWidget(mGp);
		WidgetServiceBtAdapter.initialyzeAdapterWidget(mGp);
		startBasicEventReceiver(mGp);
		setHeartBeat(mContext);

//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		ntfy.setListener(new NotifyEventListener(){
//			@Override
//			public void positiveResponse(Context c, Object[] o) {
//				WidgetServicePan.initialyzePanWidget(mGp);
//				WidgetServiceHsp.initialyzeHspWidget(mGp);
//				WidgetServiceA2dp.initialyzeA2dpWidget(mGp);
//				WidgetServiceBtAdapter.initialyzeAdapterWidget(mGp);
//				startBasicEventReceiver(mGp);
//				startHeartBeat(mContext);
//			}
//			@Override
//			public void negativeResponse(Context c, Object[] o) {}
//		});
//		if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//			if (Build.VERSION.SDK_INT>=21) setProxyServiceListener(mGp, ntfy);
//			else {
//				setProxyServiceListener(mGp, null);
//				ntfy.notifyToListener(true, null);
//			}
//		} else ntfy.notifyToListener(true, null);
		mLog.addDebugMsg(1, "I", "onCreate exited");
	};

	private LinkedList<Intent> mIntentQueue=new LinkedList<Intent>();
	@SuppressLint("Wakelock")
	@Override
	public int onStartCommand(final Intent in, int flags, int startId) {
		if (in!=null) {
			synchronized(mIntentQueue) {
				mIntentQueue.add(in); 
			}
			if (mGp.btAdapter.isEnabled() &&
					(!mGp.isPanAvailable||!mGp.isA2dpAvailable||!mGp.isHspAvailable)) {
				mLog.addDebugMsg(1, "I","Intent was queded, action="+in.getAction());
			} else {
				synchronized(mIntentQueue) {
					if (!mIntentQueue.isEmpty()) {
//						mLog.addDebugMsg(1, "I", "Process Queued Intent");
						while(!mIntentQueue.isEmpty()) {
							Intent q_in=mIntentQueue.poll();
							processIntent(q_in);
						}
					}
				}
//
//				mLog.addDebugMsg(1, "I", "Intent is processing immediately");
//				processIntent(in);
			}
		}
		return START_STICKY;
	};

	private void processIntent(Intent in) {
		String t_act = "";
		int[] tmp_widget_id_array = null;
		int tmp_widget_id=0;
		if (in != null && in.getAction() != null) {
			t_act = in.getAction();
			tmp_widget_id_array = in.getIntArrayExtra(WIDGET_ARRAY_ID_KEY);
			tmp_widget_id = in.getIntExtra(WIDGET_ID_KEY,0);
		} else
			t_act = "";
		final String action = t_act;
		final int[] widget_id_array = tmp_widget_id_array;
		final int widget_id=tmp_widget_id;
		mLog.addDebugMsg(1, "I", "onStartCommand entered, action=", action);
		if (!action.equals("")) {
			if (action.equals(WIDGET_SERVICE_REFRESH)) {
				mGp.loadSettingParms(mContext);
			} else if (action.equals(BROADCAST_SERVICE_HEARTBEAT)) {
				cancelHeartBeat(mContext);
				setHeartBeat(mContext);
				synchronized(mIntentQueue) {
					if (mIntentQueue.isEmpty()) mIntentQueue=new LinkedList<Intent>();
				}
			} else if (action.equals(BROADCAST_DISCONNECT_UNPLAYING_DEVICE)) {
				WidgetUtil.cancelA2dpDisconnectTimer(mGp);
				String disc_dev_name=in.getStringExtra(DISCONNECT_DEVICE_NAME_KEY);
				if (disc_dev_name!=null && !disc_dev_name.equals("")) {
					BluetoothDevice bd=WidgetUtil.getBluetoothDeviceByName(mGp, disc_dev_name);
					if (bd!=null) {
						if (!WidgetUtil.a2dpIsA2dpPlaying(mGp, bd)) {
							WidgetUtil.a2dpDisconnect(mGp, bd);
							WidgetUtil.hspDisconnect(mGp, bd);
							WidgetUtil.removeUnplayingList(mGp, disc_dev_name,
									mGp.unplayingDeviceList);
						}
					}
				}
//				WidgetUtil.scheduleA2dpDisconnectTimer(mGp, mGp.unplayingDeviceList);
			} else {
		    	if (action.startsWith(HOME_SCREEN_PAN_DEV_BTN_PREFIX)) {
		    		WidgetServicePan.processPanButtonIntent(mGp,action, in, widget_id);
		    		if (WidgetUtil.isIdentifyWidgetEnabled(mGp)) {
						WidgetUtil.setIdentifyWidget(mGp, false);
						WidgetServiceA2dp.setA2dpButtonIcon(mGp);
						WidgetServicePan.setPanButtonIcon(mGp);
		    		}
		    	} else if (action.startsWith(HOME_SCREEN_A2DP_DEV_BTN_PREFIX)) {
		    		WidgetServiceA2dp.processA2dpButtonIntent(mGp,action, in, widget_id);
		    		if (WidgetUtil.isIdentifyWidgetEnabled(mGp)) {
						WidgetUtil.setIdentifyWidget(mGp, false);
						WidgetServiceA2dp.setA2dpButtonIcon(mGp);
						WidgetServicePan.setPanButtonIcon(mGp);
		    		}
		    	} else if (action.startsWith(HOME_SCREEN_ADAPTER_BTN_PREFIX)) {
		    		WidgetServiceBtAdapter.processAdapterButtonIntent(mGp,action, in);
		    	} else if (action.startsWith(WIDGET_PAN_DEV_PREFIX)) {
		    		WidgetServicePan.processPanWidgetIntent(mGp, action, widget_id_array);
		    	} else if (action.startsWith(WIDGET_A2DP_DEV_PREFIX)) {
		    		WidgetServiceA2dp.processA2dpWidgetIntent(mGp,action, widget_id_array);
		    	} else if (action.startsWith(WIDGET_ADAPTER_PREFIX)) {
		    		WidgetServiceBtAdapter.processAdapterWidgetIntent(mGp,action, in);
		    	}
		    	checkTerminate();
			} 
		}
	};
	
	private void checkTerminate() {
		if (!WidgetServicePan.isPanRemoteViewExists(mGp) && 
				!WidgetServiceA2dp.isA2dpRemoteViewExists(mGp) && 
				!WidgetServiceBtAdapter.isAdapterRemoteViewExists(mGp)) {
			mLog.addDebugMsg(1, "I", "No AppWidget exits, service will be terminated");
			mGp.svcHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					stopSelf();
				}
			}, 1000);
		}
	};
	
	@Override
	public IBinder onBind(Intent in) {
		mLog.addDebugMsg(1, "I", "onBind entered");
		// String action="";
		// if (in!=null && in.getAction()!=null) action=in.getAction();
		return mSvcServiceClient;
	};

	@Override
	public boolean onUnbind(Intent in) {
		mLog.addDebugMsg(1, "I", "onUnbind entered");
		checkTerminate();
		return true;
	};

	@Override
	public void onDestroy() {
		mLog.addDebugMsg(1, "I", "onDestroy entered");
		WidgetServicePan.removePanWidget(mGp);
		WidgetServiceA2dp.removeA2dpWidget(mGp);
		WidgetServiceBtAdapter.removeAdapterWidget(mGp);
		unsetProxyServiceListener(mGp);
		stopBasicEventReceiver(mGp);
		cancelHeartBeat(mContext);
		Handler hndl = new Handler();
		hndl.postDelayed(new Runnable() {
			@Override
			public void run() {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}, 100);
	};

	final static private IServiceServer.Stub mSvcServiceClient = new IServiceServer.Stub() {
		final public void setCallBack(final IServiceCallback callback)
				throws RemoteException {
			mLog.addDebugMsg(2, "I", "setCallBack entered");
			mGp.callBackList.register(callback);
		};

		final public void removeCallBack(IServiceCallback callback)
				throws RemoteException {
			mLog.addDebugMsg(2, "I", "removeCallBack entered");
			mGp.callBackList.unregister(callback);
		};

		final public void applySettingParms() throws RemoteException {
			mLog.addDebugMsg(2, "I", "applySettingParms entered");

			mGp.loadSettingParms(mContext);
			
			mLog.resetLogReceiver();
			
			if (WidgetUtil.isBluetoothTetherAvailable(mContext, mLog))
				WidgetUtil.panSetBluetoothTering(mGp, mGp.settingsBtEnabled);
		};

		final public void updateA2dpDeiveNameByWidgetId(int wid, 
				String dev_name, boolean auto_conn) throws RemoteException {
			mLog.addDebugMsg(2, "I", "updateA2dpDeiveNameByWidgetId entered,"+
				" id="+wid+", dev="+dev_name+", auto="+auto_conn);

			WidgetListItem wli=
				WidgetUtil.getWidgetItemByWidgetId(mGp.a2dpWidgetTableList, wid);
			if (auto_conn) WidgetUtil.resetAutoConnect(mGp.a2dpWidgetTableList);
			if (wli!=null) {
				wli.device_name=dev_name;
				wli.auto_connect_adapter_on=auto_conn;
				List<BluetoothDevice> bdl=WidgetUtil.a2dpGetConnectedDevices(mGp);
				if (bdl!=null) {
					for(BluetoothDevice li:bdl) {
						if (li.getName().equals(dev_name)) {
							wli.connection_status=BluetoothProfile.STATE_CONNECTED;
							break;
						}
					}
				}
				WidgetUtil.saveA2dpWidgetTable(mGp,mGp.a2dpWidgetTableList, mLog);
				WidgetServiceA2dp.setA2dpButtonIcon(mGp);
			}
			
		};

		final public void updatePanDeiveNameByWidgetId(int wid, String dev_name,
				boolean auto_conn) throws RemoteException {
			mLog.addDebugMsg(2, "I", "updatePanDeiveNameByWidgetId entered, dev="+dev_name);

			String prev_name="";
			WidgetListItem wli=WidgetUtil.getWidgetItemByWidgetId(mGp.panWidgetTableList, wid);
			if (auto_conn) WidgetUtil.resetAutoConnect(mGp.panWidgetTableList);
			if (wli!=null) {
				if (auto_conn) {
					for (int i=0;i<mGp.panWidgetTableList.size();i++)
						mGp.panWidgetTableList.get(i).auto_connect_adapter_on=false;
				}
				prev_name=wli.device_name;
				BluetoothDevice pbd=
						WidgetUtil.getBluetoothDeviceByName(mGp, prev_name);
				if (pbd!=null) WidgetUtil.panDisconnect(mGp, pbd);
				wli.device_name=dev_name;

				for (int i=0;i<mGp.panWidgetTableList.size();i++) {
					wli=mGp.panWidgetTableList.get(i);
					if (wli.device_name.equals(dev_name)) {
						wli.auto_connect_adapter_on=auto_conn;
					}
				}
				WidgetUtil.savePanWidgetTable(mGp, mGp.panWidgetTableList, mLog);
				BluetoothDevice nbd=WidgetUtil.getBluetoothDeviceByName(mGp, dev_name);
				int cs=WidgetUtil.panGetConnectionState(mGp, nbd);
				for (int i=0;i<mGp.panWidgetTableList.size();i++) {
					mGp.panWidgetTableList.get(i).connection_status=cs;
				}
				WidgetServicePan.setPanButtonIcon(mGp);
			}
		};

		final public byte[] getA2dpWidgetTable() throws RemoteException{
			mLog.addDebugMsg(2, "I", "getA2dpWidgetTable entered");
			byte[] result=null;
			try {
				ByteArrayOutputStream bos=new ByteArrayOutputStream(4096*4);
				ObjectOutputStream oos=new ObjectOutputStream(bos);
				SerializeUtil.writeArrayList(oos, mGp.a2dpWidgetTableList);
				oos.flush();
				bos.flush();
				result=bos.toByteArray();
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		};
		
		final public byte[] getPanWidgetTable() throws RemoteException{
			mLog.addDebugMsg(2, "I", "getPanWidgetTable entered");
			byte[] result=null;
			try {
				ByteArrayOutputStream bos=new ByteArrayOutputStream(4096*4);
				ObjectOutputStream oos=new ObjectOutputStream(bos);
				SerializeUtil.writeArrayList(oos, mGp.panWidgetTableList);
				oos.flush();
				bos.flush();
				result=bos.toByteArray();
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		};
		
		final public boolean isBluetoothTetheringOn() {
			return WidgetUtil.panIsTeringOn(mGp);
		};
		
		final public void setBluetoothTethering(boolean enabled) {
			WidgetUtil.panSetBluetoothTering(mGp, enabled);
		};
		
		final public void setIdentifyWidget(boolean enabled) {
			WidgetUtil.setIdentifyWidget(mGp, enabled);
			WidgetServiceA2dp.setA2dpButtonIcon(mGp);
			WidgetServicePan.setPanButtonIcon(mGp);
		};

	};
	
    @SuppressLint("NewApi")
	final static private void setHeartBeat(Context context) {
    	mLog.addDebugMsg(1, "I", "startHeartBeat entered");
    	Intent iw = new Intent(context,WidgetService.class);
		iw.setAction(BROADCAST_SERVICE_HEARTBEAT);
		long time=System.currentTimeMillis()+mGp.settingsHeartBeatInterval;
		PendingIntent piw = PendingIntent.getService(context, 0, iw,
				PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager amw = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    if (Build.VERSION.SDK_INT>=23) amw.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, piw);
	    else amw.set(AlarmManager.RTC_WAKEUP, time, piw);

//	    amw.setRepeating(AlarmManager.RTC_WAKEUP, time, mGp.settingsHeartBeatInterval,piw);
    };

	final static private void cancelHeartBeat(Context context) {
    	Intent iw = new Intent(context,WidgetService.class);
		iw.setAction(BROADCAST_SERVICE_HEARTBEAT);
		PendingIntent piw = PendingIntent.getService(context, 0, iw,
				PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager amw = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    amw.cancel(piw);
    };

	final static private void startBasicEventReceiver(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "startBasicEventReceiver entered");
		IntentFilter intent = null;

		intent = new IntentFilter();
		intent.addAction(Intent.ACTION_SCREEN_OFF);
		intent.addAction(Intent.ACTION_SCREEN_ON);
		intent.addAction(Intent.ACTION_USER_PRESENT);
		gp.appContext.registerReceiver(mSleepReceiver, intent);

		intent = new IntentFilter();
//		intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//		intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//		intent.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
//		intent.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//		intent.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		
//		intent.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intent.addAction(BluetoothDevice.ACTION_FOUND);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//		intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);

		intent.addAction(BluetoothProfile_PAN_ACTION_CONNECTION_STATE_CHANGED);
		
		intent.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		intent.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
//		intent.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
//		intent.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
//		intent.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);

		gp.appContext.registerReceiver(mBluetoothReceiver, intent);
		
//  		intent = new IntentFilter();
//  		intent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//  		intent.addAction(WifiManager.RSSI_CHANGED_ACTION);
//  		intent.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//		intent.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//		intent.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
//		
//		gp.appContext.registerReceiver(mWifiReceiver, intent);

	};

	final static private void stopBasicEventReceiver(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "stopBasicEventReceiver entered");
		gp.appContext.unregisterReceiver(mSleepReceiver);
		gp.appContext.unregisterReceiver(mBluetoothReceiver);
//		gp.appContext.unregisterReceiver(mWifiReceiver);
	};

	static final private void notifyBtAdapterOff(GlobalParameters gp) {
		synchronized (gp.callBackList) {
			int on = gp.callBackList.beginBroadcast();
			if (on != 0) {
				IServiceCallback isv = null;
				for (int i = 0; i < on; i++) {
					try {
						isv = gp.callBackList.getBroadcastItem(i);
						isv.notifyToClientBtAdapterOff();
					} catch (RemoteException e) {
						e.printStackTrace();
						gp.log.addLogMsg("E", "notifyBtAdapterOff error, num=",
								String.valueOf(on), "\n", e.toString());
					}
				}
				gp.callBackList.finishBroadcast();
			}
		}
	};

	static final private void notifyBtAdapterOn(GlobalParameters gp) {
		synchronized (gp.callBackList) {
			int on = gp.callBackList.beginBroadcast();
			if (on != 0) {
				IServiceCallback isv = null;
				for (int i = 0; i < on; i++) {
					try {
						isv = gp.callBackList.getBroadcastItem(i);
						isv.notifyToClientBtAdapterOn();
					} catch (RemoteException e) {
						e.printStackTrace();
						gp.log.addLogMsg("E", "notifyBtAdapterOn error, num=",
								String.valueOf(on), "\n", e.toString());
					}
				}
				gp.callBackList.finishBroadcast();
			}
		}
	};

	static final private void notifyDeviceConnected(
			GlobalParameters gp, String dev_type, String dev_name) {
		synchronized (gp.callBackList) {
			int on = gp.callBackList.beginBroadcast();
			if (on != 0) {
				IServiceCallback isv = null;
				for (int i = 0; i < on; i++) {
					try {
						isv = gp.callBackList.getBroadcastItem(i);
						isv.notifyToClientDeviceConnected(dev_type,dev_name);
					} catch (RemoteException e) {
						e.printStackTrace();
						gp.log.addLogMsg("E", "notifyDeviceConnected error, num=",
								String.valueOf(on), "\n", e.toString());
					}
				}
				gp.callBackList.finishBroadcast();
			}
		}
	};

	static final private void notifyDeviceDisconnected(
			GlobalParameters gp, String dev_type, String dev_name) {
		synchronized (gp.callBackList) {
			int on = gp.callBackList.beginBroadcast();
			if (on != 0) {
				IServiceCallback isv = null;
				for (int i = 0; i < on; i++) {
					try {
						isv = gp.callBackList.getBroadcastItem(i);
						isv.notifyToClientDeviceDisconnected(dev_type, dev_name);
					} catch (RemoteException e) {
						e.printStackTrace();
						gp.log.addLogMsg("E", "notifyDeviceDisconnected error, num=",
								String.valueOf(on), "\n", e.toString());
					}
				}
				gp.callBackList.finishBroadcast();
			}
		}
	};
	
	final public static String getWifiSsidName(WifiManager wm) {
		String wssid="";
		if (wm.isWifiEnabled()) {
			String tssid=wm.getConnectionInfo().getSSID();
			if (tssid==null || tssid.equals("<unknown ssid>")) wssid="";
			else wssid=tssid.replaceAll("\"", "");
			if (wssid.equals("0x")) wssid="";
		}
		return wssid;
	};

    final static private class WifiReceiver  extends BroadcastReceiver {
		@SuppressLint({ "InlinedApi", "NewApi" })
		@Override
		final public void onReceive(Context c, Intent in) {
			String tssid=getWifiSsidName(mWifiMgr);
			String tmac=mWifiMgr.getConnectionInfo().getBSSID();
			String ss=mWifiMgr.getConnectionInfo().getSupplicantState().toString();
//			String action=in.getAction();
//			NetworkInfo ni=in.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
//			if (ni!=null) {
//				if (mEnvParms.wifiSsidName.equals("") && ni.getState().equals(NetworkInfo.State.CONNECTED)){
//					tssid=EnvironmentParms.WIFI_DIRECT_SSID;
//					ss="COMPLETED";
//				} else if (mEnvParms.wifiSsidName.equals(EnvironmentParms.WIFI_DIRECT_SSID) && ni.getState().equals(NetworkInfo.State.DISCONNECTED)){
//					tssid="";
//					ss="DISCONNECTED";
//				}
//			}
			
	        ConnectivityManager cm =
	                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo ani = cm.getActiveNetworkInfo();
	        boolean cm_connect=false;
            if (ani != null
                && ani.getType() == ConnectivityManager.TYPE_WIFI) {
            	cm_connect=true;
            }
			
			boolean new_wifi_enabled=mWifiMgr.isWifiEnabled();
			mLog.addDebugMsg(2,"I","WIFI receiver " +"Action="+in.getAction()+
					", SupplicantState="+ss+
					", new_wifi_enabled="+new_wifi_enabled+
					", cm_connect="+cm_connect+
					", tssid="+tssid+", SSID addr="+tmac);
			if (new_wifi_enabled ) {
				mLog.addDebugMsg(1,"I","WIFI receiver, WIFI On");
				//Off
			} else {
				if (ss.equals("COMPLETED")  
//						|| ss.equals("ASSOCIATING") 
//						|| ss.equals("ASSOCIATED") 
						) {
					if (!tssid.equals("") && tmac!=null) {
						//Connected
						if (mGp.settingsPanDisconnectWifiConnected) {
							for (int i=0;i<mGp.panWidgetTableList.size();i++) {
								WidgetListItem cwt=mGp.panWidgetTableList.get(i);
								if (cwt.connection_status==BluetoothProfile.STATE_CONNECTED) {
									BluetoothDevice device = 
											WidgetUtil.getBluetoothDeviceByName(mGp, cwt.device_name);
									WidgetUtil.panDisconnect(mGp, device);
									mGp.panBtnEnabled = true;
									WidgetServicePan.setPanButtonIcon(mGp);
								}
							}
						}
					}
				} else if (ss.equals("INACTIVE") ||
						ss.equals("DISCONNECTED") ||
						ss.equals("UNINITIALIZED") ||
						ss.equals("INTERFACE_DISABLED") ||
						ss.equals("SCANNING")) {
					//DisConnected							
				}
			}
		}
    };


	final static private class SleepReceiver extends BroadcastReceiver {
		@SuppressLint("Wakelock")
		@Override
		final public void onReceive(Context c, Intent in) {
			String action = in.getAction();
			mLog.addDebugMsg(2, "I","Sleep receiver entered,"+
					" screenIsLocked="+mGp.screenIsLocked+
					", action=", action);
			if (action.equals(Intent.ACTION_SCREEN_ON)) {
				boolean kge = WidgetUtil
						.isKeyguardEffective(mContext);
				if (mGp.screenIsLocked) {
					if (!kge) {// Screen unlocked
						mGp.screenIsLocked = false;
					}
				} else {
					if (kge) {// Screen locked
						mGp.screenIsLocked = true;
						processScreenLocked(mGp);
					}
				}
			} else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				boolean kge = WidgetUtil
						.isKeyguardEffective(mContext);
				if (kge) {// Screen locked
					mGp.screenIsLocked = true;
					processScreenLocked(mGp);
				} else {
					mGp.screenIsLocked = false;
					processScreenLocked(mGp);
				}
			} else if (action.equals(Intent.ACTION_USER_PRESENT)) {
				mGp.screenIsLocked = false;
	    		if (WidgetUtil.isIdentifyWidgetEnabled(mGp)) {
					WidgetUtil.setIdentifyWidget(mGp, false);
					WidgetServiceA2dp.setA2dpButtonIcon(mGp);
					WidgetServicePan.setPanButtonIcon(mGp);
	    		}
			}
		}
	};

	private static void processScreenLocked(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "disconnectBluetoothDevice entered,"
				+ " PanDisconnect="+ gp.settingsPanDisconnectScreenLocked
				+ ", A2dpDisconnect="+ gp.settingsA2dpDisconnectScreenLocked);
		
		//PAN
		if (gp.settingsPanDisconnectScreenLocked) {
			for (int i=0;i<gp.panWidgetTableList.size();i++) {
				WidgetListItem wli=gp.panWidgetTableList.get(i);
				if (wli.connection_status == BluetoothProfile.STATE_CONNECTED) {
					gp.log.addDebugMsg(1, "I", "PAN disconnected by screen locked, name="+wli.device_name);
					BluetoothDevice bd = WidgetUtil.getBluetoothDeviceByName(gp, wli.device_name);
					WidgetUtil.panDisconnect(gp, bd);
					wli.connection_status = BluetoothProfile.STATE_DISCONNECTING;
				}
			}
		}
		
		//A2DP
		if (gp.settingsA2dpDisconnectScreenLocked) {
			for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
				WidgetListItem wli=gp.a2dpWidgetTableList.get(i);
				if (wli.connection_status == BluetoothProfile.STATE_CONNECTED) {
					gp.log.addDebugMsg(1, "I", "A2DP disconnected by screen locked, name="+wli.device_name);
					BluetoothDevice bd = WidgetUtil.getBluetoothDeviceByName(gp, wli.device_name);
					WidgetUtil.a2dpDisconnect(gp, bd);
					WidgetUtil.hspDisconnect(gp, bd);
					wli.connection_status = BluetoothProfile.STATE_DISCONNECTING;
				}
			}
		}
	};

	final static private class BluetoothReceiver extends BroadcastReceiver {
		@Override
		final public void onReceive(Context c, Intent in) {
			String action = in.getAction();
			mLog.addDebugMsg(2, "I",
					"Bluetooth receiver entered, action=", action);
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int bs = BluetoothAdapter.getDefaultAdapter().getState();
				if (bs == BluetoothAdapter.STATE_OFF) {// BT Off
					processBtAdapterOff(mGp);
				} else if (bs == BluetoothAdapter.STATE_ON) {// BT On
					processBtAdapterOn(mGp);
				}
//			} else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
			} else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {				
				BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int ecs = in.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
				int pcs = in.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, -1);
				mLog.addDebugMsg(1, "I",
						"BluetoothA2dp Connection state changed, device="+device.getName()+", ecs="+ecs+", pcs="+pcs);
				if (device != null) {
					if (ecs == BluetoothProfile.STATE_CONNECTED) {// Connected
						processDeviceConnected(mGp, device);
					} else if (ecs == BluetoothProfile.STATE_DISCONNECTED) {// Disconnected
						processDeviceDisconnected(mGp, device);
					}
				}
			} else if (action.equals(BluetoothProfile_PAN_ACTION_CONNECTION_STATE_CHANGED)) {				
				BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int ecs = in.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
				int pcs = in.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, -1);
				mLog.addDebugMsg(1, "I",
						"BluetoothPan Connection state changed, device="+device.getName()+", ecs="+ecs+", pcs="+pcs);
				if (device != null) {
					if (ecs == BluetoothProfile.STATE_CONNECTED) {// Connected
						processDeviceConnected(mGp, device);
					} else if (ecs == BluetoothProfile.STATE_DISCONNECTED) {// Disconnected
						processDeviceDisconnected(mGp, device);
					}
				}
			} else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
				BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int ecs = in.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
				int pcs = in.getIntExtra(BluetoothAdapter. EXTRA_PREVIOUS_CONNECTION_STATE, -1);
				mLog.addDebugMsg(1, "I",
						"BluetoothA2dp playing state changed, device="+
								device.getName()+", ecs="+ecs+", pcs="+pcs);
				processA2dpPlayingStateChanged(mGp, device, ecs, 
						mGp.unplayingDeviceList);
			} else {
				BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String dev_name="";
				if (device != null) dev_name=device.getName(); 
				if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {// Connected
					mLog.addDebugMsg(1, "I","ACL_CONNECTED, device="+dev_name);
					processDeviceConnected(mGp, device);
				} else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {// Disconnected
					mLog.addDebugMsg(1, "I","ACL_DISCONNECTED, device="+dev_name);
					processDeviceDisconnected(mGp, device);
				}
			}
		}
	};
	
	final private static void processA2dpPlayingStateChanged(GlobalParameters gp,
			BluetoothDevice bd, int cs,
			ArrayList<UnplayingDeviceListItem>tbl) {
		if (bd==null || 
				gp.settingsA2dpAutoDiscTimeUnplaying.equals("0")) return;
		WidgetUtil.removeUnplayingList(gp,bd.getName(),tbl);
		if (cs==BluetoothA2dp.STATE_PLAYING) {
			
		} else if (cs==BluetoothA2dp.STATE_NOT_PLAYING) {
			WidgetUtil.addUnplayingList(gp,bd.getName(),tbl);
		}
		WidgetUtil.cancelA2dpDisconnectTimer(gp);
		WidgetUtil.scheduleA2dpDisconnectTimer(gp,tbl);
	};

	final private static void processBtAdapterOn(final GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "Bluetooth adapter on detected");
		notifyBtAdapterOn(gp);
		gp.bluetoothIsActive = true;
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
//				if (!mGp.isA2dpAvailable) {
//					WidgetServicePan.initialyzePanWidget(mGp);
//					WidgetServiceHsp.initialyzeHspWidget(mGp);
//					WidgetServiceA2dp.initialyzeA2dpWidget(mGp);
//					WidgetServiceBtAdapter.initialyzeAdapterWidget(mGp);
//				}
				
				processBtAdapterOnPan(gp);
				processBtAdapterOnA2dp(gp);
				
				//Adapter
				gp.adapterBtnEnabled = true;
				WidgetServiceBtAdapter.setAdapterButtonIcon(mGp, false);
				
				//Bluetooth tethering設定
				if (WidgetUtil.isBluetoothTetherAvailable(mContext, mLog)) {
					if (gp.settingsBtAutoEnabledAdapterOn) {
						boolean cs=WidgetUtil.panIsTeringOn(gp);
						if (!cs) {
							gp.setBluetoothTetheringEnabled(gp.appContext, true);
							WidgetUtil.panSetBluetoothTering(gp, true);
						}
					}
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		
		if (!mGp.isA2dpAvailable) {
			setProxyServiceListener(mGp, ntfy);
		} else ntfy.notifyToListener(true, null);
	};

	final private static void processBtAdapterOnPan(final GlobalParameters gp) {
		boolean update_required=false;
		for (int i=0;i<gp.panWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.panWidgetTableList.get(i);
			if (cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
				gp.log.addDebugMsg(1, "I",
						"Bt adapter on process canceled, because PAN Device not selected, ID="+cwt.widget_id);
			} else {
				if (cwt.auto_connect_adapter_on || cwt.connect_required_if_bt_off) {
					BluetoothDevice device = 
							WidgetUtil.getBluetoothDeviceByName(gp, cwt.device_name);

					WidgetUtil.panConnect(gp, device);
//					cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
//					WidgetServiceA2dp.setA2dpButtonIcon(gp);
					cwt.connect_required_if_bt_off = false;
					update_required=true;
				} else {
					if (cwt.connection_status!=BluetoothProfile.STATE_DISCONNECTED)
						update_required=true;
					cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
					cwt.connect_required_if_bt_off=false;
//					WidgetServiceA2dp.setA2dpButtonIcon(mGp);
				}
			}
		}
		gp.panBtnEnabled = true;
		if (update_required) WidgetServicePan.setPanButtonIcon(mGp);
	};
	
	final private static void processBtAdapterOnA2dp(final GlobalParameters gp) {
		boolean update_required=false;
		gp.log.addDebugMsg(1, "I","Set A2DP device status when Bluetooth adapter on");
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
			if (cwt.device_name.equals(DEVICE_NAME_UNSELECTED)) {
				gp.log.addDebugMsg(1, "I",
						"Bt adapter on process canceled, because A2DP Device not selected, ID="+cwt.widget_id);
			} else {
				if (cwt.auto_connect_adapter_on
						|| cwt.connect_required_if_bt_off) {
					BluetoothDevice device = 
							WidgetUtil.getBluetoothDeviceByName(gp, cwt.device_name);

					if (device!=null) WidgetUtil.a2dpConnect(gp, device);
//					cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
//					WidgetServiceA2dp.setA2dpButtonIcon(gp);
					cwt.connect_required_if_bt_off = false;
					update_required=true;
				} else {
					if (cwt.connection_status!=BluetoothProfile.STATE_DISCONNECTED)
						update_required=true;
					cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
					cwt.connect_required_if_bt_off=false;
//					WidgetServiceA2dp.setA2dpButtonIcon(mGp);
				}
			}
		}
		gp.a2dpBtnEnabled = true;
		if (update_required) WidgetServiceA2dp.setA2dpButtonIcon(mGp);
	};

	final private static void processBtAdapterOff(GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "Bluetooth adapter off detected");
		gp.bluetoothIsActive = false;
		notifyBtAdapterOff(gp);
		
		//PAN
		gp.panBtnEnabled = true;
		for (int i=0;i<gp.panWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.panWidgetTableList.get(i);
			cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
		}
		WidgetServicePan.setPanButtonIcon(mGp);

		//A2DP
		gp.a2dpBtnEnabled = true;
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
			cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
		}
		WidgetServiceA2dp.setA2dpButtonIcon(mGp);
		
		//Adapter
		gp.adapterBtnEnabled = true;
		if (gp.adapterRemoteViews!=null) {
			WidgetServiceBtAdapter.setAdapterButtonIcon(mGp, false);
		}

	};

	final private static void processDeviceConnected(GlobalParameters gp,
			BluetoothDevice device) {
		gp.log.addDebugMsg(1, "I", "Bluetooth device was connected, dev=",
				device.getName());
		String dev_name="";
		if (device!=null) {
			dev_name=device.getName();
			boolean processed=false;
			//Update PAN Device
			for (int i=0;i<gp.panWidgetTableList.size();i++) {
				WidgetListItem wli=gp.panWidgetTableList.get(i);
				if (wli.device_name.equals(dev_name)) {
					notifyDeviceConnected(gp, DEVICE_TYPE_PAN, device.getName());
					wli.connection_status=BluetoothProfile.STATE_CONNECTED;
					processed=true;
					if (gp.btAdapter.getProfileConnectionState(BluetoothProfile_PAN)>0)
						gp.panBtnEnabled=true;
				}
			}
			if (processed) WidgetServicePan.setPanButtonIcon(mGp);
			
			//Update A2DP device
			if (!processed) {
				for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
					WidgetListItem wli=gp.a2dpWidgetTableList.get(i);
					if (wli.device_name.equals(dev_name)) {
						notifyDeviceConnected(gp, DEVICE_TYPE_A2DP, device.getName());
						wli.connection_status=BluetoothProfile.STATE_CONNECTED;
						gp.a2dpBtnEnabled=true;
						
						if (!WidgetUtil.a2dpIsA2dpPlaying(gp, device)) {
							WidgetUtil.addUnplayingList(gp,device.getName(),gp.unplayingDeviceList);
							WidgetUtil.cancelA2dpDisconnectTimer(gp);
							WidgetUtil.scheduleA2dpDisconnectTimer(gp, gp.unplayingDeviceList);
						}

					}
				}
				WidgetServiceA2dp.setA2dpButtonIcon(mGp);
			}
		}
	};


	final private static void processDeviceDisconnected(GlobalParameters gp,
			BluetoothDevice device) {
		gp.log.addDebugMsg(1, "I", "Bluetooth device was disconnected, device="+device.getName());
		String dev_name="";
		if (device!=null) {
			dev_name=device.getName();
			if (dev_name!=null) {
				boolean processed=false;
				//Update PAN Device
				for (int i=0;i<gp.panWidgetTableList.size();i++) {
					WidgetListItem wli=gp.panWidgetTableList.get(i);
					if (wli.device_name.equals(dev_name)) {
						notifyDeviceDisconnected(gp, DEVICE_TYPE_PAN, device.getName());
						wli.connection_status=BluetoothProfile.STATE_DISCONNECTED;
						processed=true;
						gp.panBtnEnabled=true;
					}
				}
				if (processed) WidgetServicePan.setPanButtonIcon(mGp);
				
				//Update A2DP device
				if (!processed) {
					for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
						WidgetListItem wli=gp.a2dpWidgetTableList.get(i);
						if (wli.device_name.equals(dev_name)) {
							notifyDeviceDisconnected(gp, DEVICE_TYPE_A2DP, device.getName());
							wli.connection_status=BluetoothProfile.STATE_DISCONNECTED;
							gp.a2dpBtnEnabled=true;
						}
					}
					WidgetServiceA2dp.setA2dpButtonIcon(mGp);
				}
			}
		}
	};

	private static String mProxyInitListener="prroxy";
    static final private void setProxyServiceListener(final GlobalParameters gp, final NotifyEvent ntfy) {
    	gp.btAdapter.getProfileProxy(gp.appContext, mHspProxyServiceListener, BluetoothProfile.HEADSET);
    	gp.btAdapter.getProfileProxy(gp.appContext, mPanProxyServiceListener, BluetoothProfile_PAN);
    	gp.btAdapter.getProfileProxy(gp.appContext, mA2dpProxyServiceListener, BluetoothProfile.A2DP);
    	Thread th=new Thread(){
    		@Override
    		public void run() {
    			while(!mGp.isA2dpAvailable || !mGp.isHspAvailable || !mGp.isPanAvailable) {
    				synchronized(mProxyInitListener) {
    					try {
							mProxyInitListener.wait();
//							Log.v("","hs="+mGp.isHspAvailable+", a2dp="+mGp.isA2dpAvailable+", pan="+mGp.isPanAvailable);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
    				}
//    				SystemClock.sleep(100);
    			}
    			if (ntfy!=null) ntfy.notifyToListener(true, null);
    			Intent in=new Intent(mContext, WidgetService.class);
    			in.setAction("init_proxy_completed");
    			mContext.startService(in);
    		}
    	};
    	th.start();
    };

    static final private void unsetProxyServiceListener(final GlobalParameters gp) { 
    	gp.btAdapter.closeProfileProxy(BluetoothProfile.A2DP, mGp.a2dpProxy);
    	gp.btAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mGp.hspProxy);
    	gp.btAdapter.closeProfileProxy(BluetoothProfile_PAN, mGp.panProxy);
    };

	private static BluetoothProfile.ServiceListener mA2dpProxyServiceListener=new BluetoothProfile.ServiceListener() {
	    @Override
	    public void onServiceDisconnected(int profile) {
	    	mLog.addDebugMsg(1, "I", "A2DP onServiceDisconnected entered, profile="+profile);
	    }
	    @Override
	    public void onServiceConnected(int profile, BluetoothProfile proxy) {
	    	mLog.addDebugMsg(1, "I", "A2DP onServiceConnected entered, profile="+profile);
	    	if (profile==BluetoothProfile.A2DP) {
	    		mGp.a2dpProxy=proxy;
		    	Method[] m=mGp.a2dpProxy.getClass().getMethods();
		    	for (int i=0;i<m.length;i++) {
			    	if (m[i].getName().equals("connect")) { 
			    		mGp.a2dp_dev_method_connect=m[i];
			    		mLog.addDebugMsg(1, "I", "A2DP Method connect detected");
			    	} else if (m[i].getName().equals("disconnect")) {
			    		mGp.a2dp_dev_method_disconnect=m[i];
			    		mLog.addDebugMsg(1, "I", "A2DP Method disconnect detected");
			    	} else if (m[i].getName().equals("getConnectionState")) {
			    		mGp.a2dp_dev_method_getConnectionState=m[i];
			    		mLog.addDebugMsg(1, "I", "A2DP Method getConnectionState detected");
			    	} else if (m[i].getName().equals("getConnectedDevices")) {
			    		mGp.a2dp_dev_method_getConnectedDevices=m[i];
			    		mLog.addDebugMsg(1, "I", "A2DP Method getConnectedDevices detected");
			    	} else if (m[i].getName().equals("isA2dpPlaying")) {
			    		mGp.a2dp_dev_method_isA2dpPlaying=m[i];
			    		mLog.addDebugMsg(1, "I", "A2DP Method isA2dpPlaying detected");
			    	} else {
			    		if (mGp.settingsDebugLevel==2)
			    			mLog.addDebugMsg(2, "I", "A2DP Unused method="+m[i].getName()); 
			    	}
		    	}
		    	mGp.isA2dpAvailable=true;
				synchronized(mProxyInitListener) {
					mProxyInitListener.notify();
				}
	    	 }
	    }
	};
	private static BluetoothProfile.ServiceListener mHspProxyServiceListener=new BluetoothProfile.ServiceListener() {
	    @Override
	    public void onServiceDisconnected(int profile) {
	    	mLog.addDebugMsg(1, "I", "HSP onServiceDisconnected entered, profile="+profile);
	    }
	    @Override
	    public void onServiceConnected(int profile, BluetoothProfile proxy) {
	    	mLog.addDebugMsg(1, "I", "HSP onServiceConnected entered, profile="+profile);
	    	if (profile==BluetoothProfile.HEADSET) {
	    		mGp.hspProxy=proxy;
	  	    	Method[] m=mGp.hspProxy.getClass().getMethods();
	  	    	for (int i=0;i<m.length;i++) {
			    	if (m[i].getName().equals("connect")) { 
			    		mGp.hsp_method_connect=m[i];
			    		mLog.addDebugMsg(1, "I", "HSP Method connect detected");
			    	} else if (m[i].getName().equals("disconnect")) {
			    		mGp.hsp_method_disconnect=m[i];
			    		mLog.addDebugMsg(1, "I", "HSP Method disconnect detected");
			    	} else if (m[i].getName().equals("getConnectionState")) {
			    		mGp.hsp_method_getConnectionState=m[i];
			    		mLog.addDebugMsg(1, "I", "HSP Method getConnectionState detected");
			    	} else if (m[i].getName().equals("getConnectedDevices")) {
			    		mGp.hsp_method_getConnectedDevices=m[i];
			    		mLog.addDebugMsg(1, "I", "HSP Method getConnectedDevices detected");
			    	} else {
			    		if (mGp.settingsDebugLevel==2)
			    			mLog.addDebugMsg(2, "I", "HSP Unused method="+m[i].getName()); 
			    	}
	  	    	}
	  	    	mGp.isHspAvailable=true;
				synchronized(mProxyInitListener) {
					mProxyInitListener.notify();
				}
	    	}
	    }
	};
	private static BluetoothProfile.ServiceListener mPanProxyServiceListener=new BluetoothProfile.ServiceListener() {
	    @Override
	    public void onServiceDisconnected(int profile) {
	    	mLog.addDebugMsg(1, "I", "PAN onServiceDisconnected entered, profile="+profile);
	    }
	    @Override
	    public void onServiceConnected(int profile, BluetoothProfile proxy) {
	    	mLog.addDebugMsg(1, "I", "PAN onServiceConnected entered, profile="+profile);
	    	if (profile==BluetoothProfile_PAN) {
	    		mGp.panProxy=proxy;
	  	    	Method[] m=mGp.panProxy.getClass().getMethods();
	  	    	for (int i=0;i<m.length;i++) {
	  	    		if (m[i].getName().equals("connect")) { 
			    		mGp.pan_method_connect=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method connect detected");
			    	} else if (m[i].getName().equals("disconnect")) {
			    		mGp.pan_method_disconnect=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method disconnect detected");
			    	} else if (m[i].getName().equals("getConnectionState")) {
			    		mGp.pan_method_getConnectionState=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method getConnectionState detected");
			    	} else if (m[i].getName().equals("isTetheringOn")) {
			    		mGp.pan_method_isTetheringOn=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method isTetheringOn detected");
			    	} else if (m[i].getName().equals("setBluetoothTethering")) {
			    		mGp.pan_method_setBluetoothTethering=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method setBluetoothTethering detected");
			    	} else if (m[i].getName().equals("getConnectedDevices")) {
			    		mGp.pan_method_getConnectedDevices=m[i];
			    		mLog.addDebugMsg(1, "I", "PAN Method getConnectedDevices detected");
			    	} else {
			    		if (mGp.settingsDebugLevel==2)
			    			mLog.addDebugMsg(2, "I", "PAN Unused method="+m[i].getName()); 
			    	}
	  	    	}
	  	    	mGp.isPanAvailable=true;
				synchronized(mProxyInitListener) {
					mProxyInitListener.notify();
				}
	    	}
	    }
	};
}
