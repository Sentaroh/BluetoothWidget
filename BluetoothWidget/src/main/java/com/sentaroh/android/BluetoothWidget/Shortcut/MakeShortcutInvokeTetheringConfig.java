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

import com.sentaroh.android.BluetoothWidget.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public class MakeShortcutInvokeTetheringConfig extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
        
        sendBroadcast(makeShortcutIntent(this, 
        		InvokeTetheringConfig.class.getName(),
        		getString(R.string.msgs_invoke_tether_config_title),
        		R.drawable.tethering_config));
        finish();
    }

    private Intent makeShortcutIntent(Context c, String class_name, 
    		String shortcut_name,
    		int icon_id) {
        Intent shortcut_activity=new Intent(Intent.ACTION_VIEW);
        shortcut_activity.setClassName(c, class_name);
        
        Intent shortcut_intent = new Intent();
        shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut_activity);
        shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcut_name);
        Parcelable iconResource = 
        	Intent.ShortcutIconResource.fromContext(c, icon_id);
        shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        shortcut_intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        return shortcut_intent;
    };
}
