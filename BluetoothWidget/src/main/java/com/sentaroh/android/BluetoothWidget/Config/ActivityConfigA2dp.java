package com.sentaroh.android.BluetoothWidget.Config;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.sentaroh.android.BluetoothWidget.GlobalParameters;
import com.sentaroh.android.BluetoothWidget.IServiceCallback;
import com.sentaroh.android.BluetoothWidget.IServiceServer;
import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.WidgetService;
import com.sentaroh.android.BluetoothWidget.WidgetUtil;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import com.sentaroh.android.BluetoothWidget.Log.LogFileListDialogFragment;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityConfigA2dp extends AppCompatActivity {

	private GlobalParameters mGp=null;
	
	private static int mRestartStatus=0;
	
	private Context mContext=null;
	private LogUtil mLog=null;
	
	private IServiceCallback mSvcClientCallback=null;
	private ServiceConnection mSvcConnScheduler=null;
	private IServiceServer mSvcServer=null;

    private int mWidgetId=-1;
	
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
	
//	private void getOverflowMenu() {
//		http://stackoverflow.com/questions/9739498/android-action-bar-not-showing-overflow	
//	     try {
//	        ViewConfiguration config = ViewConfiguration.get(this);
//	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
//	        if(menuKeyField != null) {
//	            menuKeyField.setAccessible(true);
//	            menuKeyField.setBoolean(config, false);
//	        }
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//	};

    @SuppressLint("NewApi")
	@Override
    final public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//    	StrictMode.enableDefaults();
//        if (Build.VERSION.SDK_INT<=10) 
//        	requestWindowFeature(Window.FEATURE_NO_TITLE); 
//        setTheme(android.R.style.Theme_Holo_Light);
        mContext=this;
//        mActivity=this;
        if (mGp==null) {
            mGp=(GlobalParameters)getApplication();
            if (mGp.initialyzeRequired) {
                mGp.loadSettingParms(mContext);
                mGp.initialyzeRequired=false;
            }
            mLog=new LogUtil(mContext.getApplicationContext(), "ConfigA2DP", mGp);
        }
        if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","onCreate entered");
        
        setContentView(R.layout.a2dp_config_activity);
        
		if (Build.VERSION.SDK_INT>=14)
			this.getSupportActionBar().setHomeButtonEnabled(false);
        
        mRestartStatus=0;
        
        int conf_wid=getIntent().getIntExtra(WIDGET_ID_KEY,0);
        
		setResult(RESULT_CANCELED);
		
		// AppWidgetID の取得
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int app_wid=-1;
		if (extras != null) {
			app_wid = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
								AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (conf_wid!=0) mWidgetId=conf_wid;
		else mWidgetId=app_wid;
		
		setTitle(getTitle()+" Widget="+mWidgetId);
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

		// AppWidgetIdなしでIntnetが飛んできたら終了
		if(mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
			finish();
		} else {
		    
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					initGlblParmsByService();
					buildBonededDeviceListView();
					if (mRestartStatus==0) {
						setButtonListener();
					} else if (mRestartStatus==1) {
						
					} else if (mRestartStatus==2) {
						setButtonListener();
					}
				}

				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
				
			});
			bindWidgetService(ntfy);
		}
	};
	
	@SuppressWarnings("unchecked")
	private void initGlblParmsByService() {
		try {
			byte[] buf=null;
			ByteArrayInputStream bais=null;
			ObjectInputStream ois=null;
			
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

	@SuppressWarnings("unused")
	private void confirmTurnOnBluetooth(final NotifyEvent ntfy_main) {
		final NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				turnOnBluetooth(ntfy_main);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				finish();
			}
		});
		final CommonDialog commonDlg=new CommonDialog(mContext, getSupportFragmentManager());
		commonDlg.showCommonDialog(true, "W", 
				getString(R.string.msgs_main_bt_adapter_confirm_on_msg),
				"",
				ntfy);
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
	
	final private void bindWidgetService(final NotifyEvent p_ntfy) {
		if (mSvcServer != null) return;
		mLog.addDebugMsg(1,"I", "bindWidgetService entered");
		
        mSvcConnScheduler = new ServiceConnection(){
        	final public void onServiceConnected(ComponentName name, IBinder service) {
        		mLog.addDebugMsg(1, "I", "onServiceConnected entered");
    			mSvcServer = IServiceServer.Stub.asInterface(service);
				setCallbackListener();
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
		if (mSvcConnScheduler!=null) {
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
		}
	};
	
	private Handler mUiHandler = new Handler();
	final private void setCallbackListener() {
		mLog.addDebugMsg(1, "I", "setCallbackListener entered");
        mSvcClientCallback = new IServiceCallback.Stub() {
        	final public void notifyToClient(final String resp) throws RemoteException {
        		mLog.addDebugMsg(1, "I", "Notify received ","Resp=",resp);
				mUiHandler.post(new Runnable() {
					@Override
                    public void run() {
					}
				});
			}
        	final public void notifyToClientBtAdapterOn() throws RemoteException {
        		mLog.addDebugMsg(1, "I", "Bluetooth On received ");
				mUiHandler.post(new Runnable() {
					@Override
                    public void run() {
						setButtonEnabled();
					}
				});
			}
        	final public void notifyToClientBtAdapterOff() throws RemoteException {
        		mLog.addDebugMsg(1, "I", "Bluetooth Off received ");
				mUiHandler.post(new Runnable() {
					@Override
                    public void run() {
						setButtonEnabled();
					}
				});
			}
        	final public void notifyToClientDeviceConnected(
        			String dev_type, String dev_name) throws RemoteException {
        		mLog.addDebugMsg(1, "I", "Bluetooth connected received, device="+dev_name);
				mUiHandler.post(new Runnable() {
					@Override
                    public void run() {
					}
				});
			}
        	final public void notifyToClientDeviceDisconnected(
        			String dev_type, String dev_name) throws RemoteException {
        		mLog.addDebugMsg(1, "I", "Bluetooth disconnected received, device="+dev_name);
				mUiHandler.post(new Runnable() {
					@Override
                    public void run() {
					}
				});
			}
        };
		try{
			mSvcServer.setCallBack(mSvcClientCallback);
		} catch (RemoteException e){
			e.printStackTrace();
			mLog.addLogMsg("E", "setCallbackListener error :"+e.toString());
		}
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

	final private void invokeLogFileBrowser() {
		mLog.addDebugMsg(1,"I","Invoke log file browser.");
		mLog.resetLogReceiver();
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://"+
				mGp.settingsLogFileDir+mGp.settingsLogFileName+".txt"),
				"text/plain");
		startActivity(intent);
	};
	
	final private void invokeSettingsActivity() {
		if (mGp.settingsDebugEnabled) mLog.addDebugMsg(1,"I","Invoke Settings.");
		Intent intent = new Intent(this, ActivitySettingMain.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, 
//				"com.sentaroh.android.BluetoothWidget.ActivitySettingMain$SettingsA2dp");
		startActivityForResult(intent,0);
	};

	final protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mGp.settingsDebugEnabled) 
			mLog.addDebugMsg(1,"I","Return from External activity. ID="+requestCode+", result="+resultCode);
		if (requestCode==0) {
			applySettingParms();
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

    private ListView mBondedDeviceListView=null;
    private ArrayAdapter<String> mBondedDeviceAdapter=null;
    private void buildBonededDeviceListView() {
    	final TextView tv_msg=(TextView)findViewById(R.id.a2dp_config_activity_msg);
    	mBondedDeviceAdapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, new ArrayList<String>());
    	mBondedDeviceListView=(ListView)findViewById(R.id.a2dp_config_activity_device_list);
    	mBondedDeviceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    	mBondedDeviceListView.setAdapter(mBondedDeviceAdapter); 

    	BluetoothAdapter bt_adapter=BluetoothAdapter.getDefaultAdapter();
    	if (bt_adapter.isEnabled()) {
    		Set<BluetoothDevice>bdlist=bt_adapter.getBondedDevices();
    	    Iterator<BluetoothDevice> bDeviceIterator = bdlist.iterator();
    	    while (bDeviceIterator.hasNext()) {
	    	    BluetoothDevice device = bDeviceIterator.next();
	    	    boolean a2dp=WidgetUtil.bluetoothDeviceIsClassMatch(device,WidgetUtil.PROFILE_A2DP);
	    	    boolean hsp=WidgetUtil.bluetoothDeviceIsClassMatch(device,WidgetUtil.PROFILE_HEADSET);
	    	    if (mGp.settingsDebugEnabled) 
	    	    	mLog.addDebugMsg(1,"I","Device="+device.getName()+", a2dp="+a2dp+", hsp="+hsp);
	    	    if (a2dp || hsp)
	    	    	mBondedDeviceAdapter.add(device.getName());
    	    }
        	int sel_pos=-1;
    		WidgetListItem wli=WidgetUtil.getWidgetItemByWidgetId(mGp.a2dpWidgetTableList, mWidgetId);
    		if (wli!=null) {
            	for (int i=0;i<mBondedDeviceAdapter.getCount();i++) {
            	    if (mBondedDeviceAdapter.getItem(i).equals(wli.device_name)) {
            	    	sel_pos=i;
            	    	break;
            	    }
            	}
    		}
        	mBondedDeviceListView.setItemChecked(sel_pos,true);;
        	mBondedDeviceAdapter.notifyDataSetChanged();
        	if (mBondedDeviceAdapter.getCount()>0) {
        		tv_msg.setText(mContext.getString(R.string.msgs_a2dp_config_message));
        	} else {
        		tv_msg.setText(mContext.getString(R.string.msgs_a2dp_config_no_device_found));
        	}
        	setButtonEnabled();
    	} else {
    		tv_msg.setText(mContext.getString(R.string.msgs_a2dp_config_disable_bt_adapter));
    	}
    };
    
    private void setDefaultCheckedTextViewListener(final CheckedTextView ctv) {
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.setChecked(!ctv.isChecked());
			}
		});
    };
    
    private void setButtonListener() {
    	final Button btn_apply=(Button)findViewById(R.id.a2dp_config_activity_btn_assign);
    	final CheckedTextView ctv_auto_connect=(CheckedTextView)findViewById(R.id.a2dp_config_activity_auto_connect);
    	setDefaultCheckedTextViewListener(ctv_auto_connect);
    	Button btn_cancel=(Button)findViewById(R.id.a2dp_config_activity_btn_cancel);
    	final Button btn_on=(Button)findViewById(R.id.a2dp_config_activity_btn_on);
    	final BluetoothAdapter bt_adap=BluetoothAdapter.getDefaultAdapter();

    	btn_apply.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
				RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout_a2dp);
				appWidgetManager.updateAppWidget(mWidgetId, views);
    			int sel_pos=mBondedDeviceListView.getCheckedItemPosition();
    			String dev_name=(String) mBondedDeviceListView.getAdapter().getItem(sel_pos);
    			try {
					mSvcServer.updateA2dpDeiveNameByWidgetId(mWidgetId, dev_name, ctv_auto_connect.isChecked());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
    			
    			// Result をセットして終了
    			Intent intent = new Intent();
    			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
    			setResult(RESULT_OK, intent);
    			finish();
			}
    	});
    	btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				finish();
			}
    	});
    	
    	WidgetListItem wli=WidgetUtil.getWidgetItemByWidgetId(mGp.a2dpWidgetTableList, mWidgetId);
    	if (wli!=null) ctv_auto_connect.setChecked(wli.auto_connect_adapter_on);
    	ctv_auto_connect.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_auto_connect.setChecked(!ctv_auto_connect.isChecked());
				if (mBondedDeviceListView.getCheckedItemPosition()>=0)
					btn_apply.setEnabled(true);
			}
    	});
    	mBondedDeviceListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
//				Log.v("","selected="+pos);
				btn_apply.setEnabled(true);
			}
    	});
    	
    	final Handler hndl=new Handler();
    	btn_on.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (!bt_adap.isEnabled()) {
					btn_on.setEnabled(false);
					final NotifyEvent ntfy=new NotifyEvent(mContext);
					ntfy.setListener(new NotifyEventListener(){
						@Override
						public void positiveResponse(Context c, Object[] o) {
							hndl.post(new Runnable(){
								@Override
								public void run() {
									buildBonededDeviceListView();
								}
							});
						}
						@Override
						public void negativeResponse(Context c, Object[] o) {
						}
					});
					turnOnBluetooth(ntfy);
				}
			}
    	});
    	setButtonEnabled();
    };
    
    private void setButtonEnabled() {
    	Button btn_apply=(Button)findViewById(R.id.a2dp_config_activity_btn_assign);
//    	Button btn_cancel=(Button)findViewById(R.id.pan_dev_activity_btn_cancel);
    	final Button btn_on=(Button)findViewById(R.id.a2dp_config_activity_btn_on);
    	final BluetoothAdapter bt_adap=BluetoothAdapter.getDefaultAdapter();
    	
//    	if (mBondedDeviceAdapter.getCount()>0) {
//    		btn_apply.setEnabled(true);
//    	} else {
//    		btn_apply.setEnabled(false);
//    	}
    	btn_apply.setEnabled(false);
    	
    	btn_on.setEnabled(true);
    	if (bt_adap==null) btn_on.setEnabled(false);
    	else if (bt_adap.isEnabled()) btn_on.setEnabled(false);
    };

	private void turnOnBluetooth(final NotifyEvent ntfy_main) {
		final BluetoothAdapter adap=BluetoothAdapter.getDefaultAdapter();
		
		final CommonDialog commonDlg=new CommonDialog(mContext, getSupportFragmentManager());
		final ProgressSpinDialogFragment pdsf=ProgressSpinDialogFragment.newInstance(
				getString(R.string.msgs_main_bt_adapter_on_title),
				getString(R.string.msgs_main_bt_adapter_on_msg), 
				getString(R.string.msgs_main_bt_adapter_on_cancel_before),
				getString(R.string.msgs_main_bt_adapter_on_cancel_after));
		final Handler hndl=new Handler();
		final ThreadCtrl tctl=new ThreadCtrl();
		
		final NotifyEvent ntfy_cancel=new NotifyEvent(mContext);
		ntfy_cancel.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				tctl.setDisabled();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				tctl.setDisabled();
			}
		});

		pdsf.showDialog(getSupportFragmentManager(), pdsf, ntfy_cancel,true);
		Thread th=new Thread() {
			@Override
			public void run() {
				if (!adap.isEnabled()) {
					adap.enable();
					boolean to=true;
					try {
						for (int i=0;i<200;i++) {
							Thread.sleep(50);
							if (adap.getState()==BluetoothAdapter.STATE_ON) {
								to=false;
								break;
							}
							if (!tctl.isEnabled()) {
								hndl.post(new Runnable(){
									@Override
									public void run() {
										pdsf.dismiss();
									}
								});
								break;
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					hndl.post(new Runnable(){
						@Override
						public void run() {
							pdsf.dismiss();
						}
					});
					if (to) {
						if (!tctl.isEnabled()) {
						} else {
							String m_txt=getString(R.string.msgs_main_bt_adapter_could_not_on);
							commonDlg.showCommonDialog(false, "E", m_txt, "", null);
						}
					} else {
						if (ntfy_main!=null)
							ntfy_main.notifyToListener(true, null);
					}
				} else {
					if (ntfy_main!=null)
						ntfy_main.notifyToListener(true, null);
				}
			}
		};
		th.start();
	};

}

class HolderActivityConfigA2dp implements Serializable  {
	private static final long serialVersionUID = 1L;
//	public ArrayList<ProfileListItem> tpfal;
	public ArrayList<String> aal;
};

