package com.sentaroh.android.BluetoothWidget.Config;

import java.util.ArrayList;

import com.sentaroh.android.BluetoothWidget.R;
import com.sentaroh.android.BluetoothWidget.Common.WidgetListItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AdapterWidgetList extends ArrayAdapter<WidgetListItem>{

	private Context c;
	private int id;
	private ArrayList<WidgetListItem>items;
	
	public AdapterWidgetList(Context context, 
			int textViewResourceId, ArrayList<WidgetListItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items=objects;
	};
	
	@Override
	final public int getCount() {
		return items.size();
	}

	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
            holder.tv_widget_id= (TextView) v.findViewById(R.id.config_widget_table_list_item_widget_id);
            holder.tv_type= (TextView) v.findViewById(R.id.config_widget_table_list_item_type);
            holder.tv_auto_connect= (TextView) v.findViewById(R.id.config_widget_table_list_item_auto_connect);
            holder.tv_device_name= (TextView) v.findViewById(R.id.config_widget_table_list_item_device_name);
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final WidgetListItem o = items.get(position);
        if (o != null) {
        	if (o.widget_id==-1) {
            	holder.tv_widget_id.setVisibility(TextView.GONE);
            	holder.tv_type.setVisibility(TextView.GONE);
            	holder.tv_auto_connect.setVisibility(TextView.GONE);
            	holder.tv_device_name.setText("No widget registered");
        	} else {
            	holder.tv_widget_id.setVisibility(TextView.VISIBLE);
            	holder.tv_type.setVisibility(TextView.VISIBLE);
            	holder.tv_auto_connect.setVisibility(TextView.VISIBLE);
            	holder.tv_widget_id.setText(String.format("%4S", o.widget_id));
            	holder.tv_type.setText(o.widget_type);
            	if (o.auto_connect_adapter_on) holder.tv_auto_connect.setText("Yes");
            	else holder.tv_auto_connect.setText("No");
            	holder.tv_device_name.setText(o.device_name);
        	}
       	}
        return v;
	};

	class ViewHolder {
		TextView tv_widget_id, tv_type, tv_device_name, tv_auto_connect;
	}
}
