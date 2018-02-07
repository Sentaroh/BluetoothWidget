package com.sentaroh.android.BluetoothWidget.Shortcut;

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

import com.sentaroh.android.BluetoothWidget.GlobalParameters;
import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.WidgetUtil;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

public class InvokeTetheringConfig extends FragmentActivity{
	
	private Context context;

    private int restartStatus=0;
    
    private LogUtil mLog=null;
    private GlobalParameters mGp=null;
    
	@Override  
	final protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
	};  
	  
	@Override  
	final protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		restartStatus=2;
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setContentView(R.layout.activity_transrucent);
        context=this;
        if (mGp==null) {
        	mGp=new GlobalParameters();
       		mGp.loadSettingParms(context);
        }
        mLog=new LogUtil(context, "TetheringConfig", mGp);
		
        mLog.addDebugMsg(1, "I", "onCreate entered restartStaus="+restartStatus);
        // Application process is follow
    };
    
	@Override
	public void onStart() {
		super.onStart();
        mLog.addDebugMsg(1, "I", "onStart entered restartStaus="+restartStatus);
	};

	@Override
	public void onRestart() {
		super.onStart();
        mLog.addDebugMsg(1, "I", "onRestart entered restartStaus="+restartStatus);
	};

	final public void onResume() {
		super.onResume();
        mLog.addDebugMsg(1, "I", "onResume entered restartStaus="+restartStatus);

        if (WidgetUtil.isTetherAvailable(this,mLog)) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.SubSettings");//.TetherSettings");
            startActivity(intent);
    		finish();
        } else {
        	NotifyEvent ntfy=new NotifyEvent(context);
        	ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					finish();
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
        	});
    		final CommonDialog commonDlg=new CommonDialog(context, getSupportFragmentManager());
    		commonDlg.showCommonDialog(false, "W", 
    				getString(R.string.msgs_invoke_tether_config_tether_not_available),
    				"", ntfy);
        }
		restartStatus=1;
	};

	@Override
	public void onPause() {
		super.onPause();
        mLog.addDebugMsg(1, "I", "onPause entered restartStaus="+restartStatus);
        // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
        mLog.addDebugMsg(1, "I", "onStop entered restartStaus="+restartStatus);
        // Application process is follow
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
        mLog.addDebugMsg(1, "I", "onDestroy entered restartStaus="+restartStatus);
        // Application process is follow
		System.gc();
	};
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	};
	
}
