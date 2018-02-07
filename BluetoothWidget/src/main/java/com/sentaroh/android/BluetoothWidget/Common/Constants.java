package com.sentaroh.android.BluetoothWidget.Common;

public class Constants {
	public final static String APPLICATION_TAG="BluetoothWidget";
	public final static String PACKAGE_NAME="com.sentaroh.android.BluetoothWidget";
	public final static boolean WIDGET_DEBUG_ENABLE=true;
	
	public static final String DEFAULT_PREFS_FILENAME="default_preferences";
	
	public final static String WIFI_STATUS_KEY="wifi_status_key";
	public final static String WIFI_STATUS_ON="ON";
	public final static String WIFI_STATUS_OFF="OFF";
	
	public final static String WIDGET_ARRAY_ID_KEY="widget_id_array_key";
	public final static String WIDGET_ID_KEY="widget_id_key";
	public final static String WIDGET_SERVICE_REFRESH=
			PACKAGE_NAME+".WIDGET_SERVICE_REFRESH";
	
	public static final String BluetoothProfile_PAN_ACTION_CONNECTION_STATE_CHANGED =
	        "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
	public static final int BluetoothProfile_PAN = 5;

	public final static String DEVICE_TYPE_A2DP="A2DP";
	public final static String DEVICE_TYPE_HSP="HSP";
	public final static String DEVICE_TYPE_PAN="PAN";

	public final static int DEVICE_REFRESH_TIME_OUT=1000*10;
	
	public final static String DEVICE_NAME_UNSELECTED="*Unselected*";
	
	public final static String BONDED_DEVICE_LIST_FILE_NAME="dev_list.bin"; 
	
	public final static String CURRENT_PAN_DEVICE_NAME_KEY="current_pan_device_name";

	public final static String WIDGET_PAN_DEV_PREFIX="WIDGET_PAN_DEV_";
	public final static String WIDGET_PAN_DEV_UPDATE="WIDGET_PAN_DEV_UPDATE";
	public final static String WIDGET_PAN_DEV_DELETED="WIDGET_PAN_DEV_DELETED";
	public final static String WIDGET_PAN_DEV_DISABLE="WIDGET_PAN_DEV_DISABLE";
	public final static String WIDGET_PAN_DEV_ENABLE="WIDGET_PAN_DEV_ENABLE";

	public final static String HOME_SCREEN_PAN_DEV_BTN_PREFIX=
			PACKAGE_NAME+".HOME_SCREEN_PAN_DEV";
	public final static String HOME_SCREEN_PAN_TOGGLE_BTN=
			PACKAGE_NAME+".HOME_SCREEN_PAN_DEV_TOGGLE_BTN";

	public final static String CURRENT_A2DP_DEVICE_NAME_KEY="current_a2dp_device_name";
	
	public final static String WIDGET_A2DP_DEV_PREFIX="WIDGET_A2DP_DEV_";
	public final static String WIDGET_A2DP_DEV_UPDATE="WIDGET_A2DP_DEV_UPDATE";
	public final static String WIDGET_A2DP_DEV_DELETED="WIDGET_A2DP_DEV_DELETED";
	public final static String WIDGET_A2DP_DEV_DISABLE="WIDGET_A2DP_DEV_DISABLE";
	public final static String WIDGET_A2DP_DEV_ENABLE="WIDGET_A2DP_DEV_ENABLE";

	public final static String HOME_SCREEN_A2DP_DEV_BTN_PREFIX=
			PACKAGE_NAME+".HOME_SCREEN_A2DP_DEV";
	public final static String HOME_SCREEN_A2DP_DEV_TOGGLE_BTN=
			PACKAGE_NAME+".HOME_SCREEN_A2DP_DEV_TOGGLE_BTN";

	public final static String WIDGET_ADAPTER_PREFIX="WIDGET_ADAPTER_";
	public final static String WIDGET_ADAPTER_UPDATE="WIDGET_ADAPTER_UPDATE";
	public final static String WIDGET_ADAPTER_DELETED="WIDGET_ADAPTER_DELETED";
	public final static String WIDGET_ADAPTER_DISABLE="WIDGET_ADAPTER_DISABLE";
	public final static String WIDGET_ADAPTER_ENABLE="WIDGET_ADAPTER_ENABLE";

	public final static String HOME_SCREEN_ADAPTER_BTN_PREFIX=
			PACKAGE_NAME+".HOME_SCREEN_ADAPTER";
	public final static String HOME_SCREEN_ADAPTER_TOGGLE_BTN=
			PACKAGE_NAME+".HOME_SCREEN_ADAPTER_TOGGLE_BTN";

	public static final String BROADCAST_SERVICE_HEARTBEAT=
			PACKAGE_NAME+".ACTION_SERVICE_HEARTBEAT";

	public static final String BROADCAST_DISCONNECT_UNPLAYING_DEVICE=
			PACKAGE_NAME+".ACTION_DISCONNECT_UNPLAYING_DEVICE";
	public static final String DISCONNECT_DEVICE_NAME_KEY="disc_device_name";

	public static final String BROADCAST_LOG_SEND=PACKAGE_NAME+".ACTION_LOG_SEND";
	public static final String BROADCAST_LOG_RESET=PACKAGE_NAME+".ACTION_LOG_RESET";
	public static final String BROADCAST_LOG_ROTATE=PACKAGE_NAME+".ACTION_LOG_ROTATE";
	public static final String BROADCAST_LOG_DELETE=PACKAGE_NAME+".ACTION_LOG_DELETE";
	public static final String BROADCAST_LOG_FLUSH=PACKAGE_NAME+".ACTION_LOG_FLUSH";
	public static final String BROADCAST_LOG_CLOSE=PACKAGE_NAME+".ACTION_LOG_CLOSE";

}
