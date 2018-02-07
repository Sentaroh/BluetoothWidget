package com.sentaroh.android.BluetoothWidget;

import android.os.SystemClock;

public class WidgetServiceHsp {
    static final public void initialyzeHspWidget(final GlobalParameters gp) {
    	gp.log.addDebugMsg(1, "I", "initialyzeHspWidget entered");

		if (gp.btAdapter.isEnabled()) {
			Thread th=new Thread() {
				@Override
				public void run() {
					for (int i=0;i<100;i++) {
						if (gp.isHspAvailable) break;
						SystemClock.sleep(50);
					}
				}
			};
			th.start();
		}
    };


}
