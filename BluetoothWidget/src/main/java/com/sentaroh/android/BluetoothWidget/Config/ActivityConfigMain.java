package com.sentaroh.android.BluetoothWidget.Config;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.sentaroh.android.BluetoothWidget.GlobalParameters;
import com.sentaroh.android.BluetoothWidget.IServiceCallback;
import com.sentaroh.android.BluetoothWidget.IServiceServer;
import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.WidgetProviderA2dp;
import com.sentaroh.android.BluetoothWidget.WidgetProviderPan;
import com.sentaroh.android.BluetoothWidget.WidgetUtil;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import com.sentaroh.android.BluetoothWidget.WidgetService;
import com.sentaroh.android.BluetoothWidget.IServiceServer.Stub;
import com.sentaroh.android.BluetoothWidget.Log.LogFileListDialogFragment;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.BluetoothWidget.Shortcut.InvokeTetheringConfig;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

@SuppressWarnings("unused")
public class ActivityConfigMain extends AppCompatActivity {

	private GlobalParameters mGp=null;
	
	private static int mRestartStatus=0;
	
	private Context mContext=null;
	private LogUtil mLog=null;
	
	private IServiceCallback mSvcClientCallback=null;
	private ServiceConnection mSvcConnScheduler=null;
	private IServiceServer mSvcServer=null;

	private CustomContextMenu mCcMenu=null;
	@Override  
	final protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onSaveInstanceState entered");
	};  
	  
	@Override  
	final protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onRestoreInstanceState entered");
		mRestartStatus=2;
	};
	
	final private String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};
	
	@Override
    final public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//    	StrictMode.enableDefaults();
//        if (Build.VERSION.SDK_INT<=10) requestWindowFeature(Window.FEATURE_NO_TITLE); 
        setContentView(R.layout.config_activity_main);
        
        setTitle(getString(R.string.app_name)+" Ver "+getApplVersionName());
        mContext=this;
//        mActivity=this;
        if (mGp==null) {
            mGp=(GlobalParameters)getApplication();
            if (mGp.initialyzeRequired) {
                mGp.loadSettingParms(mContext);
                mGp.initialyzeRequired=false;
            }
            mLog=new LogUtil(mContext.getApplicationContext(), "ConfigMain", mGp);
        }
        if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onCreate entered");
        
        mRestartStatus=0;

        mCcMenu=new CustomContextMenu(this.getResources(), getSupportFragmentManager());
        
		Intent intent = new Intent(mContext, WidgetService.class);
		intent.setAction("Main");
		startService(intent); 

    };
    
	@Override
	final public void onStart() {
		super.onStart();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onStart entered");
	};

	@Override
	final public void onRestart() {
		super.onStart();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onRestart entered");
	};
	
	@Override
	final public void onResume() {
		super.onResume();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onResume entered, restartStatus="+mRestartStatus);

		if (mRestartStatus==1) {
			resetIdentifyWidget();
			initGlblParmsByService();
			buildWidgetListView();
			mWidgetListAdapter.notifyDataSetChanged();
			mWidgetListView.invalidate();
			setWidgetListviewListener();
		} else {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					initGlblParmsByService();
					if (mRestartStatus==0) {
						buildWidgetListView();
					} else if (mRestartStatus==2) {
						buildWidgetListView();
					}
					setWidgetListviewListener();
					mRestartStatus=1;
				}

				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
				
			});
			bindWidgetService(ntfy);
		}
	};

	private ListView mWidgetListView=null;
	private AdapterWidgetList mWidgetListAdapter=null;
	
	private void setWidgetListviewListener() {
		mWidgetListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				WidgetListItem li=mWidgetListAdapter.getItem(position);
				if (li.widget_id==-1) return;
				if (li.widget_type.equals(WidgetListItem.WIDGET_TABLE_TYPE_PAN)) {
//					WidgetUtil.startPanConfigActivity(mGp, li.widget_id);
					Intent in_act=new Intent(mContext,ActivityConfigPan.class);
					in_act.putExtra(WIDGET_ID_KEY, li.widget_id);
					startActivityForResult(in_act,1);
				} else if (li.widget_type.equals(WidgetListItem.WIDGET_TABLE_TYPE_A2DP)) {
//					WidgetUtil.startA2dpConfigActivity(mGp, li.widget_id);
					Intent in_act=new Intent(mContext,ActivityConfigA2dp.class);
					in_act.putExtra(WIDGET_ID_KEY, li.widget_id);
					startActivityForResult(in_act,2);
				}
			}
			
		});
//		mWidgetListView.setOnItemLongClickListener(new OnItemLongClickListener(){
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				setWidgetListContextMenu(position);
//				return true;
//			}
//			
//		});
		
		final Handler hndl=new Handler();
		final Button btn_show_tether_menu=(Button)findViewById(R.id.activity_main_show_tethering_menu);
		final ToggleButton tgb_wifi_tether=(ToggleButton)findViewById(R.id.activity_main_wifi_tether);
		final ToggleButton tgb_bluetooth_tether=(ToggleButton)findViewById(R.id.activity_main_bluetooth_tether);
		if (Build.VERSION.SDK_INT>=23) {
			tgb_bluetooth_tether.setEnabled(false);
			tgb_wifi_tether.setEnabled(false);
		}
		if (WidgetUtil.isTetherAvailable(mContext, mLog)) {
			btn_show_tether_menu.setVisibility(Button.VISIBLE);
			tgb_wifi_tether.setVisibility(Button.VISIBLE);
			tgb_bluetooth_tether.setVisibility(Button.VISIBLE);
		} else {
			btn_show_tether_menu.setVisibility(Button.GONE);
			tgb_wifi_tether.setVisibility(Button.GONE);
			tgb_bluetooth_tether.setVisibility(Button.GONE);
		}
		btn_show_tether_menu.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
	            Intent intent = new Intent(mContext, InvokeTetheringConfig.class);
	            startActivity(intent);
			}
		});
		
		int ap=WidgetUtil.getWifiApState(mContext, mLog);
		if (ap==WIFI_AP_STATE_DISABLED) tgb_wifi_tether.setChecked(false);
		else tgb_wifi_tether.setChecked(true);
		tgb_wifi_tether.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=tgb_wifi_tether.isChecked();
//				tgb_wifi_tether.setChecked(isChecked);
				tgb_wifi_tether.setEnabled(false);
				WifiManager wm =(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
				if (isChecked) {
					SharedPreferences prefs = mGp.getSharedPrefs(mContext);
					if (wm.isWifiEnabled()) {
						prefs.edit().putString(WIFI_STATUS_KEY, WIFI_STATUS_ON).commit();
					} else {
						prefs.edit().putString(WIFI_STATUS_KEY, WIFI_STATUS_OFF).commit();
					}
					wm.setWifiEnabled(false);
					WidgetUtil.setWifiApEnabled(mContext, mLog, null, true);
					hndl.postDelayed(new Runnable(){
						@Override
						public void run() {
							WidgetUtil.setWifiTether(mContext,mLog, true);
						}
					}, 1000);
				} else {
					WidgetUtil.setWifiTether(mContext,mLog, false);
					WidgetUtil.setWifiApEnabled(mContext, mLog, null, false);
					
					SharedPreferences prefs = mGp.getSharedPrefs(mContext);
					String ws=prefs.getString(WIFI_STATUS_KEY, WIFI_STATUS_OFF);
					if (ws.equals(WIFI_STATUS_ON)) {
						wm.setWifiEnabled(true);
					}

				}
				hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						tgb_wifi_tether.setEnabled(true);
					}
				}, 2000);
			}
		});
		
		boolean bs=false;
		try {
			bs = mSvcServer.isBluetoothTetheringOn();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (bs) tgb_bluetooth_tether.setChecked(true);
		else tgb_bluetooth_tether.setChecked(false);
		tgb_bluetooth_tether.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=tgb_bluetooth_tether.isChecked();
//				tgb_bluetooth_tether.setChecked(isChecked);
				tgb_bluetooth_tether.setEnabled(false);
				if (isChecked) {
					try {
						mSvcServer.setBluetoothTethering(true);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else {
					try {
						mSvcServer.setBluetoothTethering(false);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						tgb_bluetooth_tether.setEnabled(true);
					}
				}, 2000);
			}
		});

	};
	
    private static final int WIFI_AP_STATE_DISABLING = 10;
    private static final int WIFI_AP_STATE_DISABLED = 11;
    private static final int WIFI_AP_STATE_ENABLING = 12;
    private static final int WIFI_AP_STATE_ENABLED = 13;
    private static final int WIFI_AP_STATE_FAILED = 14;

//	private void setWidgetListContextMenu(final int pos) {
//		mCcMenu.addMenuItem("Delete")
//	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//	  		@Override
//			public void onClick(CharSequence menuTitle) {
//	  			WidgetListItem li=mWidgetListAdapter.getItem(pos);
//	  			AppWidgetHost host = new AppWidgetHost(mContext,R.string.app_name);
//	  			host.deleteAppWidgetId(li.widget_id);
//	  			host.deleteHost();
//	  			mWidgetListAdapter.remove(li);
//	  			mWidgetListAdapter.notifyDataSetChanged();
//			}
//	  	});
//		mCcMenu.createMenu();
//	};
	
	private void buildWidgetListView() {
		mWidgetListView=(ListView)findViewById(R.id.activity_main_listview);
		ArrayList<WidgetListItem> list=new ArrayList<WidgetListItem>();
		for (int i=0;i<mGp.a2dpWidgetTableList.size();i++) {
			list.add(mGp.a2dpWidgetTableList.get(i));
//			Log.v("","type="+mGp.a2dpWidgetTableList.get(i).widget_type);
		}
		if (mGp.panWidgetTableList!=null) {
			for (int i=0;i<mGp.panWidgetTableList.size();i++) {
				list.add(mGp.panWidgetTableList.get(i));
//				Log.v("","type="+mGp.panWidgetTableList.get(i).widget_type);
			}
		}
		Collections.sort(list,new Comparator<WidgetListItem>() {
			@Override
			public int compare(WidgetListItem lhs, WidgetListItem rhs) {
				int r=lhs.widget_id-rhs.widget_id;
				if (lhs.widget_type.equals(rhs.widget_type)) return r;
				else return lhs.widget_type.compareToIgnoreCase(rhs.widget_type);
			}
		});
		if (list.size()==0) {
			WidgetListItem wli=new WidgetListItem();
			wli.widget_id=-1;
			list.add(wli);
		}
		mWidgetListAdapter=new AdapterWidgetList(mContext, R.layout.config_widget_table_list_item,list);
		mWidgetListView.setAdapter(mWidgetListAdapter);
	};
	
	@SuppressWarnings("unchecked")
	private void initGlblParmsByService() {
		try {
			byte[] buf=null;
			ByteArrayInputStream bais=null;
			ObjectInputStream ois=null;
			
			mGp.a2dpWidgetTableList=null;
			while(mGp.a2dpWidgetTableList==null) {
				SystemClock.sleep(100);
				buf=mSvcServer.getA2dpWidgetTable();
				bais=new ByteArrayInputStream(buf);
				ois=new ObjectInputStream(bais);
				mGp.a2dpWidgetTableList=(ArrayList<WidgetListItem>) SerializeUtil.readArrayList(ois);
			}

			buf=mSvcServer.getA2dpWidgetTable();
			bais=new ByteArrayInputStream(buf);
			ois=new ObjectInputStream(bais);
			mGp.a2dpWidgetTableList=(ArrayList<WidgetListItem>) SerializeUtil.readArrayList(ois);

			buf=mSvcServer.getPanWidgetTable();
			bais=new ByteArrayInputStream(buf);
			ois=new ObjectInputStream(bais);
			mGp.panWidgetTableList=(ArrayList<WidgetListItem>) SerializeUtil.readArrayList(ois);
			
			boolean bt=mSvcServer.isBluetoothTetheringOn();
			mGp.setBluetoothTetheringEnabled(this,bt);
			
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	};

	@Override
	final public void onPause() {
		super.onPause();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onPause entered");
		
//		if (!mIsApplicationTerminated) saveTaskData(); 
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onDestroy entered");
		
        // Application process is follow
		
		unbindWidgetService();
		
		if (mGp.settingsExitCleanly) {
			android.os.Process.killProcess(android.os.Process.myPid());
		} else {
			mGp=null;
			System.gc();
		}
	};
	
	@Override
	final public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mLog.addDebugMsg(1,"I","onConfigurationChanged Entered");
//	    refreshOptionMenu();
	};
	
	@Override
	final public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	};
	
	@Override
	final public boolean onPrepareOptionsMenu(Menu menu) {
//			menu.findItem(R.id.menu_top_export_profile).setVisible(false);
		super.onPrepareOptionsMenu(menu);
        return true;
	};
	
	@Override
	final public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_browse_log:
				invokeLogFileBrowser();
				return true;			
			case R.id.menu_identify:
				setIdentifyWidget();
				return true;			
			case R.id.menu_log_management:
				invokeLogManagement();
				return true;			
			case R.id.menu_settings:
				invokeSettingsActivity();
				return true;			
		}
		return false;
	};
	
	private void invokeLogManagement() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				boolean enabled=(Boolean)o[0];
				mGp.setLogOptionEnabled(mContext,enabled);
				applySettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mLog.resetLogReceiver();
		LogFileListDialogFragment lfmf=LogFileListDialogFragment.newInstance(true,
				mContext.getString(R.string.msgs_log_file_list_title));
		lfmf.showDialog(getSupportFragmentManager(), lfmf, mGp, ntfy);

	};


	final private void setIdentifyWidget() {
		try {
			mSvcServer.setIdentifyWidget(true);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Intent in=new Intent();
		in.setAction(Intent.ACTION_MAIN);
		in.addCategory(Intent.CATEGORY_HOME);
		mContext.startActivity(in);
	};

	final private void resetIdentifyWidget() {
		try {
			mSvcServer.setIdentifyWidget(false);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	final private void invokeLogFileBrowser() {
		mLog.addDebugMsg(1,"I","Invoke log file browser.");
		mLog.resetLogReceiver();
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.parse("file://"+
				mGp.settingsLogFileDir+mGp.settingsLogFileName+".txt"),
				"text/plain");
		startActivity(intent);
	};

	final private void bindWidgetService(final NotifyEvent p_ntfy) {
		if (mSvcServer != null) return;
		mLog.addDebugMsg(1,"I", "bindWidgetService entered");
		
        mSvcConnScheduler = new ServiceConnection(){
        	final public void onServiceConnected(ComponentName name, IBinder service) {
        		mLog.addDebugMsg(1, "I", "onServiceConnected entered");
    			mSvcServer = IServiceServer.Stub.asInterface(service);
//				setCallbackListener();
    			if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
    		}
        	final public void onServiceDisconnected(ComponentName name) {
        		mLog.addDebugMsg(1, "I", "onServiceDisconnected entered");
    			mSvcServer = null;
    		}
    	};
		Intent intent = new Intent(mContext, WidgetService.class);
		intent.setAction("Main");
		bindService(intent, mSvcConnScheduler, BIND_AUTO_CREATE);
	};
	
	final private void unbindWidgetService() { 
		mLog.addDebugMsg(1, "I", "unbindWidgetService entered");
		if (mSvcClientCallback!=null) {
			try {
				if (mSvcServer!=null)
					mSvcServer.removeCallBack(mSvcClientCallback);
				mSvcClientCallback=null;
			} catch (RemoteException e) {
				e.printStackTrace();
				mLog.addLogMsg("E", "unbindWidgetService error :"+e.toString());
			}
		}
		unbindService(mSvcConnScheduler);
	};
	
	final private void invokeSettingsActivity() {
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","Invoke Settings.");
		Intent intent = new Intent(this, ActivitySettingMain.class);
		startActivityForResult(intent,0);
	};

	final protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mGp.settingsDebugEnabled) 
			mLog.addDebugMsg(1,"I","Return from External activity. ID="+requestCode+", result="+resultCode);
		if (requestCode==0) {
			applySettingParms();
		} else if (requestCode==1) {
		} else if (requestCode==2) {
		}
	};

	private void applySettingParms() {
		mGp.loadSettingParms(mContext);
		try {
			mSvcServer.applySettingParms();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

}

