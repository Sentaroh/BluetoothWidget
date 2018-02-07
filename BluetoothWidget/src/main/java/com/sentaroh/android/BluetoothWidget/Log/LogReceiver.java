package com.sentaroh.android.BluetoothWidget.Log;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;
import com.sentaroh.android.BluetoothWidget.GlobalParameters;
import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.LogUtil.CommonLogReceiver;

import android.content.Context;


public class LogReceiver extends CommonLogReceiver{
	@Override
	public void setLogParms(Context c, CommonGlobalParms gp) {
		GlobalParameters ep=new GlobalParameters();
		ep.loadSettingParms(c);
		
		gp.setDebugLevel(ep.settingsDebugLevel);
		gp.setLogLimitSize(2*1024*1024);
		gp.setLogMaxFileCount(ep.settingsLogMaxFileCount);
		gp.setLogEnabled(ep.settingsLogEnabled);
		gp.setLogDirName(ep.settingsLogFileDir);
		gp.setLogFileName(ep.settingsLogFileName);
		gp.setApplicationTag(APPLICATION_TAG);
		gp.setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	};
}
