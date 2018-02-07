package com.sentaroh.android.BluetoothWidget;

import com.sentaroh.android.BluetoothWidget.IServiceCallback;

interface IServiceServer{
	
	void setCallBack(IServiceCallback callback);
	void removeCallBack(IServiceCallback callback);
	
	void applySettingParms();
	
	void updateA2dpDeiveNameByWidgetId(int wid, String dev_name, boolean auto_conn);
	byte[] getA2dpWidgetTable();
	
	void updatePanDeiveNameByWidgetId(int wid, String dev_name, boolean auto_conn);
	byte[] getPanWidgetTable();

	boolean isBluetoothTetheringOn();
	void setBluetoothTethering(boolean enabled);	
	
	void setIdentifyWidget(boolean enabled);
}