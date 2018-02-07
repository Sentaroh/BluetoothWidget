package com.sentaroh.android.BluetoothWidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WidgetServiceReceiver extends BroadcastReceiver{
	private static WakeLock mWakeLock=null;
	@Override
	public void onReceive(Context context, Intent intent) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)context.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "TaskAutomation-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(100);
//		mWakeLock.acquire(100);
		
//		initSettingParms();
		String action=intent.getAction();
		if (action!=null) {
			Intent in = new Intent(context, WidgetService.class);
			in.setAction(action);
			context.startService(in);
		}
	}

}
