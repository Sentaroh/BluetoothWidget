package com.sentaroh.android.BluetoothWidget.Common;

import static com.sentaroh.android.BluetoothWidget.Common.Constants.DEVICE_NAME_UNSELECTED;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import android.widget.RemoteViews;

public class WidgetListItem implements Externalizable{
	public static final String WIDGET_TABLE_TYPE_A2DP="A2DP";
	public static final String WIDGET_TABLE_TYPE_PAN="PAN";
	public String widget_type=WIDGET_TABLE_TYPE_A2DP;
	public int widget_id=-1;
	public String device_name=DEVICE_NAME_UNSELECTED;
	public boolean auto_connect_adapter_on=false;
	public int connection_status=0;
	
	public boolean identify=false;

	public boolean connect_required_if_bt_off=false;
	public RemoteViews remote_view=null;
	
	public WidgetListItem() {};
	
	@Override
	public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
		widget_type=objin.readUTF();
		widget_id=objin.readInt();
		device_name=objin.readUTF();
		auto_connect_adapter_on=objin.readBoolean();
	}
	@Override
	public void writeExternal(ObjectOutput objout) throws IOException {
		objout.writeUTF(widget_type);
		objout.writeInt(widget_id);
		objout.writeUTF(device_name);
		objout.writeBoolean(auto_connect_adapter_on);
	}
}
