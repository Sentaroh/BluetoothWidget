package com.sentaroh.android.BluetoothWidget.Config;

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

import java.util.List;

import com.sentaroh.android.BluetoothWidget.GlobalParameters;
import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.WidgetUtil;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

@SuppressLint("NewApi")
public class ActivitySettingMain extends PreferenceActivity{
	private static PreferenceFragment mPrefFrag=null;
	private static Context mContext=null;

	private static GlobalParameters mGp=null;
    
	private static LogUtil mLogMain=null;

	private void initGlblParms(Context c) {
		initGlblParms(c, false);
	}
	private void initGlblParms(Context c, boolean force) {
        if (mGp==null) {
        	mContext=c;
            mGp=(GlobalParameters)getApplication();
            if (mGp.initialyzeRequired) {
                mGp.loadSettingParms(c);
                mGp.initialyzeRequired=false;
            }
            mLogMain=new LogUtil(c.getApplicationContext(), "SettingsMain", mGp);
        }
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		PreferenceManager pm=getPreferenceManager();
//		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
//		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
//		SharedPreferences shared_pref=pm.getSharedPreferences();
        initGlblParms(this,true);
        mLogMain.addDebugMsg(1, "I", "onCreate entered");

        if (Build.VERSION.SDK_INT>=11) return;
	};

    @Override
    public void onStart(){
        super.onStart();
        mLogMain.addDebugMsg(1, "I", "onStart entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        mLogMain.addDebugMsg(1, "I", "onResume entered");
		setTitle(R.string.settings_main_title);

    };

    @Override
    public void onBuildHeaders(List<Header> target) {
    	initGlblParms(this);
    	mLogMain.addDebugMsg(1, "I", "onBuildHeaders entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
    	mLogMain.addDebugMsg(1, "I", "onListItemClick entered, pos="+position);
    	
    }
//    @Override
//    public boolean isMultiPane () {
//    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"ActivitySettingMain isMultiPane entered");
//        return true;
//    };

    @Override
    public boolean onIsMultiPane () {
    	initGlblParms(this);
    	mLogMain.addDebugMsg(1, "I", "onIsMultiPane entered");
    	return true;
//        return isXLargeTablet(this) && !isSimplePreferences(this);
    };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration()
        		.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    @SuppressWarnings("unused")
	private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS || 
        		Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || 
        		!isXLargeTablet(context);
    }

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    mLogMain.addDebugMsg(1, "I", "onPause entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		mLogMain.addDebugMsg(1, "I", "onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		mLogMain.addDebugMsg(1, "I", "onDestroy entered");
	};

	@Override
	final public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mLogMain.addDebugMsg(1,"I","onConfigurationChanged Entered");
//	    refreshOptionMenu();
	};

	private static void initSettingValueAfterHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefFrag.findPreference(key_string),shared_pref,key_string);
	};

	private static void initSettingValue(Preference pref_key, 
			SharedPreferences shared_pref, String key_string) {
		
		if (!checkPanSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkA2dpSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkBtSettings(pref_key,shared_pref, key_string,mContext))	
		if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
		   	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
	};

	private static SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefFrag.findPreference(key_string);
				if (!checkPanSettings(pref_key,shared_pref, key_string,mContext))
				if (!checkA2dpSettings(pref_key,shared_pref, key_string,mContext))					
				if (!checkBtSettings(pref_key,shared_pref, key_string,mContext))					
				if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
				  	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};

	private static boolean checkPanSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_widget_pan_auto_on_adapter_off))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_widget_pan_auto_disc_screen_locked))) {
    		isChecked=true;
    	} 

		return isChecked;
	};

	private static boolean checkA2dpSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		if (key_string.equals(c.getString(R.string.settings_widget_a2dp_auto_on_adapter_off))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_widget_a2dp_auto_disc_screen_locked))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_widget_a2dp_auto_disc_time_unplaying))) {
    		String val=shared_pref.getString(key_string, "5");
    		if (val.equals("0")) {
        		pref_key.setSummary(
        	    		c.getString(R.string.settings_widget_a2dp_auto_disc_time_unplaying_summary_dis));
    		} else {
        		String m_txt=String.format(c.getString(R.string.settings_widget_a2dp_auto_disc_time_unplaying_summary_ena),
        				val);
        		pref_key.setSummary(m_txt);
    		}
    		
    		isChecked=true;
    	} 
//		Log.v("","key="+key_string+", isChecked="+isChecked);
		return isChecked;
	};

	private static boolean checkBtSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		if (key_string.equals(c.getString(R.string.settings_bt_enabled))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_bt_auto_on_adapter_on))) {
    		isChecked=true;
    	} 
		Log.v("","key="+key_string+", isChecked="+isChecked);
		return isChecked;
	};

	private static boolean checkMiscSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_logging_enabled))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_exit_cleanly))) {
    		isChecked=true;
    	} 

		return isChecked;
	};

	private static boolean checkOtherSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		Log.v("","key="+key_string);
		boolean isChecked = true;
    	if (pref_key!=null) {
    		pref_key.setSummary(
	    		c.getString(R.string.settings_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));
    	} else {
    		mLogMain.addDebugMsg(1, "I", "key not found. key="+key_string);
    	}
    	return isChecked;
	};

    public static class SettingsWidget extends PreferenceFragment {
    	private LogUtil mLogPan=null;
		@SuppressWarnings("deprecation")
		@Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
//        	initGlblParms(getActivity().getApplicationContext());
        	mLogPan=new LogUtil(this.getActivity().getApplicationContext(), 
        			"SettingsWidget", mGp);
        	mLogPan.addDebugMsg(1, "I", "onCreate entered");
    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
    		SharedPreferences shared_pref=pm.getSharedPreferences();
            
    		addPreferencesFromResource(R.xml.settings_frag_widget);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_widget_pan_auto_on_adapter_off));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_widget_pan_auto_disc_screen_locked));
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_widget_a2dp_auto_on_adapter_off));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_widget_a2dp_auto_disc_screen_locked));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_widget_a2dp_auto_disc_time_unplaying));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	mLogPan.addDebugMsg(1, "I", "onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        
        @Override
        public void onStop() {
        	super.onStop();
        	mLogPan.addDebugMsg(1, "I", "onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

    public static class SettingsBt extends PreferenceFragment {
    	private LogUtil mLogBt=null;
		@SuppressWarnings("deprecation")
		@Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
//        	initGlblParms(getActivity().getApplicationContext());
        	mLogBt=new LogUtil(this.getActivity().getApplicationContext(), 
        			"SettingsBt", mGp);
        	mLogBt.addDebugMsg(1, "I", "onCreate entered");
    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
    		SharedPreferences shared_pref=pm.getSharedPreferences();
            
    		addPreferencesFromResource(R.xml.settings_frag_bt);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_bt_enabled));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_bt_auto_on_adapter_on));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	mLogBt.addDebugMsg(1, "I", "onStart entered");
            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
    	    if (!WidgetUtil.isTetherAvailable(mContext, mLogBt)) {
    	    	mPrefFrag.findPreference(getString(R.string.settings_bt_enabled)).setEnabled(false);
    	    	mPrefFrag.findPreference(getString(R.string.settings_bt_auto_on_adapter_on)).setEnabled(false);
    	    }
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        
        @Override
        public void onStop() {
        	super.onStop();
        	mLogBt.addDebugMsg(1, "I", "onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

    public static class SettingsMisc extends PreferenceFragment {
    	private LogUtil mLogMisc=null;
        @SuppressWarnings("deprecation")
		@Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
//        	initGlblParms(getActivity().getApplicationContext());
        	mLogMisc=new LogUtil(this.getActivity().getApplicationContext(), 
        			"SettingsMisc", mGp);
        	mLogMisc.addDebugMsg(1, "I", "onCreate entered");
    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
    		SharedPreferences shared_pref=pm.getSharedPreferences();
            
    		addPreferencesFromResource(R.xml.settings_frag_misc);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_logging_enabled));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_debug_level));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_exit_cleanly));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	mLogMisc.addDebugMsg(1, "I", "onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	mLogMisc.addDebugMsg(1, "I", "onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

}