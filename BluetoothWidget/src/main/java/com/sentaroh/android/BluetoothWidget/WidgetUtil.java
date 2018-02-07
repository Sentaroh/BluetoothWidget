package com.sentaroh.android.BluetoothWidget;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sentaroh.android.BluetoothWidget.Common.UnplayingDeviceListItem;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;
import com.sentaroh.android.BluetoothWidget.Config.ActivityConfigA2dp;
import com.sentaroh.android.BluetoothWidget.Config.ActivityConfigPan;
import com.sentaroh.android.BluetoothWidget.Log.LogUtil;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.StringUtil;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothClass.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WidgetUtil {

	final public static void setIdentifyWidget(GlobalParameters gp, boolean enabled) {
		gp.log.addDebugMsg(1, "I", "setIdentifyWidget entered, enabled="+enabled);
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
			gp.a2dpWidgetTableList.get(i).identify=enabled;
		}
		for (int i=0;i<gp.panWidgetTableList.size();i++) {
			gp.panWidgetTableList.get(i).identify=enabled;
		}
	};

	final public static boolean isIdentifyWidgetEnabled(GlobalParameters gp) {
		boolean result=false;
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
			if (gp.a2dpWidgetTableList.get(i).identify) {
				result=true;
				break;
			}
		}
		if (!result) {
			for (int i=0;i<gp.panWidgetTableList.size();i++) {
				if (gp.panWidgetTableList.get(i).identify) {
					result=true;
					break;
				}
			}
		}
		gp.log.addDebugMsg(1, "I", "isIdentifyWidgetEnabled entered, result="+result);
		return result;
	};

	final public static void resetAutoConnect(ArrayList<WidgetListItem> wl) {
		for (int i=0;i<wl.size();i++) wl.get(i).auto_connect_adapter_on=false;
	};
	
    @SuppressWarnings("deprecation")
	final public static void startA2dpConfigActivity(GlobalParameters gp, int wid) { 
		Intent in_act=new Intent(gp.appContext,ActivityConfigA2dp.class);
		in_act.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
				Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//		in.addCategory(Intent.CATEGORY_DEFAULT);
		in_act.putExtra(WIDGET_ID_KEY, wid);
		gp.appContext.startActivity(in_act);
    };

    @SuppressWarnings("deprecation")
	final public static void startPanConfigActivity(GlobalParameters gp, int wid) { 
		Intent in_act=new Intent(gp.appContext,ActivityConfigPan.class);
		in_act.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
				Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//		in.addCategory(Intent.CATEGORY_DEFAULT);
		in_act.putExtra(WIDGET_ID_KEY, wid);
		gp.appContext.startActivity(in_act);
    };
	
	final static public void removeUnplayingList(
			GlobalParameters gp,
			String dev_name, 
			ArrayList<UnplayingDeviceListItem>tbl) {
		for (int i=0;i<tbl.size();i++) {
			if (tbl.get(i).device_name.equals(dev_name)) {
				tbl.remove(i);
				break;
			}
		}
	};
	
	final static public void addUnplayingList(
			GlobalParameters gp,
			String dev_name, 
			ArrayList<UnplayingDeviceListItem>tbl) {
		UnplayingDeviceListItem li=new UnplayingDeviceListItem();
		li.device_name=dev_name;
		li.not_playing_begin_time=System.currentTimeMillis();
		tbl.add(li);
	};
	
	final static public UnplayingDeviceListItem getUnplayingListItem(
			GlobalParameters gp,
			String dev_name, 
			ArrayList<UnplayingDeviceListItem>tbl) {
		UnplayingDeviceListItem li=null;
		for (int i=0;i<tbl.size();i++) {
			if (tbl.get(i).device_name.equals(dev_name)) {
				li=tbl.get(i);
				break;
			}
		}
		return li;
	};

	final static public void scheduleA2dpDisconnectTimer(GlobalParameters gp,
			ArrayList<UnplayingDeviceListItem>tbl) {
		if (tbl.size()>0 && 
				!gp.settingsA2dpAutoDiscTimeUnplaying.equals("0")) {
			Collections.sort(tbl, new Comparator<UnplayingDeviceListItem>(){
				@Override
				public int compare(UnplayingDeviceListItem lh,
						UnplayingDeviceListItem rh) {
					if (lh.not_playing_begin_time>rh.not_playing_begin_time) return -1;
					if (lh.not_playing_begin_time==rh.not_playing_begin_time) return 0;
					if (lh.not_playing_begin_time<rh.not_playing_begin_time) return 1;
					return 0;
				}
			});
			long sched_time=System.currentTimeMillis()+
					Integer.parseInt(gp.settingsA2dpAutoDiscTimeUnplaying)*1000*60;
			
	    	Intent iw = new Intent(gp.appContext,WidgetService.class);
			iw.setAction(BROADCAST_DISCONNECT_UNPLAYING_DEVICE);
			iw.putExtra(DISCONNECT_DEVICE_NAME_KEY,tbl.get(0).device_name);
			
			PendingIntent piw = PendingIntent.getService(gp.appContext, 0, iw,
					PendingIntent.FLAG_UPDATE_CURRENT);
		    AlarmManager amw = (AlarmManager)gp.appContext.getSystemService(Context.ALARM_SERVICE);
		    amw.setRepeating(AlarmManager.RTC_WAKEUP, sched_time, 0, piw);
		    gp.log.addDebugMsg(1, "I", "scheduleA2dpDisconnectTimer device="+tbl.get(0).device_name+
		    		", time="+StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(sched_time));
		}
	};
	
	final static public void cancelA2dpDisconnectTimer(GlobalParameters gp) {
    	Intent iw = new Intent(gp.appContext,WidgetService.class);
		iw.setAction(BROADCAST_DISCONNECT_UNPLAYING_DEVICE);
		PendingIntent piw = PendingIntent.getService(gp.appContext, 0, iw,
				PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager amw = (AlarmManager)gp.appContext.getSystemService(Context.ALARM_SERVICE);
	    amw.cancel(piw);
	};

	@SuppressWarnings("unchecked")
	static final public ArrayList<WidgetListItem> loadA2dpWidgetTable(
			GlobalParameters gp,
			LogUtil log) {
		ArrayList<WidgetListItem> result=new ArrayList<WidgetListItem>();
		log.addDebugMsg(1, "I", "A2dp Widget table read lock="+gp.lockA2dpWidgetTable.getReadLockCount());
		gp.lockA2dpWidgetTable.readLock().lock();
		try {
			InputStream is=gp.appContext.openFileInput("a2dp_widget_table");
			ObjectInputStream ois=new ObjectInputStream(is);
			
			result=(ArrayList<WidgetListItem>) SerializeUtil.readArrayList(ois);
			ois.close();
			if (result!=null) {
				for (int i=0;i<result.size();i++) {
					log.addDebugMsg(1, "I", "A2DP table loaded, wid="+result.get(i).widget_id+
							", Name="+result.get(i).device_name);
				}
			} else {
				log.addDebugMsg(1, "I", "A2DP table not loaded");
			}
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			gp.lockA2dpWidgetTable.readLock().unlock();
		}
		if (result==null) return new ArrayList<WidgetListItem>();
		return result;
	};
	
	static final public void saveA2dpWidgetTable(
			GlobalParameters gp,
			ArrayList<WidgetListItem>tbl,
			LogUtil log) {
		log.addDebugMsg(1, "I", "A2dp Widget table write lock="+gp.lockA2dpWidgetTable.getWriteHoldCount());
		gp.lockA2dpWidgetTable.writeLock().lock();
		try {
			OutputStream os=gp.appContext.openFileOutput("a2dp_widget_table", 0);
			ObjectOutputStream oos=new ObjectOutputStream(os);
			SerializeUtil.writeArrayList(oos, tbl);
			oos.close();
			for (int i=0;i<tbl.size();i++) {
				log.addDebugMsg(1, "I", "A2DP table saved, wid="+tbl.get(i).widget_id+
						", Name="+tbl.get(i).device_name);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			gp.lockA2dpWidgetTable.writeLock().unlock();
		}
		log.addDebugMsg(1, "I", "saveA2dpWidgetTable exit");
	};

	@SuppressWarnings("unchecked")
	static final public ArrayList<WidgetListItem> loadPanWidgetTable(
			GlobalParameters gp,
			LogUtil log) {
		ArrayList<WidgetListItem> result=new ArrayList<WidgetListItem>();
		log.addDebugMsg(1, "I", "Pan Widget table read lock="+gp.lockPanWidgetTable.getReadLockCount());
		gp.lockPanWidgetTable.readLock().lock();
		try {
			InputStream is=gp.appContext.openFileInput("pan_widget_table");
			ObjectInputStream ois=new ObjectInputStream(is);
			
			result=(ArrayList<WidgetListItem>) SerializeUtil.readArrayList(ois);
			ois.close();
			if (result!=null) {
				for (int i=0;i<result.size();i++) {
					log.addDebugMsg(1, "I", "PAN table loaded, wid="+result.get(i).widget_id+
							", Name="+result.get(i).device_name+", auto_conn="+result.get(i).auto_connect_adapter_on);
				}
			} else {
				log.addDebugMsg(1, "I", "PAN table not loaded");
			}
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			gp.lockPanWidgetTable.readLock().unlock();
		}
		if (result==null) return new ArrayList<WidgetListItem>();
		return result;
	};
	
	static final public void savePanWidgetTable(
			GlobalParameters gp,
			ArrayList<WidgetListItem>tbl,
			LogUtil log) {
		log.addDebugMsg(1, "I", "A2dp Widget table write lock="+gp.lockA2dpWidgetTable.getWriteHoldCount());
		gp.lockA2dpWidgetTable.writeLock().lock();
		try {
			OutputStream os=gp.appContext.openFileOutput("pan_widget_table", 0);
			ObjectOutputStream oos=new ObjectOutputStream(os);
			SerializeUtil.writeArrayList(oos, tbl);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			gp.lockA2dpWidgetTable.writeLock().unlock();
		}
		log.addDebugMsg(1, "I", "savePanWidgetTable exit");
	};

    public static void setRefreshDeviceStatusTimer(final GlobalParameters gp) {
		gp.log.addDebugMsg(1, "I", "setRefreshDeviceStatusTimer entered");
//	    gp.svcHandler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				gp.log.addDebugMsg(1, "I", "setRefreshDeviceStatusTimer expired");
//				refreshPanStatus(gp);
//				refreshA2dpStatus(gp);
//			}
//	    },DEVICE_REFRESH_TIME_OUT);
    };

	final public static void refreshPanStatus(GlobalParameters gp) {
//		BluetoothDevice bd = 
//				WidgetUtil.getBluetoothDeviceByName(gp, gp.panCurrentDeviceName);
//		if (bd!=null) gp.panConnectionStatus=panGetConnectionState(gp, bd);
//		else gp.panConnectionStatus=BluetoothProfile.STATE_DISCONNECTED;
//		WidgetServicePan.setPanButtonIcon(gp);
//		gp.panBtnEnabled = true;
		for (int i=0;i<gp.panWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.panWidgetTableList.get(i);
			if (cwt.connection_status!=BluetoothProfile.STATE_DISCONNECTED) {
				BluetoothDevice bd = 
						WidgetUtil.getBluetoothDeviceByName(gp, cwt.device_name);
				if (bd!=null) cwt.connection_status=panGetConnectionState(gp, bd);
				else cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
			}
		}
		WidgetServicePan.setPanButtonIcon(gp);
	};
	
	final public static void refreshA2dpStatus(GlobalParameters gp) {
		for (int i=0;i<gp.a2dpWidgetTableList.size();i++) {
			WidgetListItem cwt=gp.a2dpWidgetTableList.get(i);
			BluetoothDevice bd = 
					WidgetUtil.getBluetoothDeviceByName(gp, cwt.device_name);
			if (bd!=null) cwt.connection_status=a2dpGetConnectionState(gp, bd);
			else cwt.connection_status=BluetoothProfile.STATE_DISCONNECTED;
		}
		WidgetServiceA2dp.setA2dpButtonIcon(gp);
	};
	
    public static WidgetListItem getWidgetItemByWidgetId(
    		ArrayList<WidgetListItem> tbl, int wid) {
    	for (int i=0;i<tbl.size();i++) {
    		if (tbl.get(i).widget_id==wid) 
    			return tbl.get(i);
    	}
    	return null;
    }

	@SuppressWarnings("deprecation")
	static final public SharedPreferences getPrefMgr(Context c) {
    	return c.getSharedPreferences(DEFAULT_PREFS_FILENAME,
        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
    };

	final static public void refreshWidgetService(Context c) {
        Intent in = new Intent(c, WidgetService.class);
    	in.setAction(WIDGET_SERVICE_REFRESH);
        c.startService(in);
	};
	
	final static public BluetoothDevice getBluetoothDeviceByName(GlobalParameters gp, String dev) {
		if (!dev.equals(DEVICE_NAME_UNSELECTED)) {
		    Set<BluetoothDevice> bd_list = gp.btAdapter.getBondedDevices();
		    Iterator<BluetoothDevice> iterator = bd_list.iterator();
		    while (iterator.hasNext()) {
		      BluetoothDevice device = iterator.next();
		      if (device.getName().equals(dev)) return device;
		    }
		}
	    return null;
	};

	static final public boolean isKeyguardEffective(Context mContext) {
        KeyguardManager keyguardMgr=
        		(KeyguardManager)mContext.getSystemService(Context.KEYGUARD_SERVICE);
    	boolean result=keyguardMgr.inKeyguardRestrictedInputMode();
    	return result;
    };
    
	static public boolean isTetherAvailable(Context c, LogUtil mLog) {
		ConnectivityManager cm =
			        (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
		String[] tether_bt=null, tether_usb=null, tether_wifi=null;
	    Method[] wmMethods = cm.getClass().getDeclaredMethods();
	    boolean result=false;

	    for(Method method: wmMethods){
	        try {
	  	      if(method.getName().equals("getTetherableBluetoothRegexs")){
	  	    	  tether_bt = (String[]) method.invoke(cm);
	  	    	  for (int i=0;i<tether_bt.length;i++) {
	  	    		  mLog.addDebugMsg(1, "I", "Bluetooth tether device="+tether_bt[i]);
	  	    	  }
	  	      } else if(method.getName().equals("getTetherableWifiRegexs")){
	  	    	  tether_wifi = (String[]) method.invoke(cm);
	  	    	  for (int i=0;i<tether_wifi.length;i++) {
	  	    		  mLog.addDebugMsg(1, "I", "WiFi tether device="+tether_wifi[i]);
	  	    	  }
	  	      } else if(method.getName().equals("getTetherableUsbRegexs")){
	  	    	  tether_usb = (String[]) method.invoke(cm);
	  	    	  for (int i=0;i<tether_usb.length;i++) {
	  	    		  mLog.addDebugMsg(1, "I", "USB tether device="+tether_usb[i]);
	  	    	  }
	  	      }
	        } catch (IllegalArgumentException e) {
	              e.printStackTrace();
	              return result;
	        } catch (IllegalAccessException e) {
	              e.printStackTrace();
	              return result;
	        } catch (InvocationTargetException e) {
	              e.printStackTrace();
	              return result;
	        }
	    }
	    if (tether_bt.length>0 || tether_usb.length>0 || tether_wifi.length>0) result=true;
	    return result;
//	    for(Method method: wmMethods){
//	          if(method.getName().equals("tether")){                  
//	              try {
//	                code = (Integer) method.invoke(cm, available[0]);
//	            } catch (IllegalArgumentException e) {
//	                e.printStackTrace();
//	                return;
//	            } catch (IllegalAccessException e) {
//	                e.printStackTrace();
//	                return;
//	            } catch (InvocationTargetException e) {
//	                e.printStackTrace();
//	                return;
//	            }
//	            break;
//	          }
//	    }
//
//	    if (code==0) 
//	        Log.d("","Enable usb tethering successfully!");
//	    else
//	        Log.d("","Enable usb tethering failed!");
	};

	static public boolean isWifiApEnabled(Context c, LogUtil mLog) {
		boolean result=false;
		WifiManager wm =(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wm.getClass().getDeclaredMethods();
	    for(Method method: wmMethods){
	        if(method.getName().equals("isWifiApEnabled")){                  
	        	try {
	        		method.setAccessible(true);
	        		result = (Boolean) method.invoke(wm);
			    } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                return false;
			    } catch (IllegalAccessException e) {
	                e.printStackTrace();
	                return false;
			    } catch (InvocationTargetException e) {
	                e.printStackTrace();
	                return false;
			    }
			    break;
	        }
	    }
		return result;
	};

	static public int getWifiApState(Context c, LogUtil mLog) {
		int result=-1;
		WifiManager wm =(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wm.getClass().getDeclaredMethods();
	    for(Method method: wmMethods){
	        if(method.getName().equals("getWifiApState")){                  
	        	try {
	        		method.setAccessible(true);
	        		result = (Integer) method.invoke(wm);
			    } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                return -1;
			    } catch (IllegalAccessException e) {
	                e.printStackTrace();
	                return -1;
			    } catch (InvocationTargetException e) {
	                e.printStackTrace();
	                return -1;
			    }
			    break;
	        }
	    }
		return result;
	};

	static public boolean setWifiApEnabled(Context c, LogUtil mLog, WifiConfiguration wifiConfig, boolean enabled) {
		boolean result=false;
		WifiManager wm =(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wm.getClass().getDeclaredMethods();
	    for(Method method: wmMethods){
	        if(method.getName().equals("setWifiApEnabled")){                  
	        	try {
	        		method.setAccessible(true);
	        		result = (Boolean) method.invoke(wm, wifiConfig, enabled);
			    } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                return false;
			    } catch (IllegalAccessException e) {
	                e.printStackTrace();
	                return false;
			    } catch (InvocationTargetException e) {
	                e.printStackTrace();
	                return false;
			    }
			    break;
	        }
	    }
		return result;
	};

	static public boolean setWifiApConfiguration(Context c, LogUtil mLog, WifiConfiguration wifiConfig) {
		boolean result=false;
		WifiManager wm =(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wm.getClass().getDeclaredMethods();
	    for(Method method: wmMethods){
	        if(method.getName().equals("setWifiApConfiguration")){                  
	        	try {
	        		method.setAccessible(true);
	        		result = (Boolean) method.invoke(wm, wifiConfig);
			    } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                return false;
			    } catch (IllegalAccessException e) {
	                e.printStackTrace();
	                return false;
			    } catch (InvocationTargetException e) {
	                e.printStackTrace();
	                return false;
			    }
			    break;
	        }
	    }
		return result;
	};

	static public WifiConfiguration getWifiApConfiguration(Context c, LogUtil mLog) {
		WifiConfiguration result=null;
		WifiManager wm =(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wm.getClass().getDeclaredMethods();
	    for(Method method: wmMethods){
	        if(method.getName().equals("getWifiApConfiguration")){                  
	        	try {
	        		method.setAccessible(true);
	        		Object o=method.invoke(wm);
	        		Log.v("","o="+o);
	        		result = (WifiConfiguration) o;
			    } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                return null;
			    } catch (IllegalAccessException e) {
	                e.printStackTrace();
	                return null;
			    } catch (InvocationTargetException e) {
	                e.printStackTrace();
	                return null;
			    }
			    break;
	        }
	    }
		return result;
	};
	
	
	static public boolean setWifiTether(Context c, LogUtil mLog, boolean enabled) {
		ConnectivityManager cm =
			        (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
	    Method[] wmMethods = cm.getClass().getDeclaredMethods();
	    boolean result=false;
	    int code=-1;
	    for(Method method: wmMethods){
	    	if (enabled) {
		        if(method.getName().equals("tether")){                  
		        	try {
		        		method.setAccessible(true);
		        		code = (Integer) method.invoke(cm, new Object[] { "wlan0" });//"wlan0");
				    } catch (IllegalArgumentException e) {
				                e.printStackTrace();
				                return false;
				    } catch (IllegalAccessException e) {
				                e.printStackTrace();
				                return false;
				    } catch (InvocationTargetException e) {
				                e.printStackTrace();
				                return false;
				    }
				    break;
		        }
	    	} else {
		        if(method.getName().equals("untether")){                  
		        	try {
		        		code = (Integer) method.invoke(cm, "wlan0");
				    } catch (IllegalArgumentException e) {
				                e.printStackTrace();
				                return false;
				    } catch (IllegalAccessException e) {
				                e.printStackTrace();
				                return false;
				    } catch (InvocationTargetException e) {
				                e.printStackTrace();
				                return false;
				    }
				    break;
		        }

	    	}
	    }

	    if (code==0) 
	        Log.d("","Enable tethering successfully!");
	    else
	        Log.d("","Enable tethering failed!");
	    return result;
	};

	static public boolean isBluetoothTetherAvailable(Context c, LogUtil mLog) {
		ConnectivityManager cm =
			        (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
		String[] tether_bt=null;
	    Method[] wmMethods = cm.getClass().getDeclaredMethods();
	    boolean result=false;

	    for(Method method: wmMethods){
	        try {
	  	      if(method.getName().equals("getTetherableBluetoothRegexs")){
	  	    	  tether_bt = (String[]) method.invoke(cm);
	  	    	  for (int i=0;i<tether_bt.length;i++) {
	  	    		  mLog.addDebugMsg(1, "I", "Bluetooth tether device="+tether_bt[i]);
	  	    	  }
	  	      }
	        } catch (IllegalArgumentException e) {
	              e.printStackTrace();
	              return result;
	        } catch (IllegalAccessException e) {
	              e.printStackTrace();
	              return result;
	        } catch (InvocationTargetException e) {
	              e.printStackTrace();
	              return result;
	        }
	    }
	    if (tether_bt.length>0) result=true;
	    return result;
	};


    
    final static public int PROFILE_A2DP=BluetoothProfile.A2DP;
    final static public int PROFILE_HEADSET=BluetoothProfile.HEADSET;
    final static public int PROFILE_OPP=2;//Object Push Profile 
    final static public int PROFILE_HID=3;
    final static public int PROFILE_NAP=4;//PAN-NAP
    final static public int PROFILE_PAN=5;//PAN-USER
    static public boolean bluetoothDeviceIsClassMatch(BluetoothDevice device, 
    		int profile) {
//    	
//    		このメソッドはBluetoothClass#doesClassMatchからコピー
//
    	BluetoothClass bc=device.getBluetoothClass();
        if (profile == PROFILE_A2DP) {
            if (bc.hasService(Service.RENDER)) {
                return true;
            }
            // By the A2DP spec, sinks must indicate the RENDER service.
            // However we found some that do not (Chordette). So lets also
            // match on some other class bits.
            switch (bc.getDeviceClass()) {
                case Device.AUDIO_VIDEO_HIFI_AUDIO:
                case Device.AUDIO_VIDEO_HEADPHONES:
                case Device.AUDIO_VIDEO_LOUDSPEAKER:
                case Device.AUDIO_VIDEO_CAR_AUDIO:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_HEADSET) {
            // The render service class is required by the spec for HFP, so is a
            // pretty good signal
            if (bc.hasService(Service.RENDER)) {
                return true;
            }
            // Just in case they forgot the render service class
            switch (bc.getDeviceClass()) {
                case Device.AUDIO_VIDEO_HANDSFREE:
                case Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                case Device.AUDIO_VIDEO_CAR_AUDIO:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_OPP) {
            if (bc.hasService(Service.OBJECT_TRANSFER)) {
                return true;
            }

            switch (bc.getDeviceClass()) {
                case Device.COMPUTER_UNCATEGORIZED:
                case Device.COMPUTER_DESKTOP:
                case Device.COMPUTER_SERVER:
                case Device.COMPUTER_LAPTOP:
                case Device.COMPUTER_HANDHELD_PC_PDA:
                case Device.COMPUTER_PALM_SIZE_PC_PDA:
                case Device.COMPUTER_WEARABLE:
                case Device.PHONE_UNCATEGORIZED:
                case Device.PHONE_CELLULAR:
                case Device.PHONE_CORDLESS:
                case Device.PHONE_SMART:
                case Device.PHONE_MODEM_OR_GATEWAY:
                case Device.PHONE_ISDN:
                    return true;
                default:
                    return false;
            }
        } else if (profile == PROFILE_HID) {
            return (bc.getDeviceClass() & Device.Major.PERIPHERAL) == Device.Major.PERIPHERAL;
        } else if (profile == PROFILE_PAN || profile == PROFILE_NAP){
            // No good way to distinguish between the two, based on class bits.
            if (bc.hasService(Service.NETWORKING)) {
                return true;
            }
            return (bc.getDeviceClass() & Device.Major.NETWORKING) == Device.Major.NETWORKING;
        } else {
            return false;
        }
    };
    
	static public int a2dpGetConnectionState(GlobalParameters gp,
            final BluetoothDevice param) {
		try {
			if (gp.a2dp_dev_method_getConnectionState!=null) {
				int ret=(Integer)gp.a2dp_dev_method_getConnectionState.invoke(gp.a2dpProxy, new Object[]{ param });
//				Thread.currentThread().dumpStack();
				gp.log.addDebugMsg(1, "I", "a2dpGetConnectionState device="+param.getName()+
						", addr="+param.getAddress()+", result="+ret);
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "a2dpGetConnectionState can not executed.");
				return -1;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		}
	};
	@SuppressWarnings({"unchecked"})
	static public List<BluetoothDevice> a2dpGetConnectedDevices(GlobalParameters gp) {
		try {
			if (gp.a2dp_dev_method_getConnectedDevices!=null) {
				List<BluetoothDevice> ret=(List<BluetoothDevice>)gp.a2dp_dev_method_getConnectedDevices.invoke(gp.a2dpProxy);
				gp.log.addDebugMsg(1, "I", "a2dpGetConnectedDevices");
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "a2dpGetConnectedDevices can not executed.");
				return null;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		}
	};

	static public boolean a2dpIsA2dpPlaying(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.a2dp_dev_method_isA2dpPlaying!=null) {
				boolean result=(Boolean)gp.a2dp_dev_method_isA2dpPlaying.invoke(gp.a2dpProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "a2dpIsA2dpPlaying executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "a2dpIsA2dpPlaying can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	};
	
	static public boolean a2dpConnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.a2dp_dev_method_connect!=null) {
				boolean result=(Boolean)gp.a2dp_dev_method_connect.invoke(gp.a2dpProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "a2dpConnect executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "a2dpConnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	};
	
	static public boolean a2dpDisconnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.a2dp_dev_method_disconnect!=null) {
				boolean result=(Boolean)gp.a2dp_dev_method_disconnect.invoke(gp.a2dpProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "a2dpDisconnect executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "a2dpDisconnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	};

	static public int hspGetConnectionState(GlobalParameters gp,
             final BluetoothDevice param) {
		try {
			if (gp.hsp_method_getConnectionState!=null) {
				int ret=(Integer)gp.hsp_method_getConnectionState.invoke(gp.hspProxy, new Object[]{ param });
//					Thread.currentThread().dumpStack();
				gp.log.addDebugMsg(1, "I", "hspGetConnectionState device="+param.getName()+
						", addr="+param.getAddress()+", result="+ret);
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "hspGetConnectionState can not executed.");
				return -1;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		}
	};
	@SuppressWarnings({"unchecked"})
	static public List<BluetoothDevice> hspGetConnectedDevices(GlobalParameters gp) {
		try {
			if (gp.hsp_method_getConnectedDevices!=null) {
				List<BluetoothDevice> ret=(List<BluetoothDevice>)gp.hsp_method_getConnectedDevices.invoke(gp.hspProxy);
				gp.log.addDebugMsg(1, "I", "hspGetConnectedDevices");
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "hspGetConnectedDevices can not executed.");
				return null;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		}
	 };

	 static public boolean hspConnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.hsp_method_connect!=null) {
				boolean result=(Boolean)gp.hsp_method_connect.invoke(gp.hspProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "hspConnect executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "hspConnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	 };
		
	 static public boolean hspDisconnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.hsp_method_disconnect!=null) {
				boolean result=(Boolean)gp.hsp_method_disconnect.invoke(gp.hspProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "hspDisconnect executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "hspDisconnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	 };

	static public int panGetConnectionState(GlobalParameters gp,
             final BluetoothDevice param) {
		try {
			if (gp.pan_method_getConnectionState!=null) {
				int ret=(Integer)gp.pan_method_getConnectionState.invoke(gp.panProxy, new Object[]{ param });
//					Thread.currentThread().dumpStack();
				gp.log.addDebugMsg(1, "I", "panGetConnectionState device="+param.getName()+
						", addr="+param.getAddress()+", result="+ret);
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "panGetConnectionState can not executed.");
				return -1;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return -1;
		}
	};
	@SuppressWarnings({"unchecked"})
	static public List<BluetoothDevice> panGetConnectedDevices(GlobalParameters gp) {
		try {
			if (gp.pan_method_getConnectedDevices!=null) {
				List<BluetoothDevice> ret=(List<BluetoothDevice>)gp.pan_method_getConnectedDevices.invoke(gp.panProxy);
				gp.log.addDebugMsg(1, "I", "panGetConnectedDevices size="+ret.size());
				return ret;
			} else {
				gp.log.addDebugMsg(1, "E", "panGetConnectedDevices can not executed.");
				return null;
			}
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return null;
		}
	 };

	 static public boolean panIsTeringOn(GlobalParameters gp) {
//		Thread.dumpStack(); 
		try {
			if (gp.pan_method_isTetheringOn!=null) {
				boolean result=(Boolean)gp.pan_method_isTetheringOn.invoke(gp.panProxy);
				gp.log.addDebugMsg(1, "I", "panIsTeringOn executed, result="+result);
				return result;
			} else {
				gp.log.addDebugMsg(1, "E", "panIsTeringOn can not executed.");
				return false;
			}
			
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	 };
	 
	 static public void panSetBluetoothTering(
			 GlobalParameters gp, final boolean enabled) {
//		Thread.dumpStack();
		try {
			if (gp.pan_method_setBluetoothTethering!=null) {
				gp.pan_method_setBluetoothTethering.invoke(gp.panProxy, new Object[]{ enabled });
				gp.log.addDebugMsg(1, "I", "panSetBluetoothTering executed");
			} else {
//				Thread.dumpStack();
				gp.log.addDebugMsg(1, "E", "panSetBluetoothTering can not executed.");
			}
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			gp.log.addDebugMsg(1, "E", "" + e); 
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			gp.log.addDebugMsg(1, "E", "" + e); 
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			gp.log.addDebugMsg(1, "E", "" + e); 
		}
	 };

	 static public boolean panConnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.pan_method_connect!=null) {
				boolean result=(Boolean)gp.pan_method_connect.invoke(gp.panProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "panConnect executed, device="+param.getName()+", result="+result);
				return result;
			} else {
				gp.log.addDebugMsg(1, "E", "panConnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	 };
	
	 static public boolean panDisconnect(
			 GlobalParameters gp, final BluetoothDevice param) {
		try {
			if (gp.pan_method_disconnect!=null) {
				boolean result=(Boolean)gp.pan_method_disconnect.invoke(gp.panProxy, new Object[]{ param });
				gp.log.addDebugMsg(1, "I", "panDisconnect executed, device="+param.getName()+", result="+result);
				return 	result;
			} else {
				gp.log.addDebugMsg(1, "E", "panDisconnect can not executed.");
			}
			return false;
		} catch (IllegalArgumentException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (InvocationTargetException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		} catch (IllegalAccessException e) {
			gp.log.addDebugMsg(1, "E", "" + e); return false;
		}
	 };

}
