package com.sentaroh.android.BluetoothWidget;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sentaroh.android.BluetoothWidget.IServiceCallback;
import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.Common.UnplayingDeviceListItem;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.Utilities.CommonGlobalParms;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.widget.RemoteViews;

@SuppressLint("SdCardPath")
public class GlobalParameters extends CommonGlobalParms{
	
	public boolean initialyzeRequired=true;

	public boolean settingsDebugEnabled=true;
	public int settingsDebugLevel=1;
	public boolean settingsExitCleanly=true;
	
	public long settingsHeartBeatInterval=1000*60*3;
	
	public boolean settingsPanOnAdapterOff=false;//true;
	public boolean settingsPanDisconnectScreenLocked=false;
	public boolean settingsPanDisconnectWifiConnected=true;
	public boolean connectRequiredPanAdapterOn=false;
	
	public boolean settingsA2dpOnAdapterOff=false;//true;
//	public boolean settingsA2dpConnectAdapterOn=false;
	public boolean settingsA2dpDisconnectScreenLocked=false;
	public String  settingsA2dpAutoDiscTimeUnplaying="5";
	
	public boolean settingsBtEnabled=false;
	public boolean settingsBtAutoEnabledAdapterOn=false;
	
	public boolean settingsLogEnabled=true;
	public String logFileDir=null;
//	public String settingsLogTag="BluetoothWidget";
	public String settingsLogFileDir="/mnt/sdcard/BluetoothWidget/";
	public String settingsLogFileName="BluetoothWidget_log";
	public int settingsLogFileBufferSize=1024*32;
	public int settingsLogMaxFileCount=10;
	
	public Resources mainResources=null;
	
	public String homeDir="/mnt/sdcard/BluetoothWidget";
	
	public boolean bluetoothIsActive=false;
	
	public AppWidgetManager widgetManager=null;
	public Context appContext=null;
	public BluetoothAdapter btAdapter=null;
	public Handler svcHandler=null;
	public LogUtil log=null;
	public ReentrantReadWriteLock lockA2dpWidgetTable=new ReentrantReadWriteLock();
	public ReentrantReadWriteLock lockPanWidgetTable=new ReentrantReadWriteLock();
	
	public boolean screenIsLocked=false;
	
	public RemoteCallbackList<IServiceCallback>  callBackList=null;

	
	public ArrayList<WidgetListItem> panWidgetTableList=new ArrayList<WidgetListItem>();

	public boolean isPanAvailable=false;
	public Method pan_method_connect=null, pan_method_disconnect=null,
			pan_method_getConnectionState=null, pan_method_isTetheringOn=null,
			pan_method_setBluetoothTethering=null,
			pan_method_getConnectedDevices=null;
	public BluetoothProfile panProxy=null;
	public Bitmap mBmPanOnOff=null, mBmPanOn=null, mBmPanOff=null;
	
	public RemoteViews panRemoteViews=null;
	public ComponentName panComponentName=null;
	public boolean panBtnEnabled=true;
//	public String panCurrentDeviceName=DEVICE_NAME_UNSELECTED;
//	public int panConnectionStatus=0;

	public ArrayList<UnplayingDeviceListItem> unplayingDeviceList=
			new ArrayList<UnplayingDeviceListItem>();

	public ArrayList<WidgetListItem> a2dpWidgetTableList=new ArrayList<WidgetListItem>();

	public boolean isA2dpAvailable=false;
	public Method a2dp_dev_method_connect=null, a2dp_dev_method_disconnect=null,
			a2dp_dev_method_getConnectionState=null,
			a2dp_dev_method_getConnectedDevices=null,
			a2dp_dev_method_isA2dpPlaying=null;
	public BluetoothProfile a2dpProxy=null;
	public Bitmap mBmA2dpOnOff=null, mBmA2dpOn=null, mBmA2dpOff=null;
	
	public boolean isHspAvailable=false;
	public Method hsp_method_connect=null, hsp_method_disconnect=null,
			hsp_method_getConnectionState=null,
			hsp_method_getConnectedDevices=null;

	public BluetoothProfile hspProxy=null;

	public ComponentName a2dpComponentName=null;
	public boolean a2dpBtnEnabled=true;

	public RemoteViews adapterRemoteViews=null;
	public ComponentName adapterComponentName=null;
	public boolean adapterBtnEnabled=true;
	public Bitmap mBmAdapterOnOff=null, mBmAdapterOn=null, mBmAdapterOff=null;
	
	public GlobalParameters() {};
	
//	@Override
//	public void  onCreate() {
//		super.onCreate();
//		Log.v("GlobalParms","onCreate entered");
//	};
	
	@SuppressWarnings("deprecation")
	public void setBluetoothTetheringEnabled(Context c, boolean enabled) {
		SharedPreferences prefs = c.getSharedPreferences(DEFAULT_PREFS_FILENAME,
        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
		settingsBtEnabled=enabled;
		prefs.edit().putBoolean(c.getString(R.string.settings_bt_enabled),enabled).commit();
	};
	 
	@SuppressWarnings("deprecation")
	public void setLogOptionEnabled(Context c, boolean enabled) {
		SharedPreferences prefs = c.getSharedPreferences(DEFAULT_PREFS_FILENAME,
        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
		prefs.edit().putBoolean(c.getString(R.string.settings_logging_enabled),enabled).commit();
		settingsLogEnabled=enabled;
	};
	
	@SuppressWarnings("deprecation")
	public SharedPreferences getSharedPrefs(Context c) {
		return c.getSharedPreferences(DEFAULT_PREFS_FILENAME,
        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
	}
	
	public void loadSettingParms(Context c) {
		SharedPreferences prefs = getSharedPrefs(c);
		settingsLogEnabled=
				prefs.getBoolean(c.getString(R.string.settings_logging_enabled),false);
		settingsExitCleanly=
				prefs.getBoolean(c.getString(R.string.settings_exit_cleanly),false);
		String lv=prefs.getString(c.getString(R.string.settings_debug_level),"0");
		settingsDebugLevel=Integer.parseInt(lv);

		settingsPanOnAdapterOff=
				prefs.getBoolean(c.getString(R.string.settings_widget_pan_auto_on_adapter_off),false);
		settingsPanDisconnectScreenLocked=
				prefs.getBoolean(c.getString(R.string.settings_widget_pan_auto_disc_screen_locked),false);

		settingsA2dpOnAdapterOff=
				prefs.getBoolean(c.getString(R.string.settings_widget_a2dp_auto_on_adapter_off),false);
		settingsA2dpDisconnectScreenLocked=
				prefs.getBoolean(c.getString(R.string.settings_widget_a2dp_auto_disc_screen_locked),false);
		settingsA2dpAutoDiscTimeUnplaying=
				prefs.getString(c.getString(R.string.settings_widget_a2dp_auto_disc_time_unplaying),"0");
		
		if (settingsDebugLevel>0) settingsDebugEnabled=true;
		else settingsDebugEnabled=false;

		settingsBtEnabled=
				prefs.getBoolean(c.getString(R.string.settings_bt_enabled),false);
		settingsBtAutoEnabledAdapterOn=
				prefs.getBoolean(c.getString(R.string.settings_bt_auto_on_adapter_on),false);

//		if (settingsDebugEnabled) 
//			Log.v(APPLICATION_TAG,"loadSettingParms "+
//				"settingsDebugEnabled="+settingsDebugEnabled+
//				", settingsExitCleanly="+settingsExitCleanly+
//				", settingsDebugLevel="+settingsDebugLevel+
//				", settingsPanConnectAdapterOn="+settingsPanConnectAdapterOn+
//				", settingsPanOnAdapterOff="+settingsPanOnAdapterOff);
		setDebugLevel(settingsDebugLevel);
		setLogLimitSize(2*1024*1024);
		setLogMaxFileCount(settingsLogMaxFileCount);
		setLogEnabled(settingsLogEnabled);
		setLogDirName(settingsLogFileDir);
		setLogFileName(settingsLogFileName);
		setApplicationTag(APPLICATION_TAG);
		setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	};
	
	@SuppressWarnings({ "unused", "deprecation" })
	public void initSettingParms(Context c) {
		SharedPreferences prefs = c.getSharedPreferences(DEFAULT_PREFS_FILENAME,
        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
		String ed=Environment.getExternalStorageDirectory().toString();
	};
	
}
