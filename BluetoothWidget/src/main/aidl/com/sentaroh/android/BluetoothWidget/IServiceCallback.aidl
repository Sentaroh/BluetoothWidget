package com.sentaroh.android.BluetoothWidget;

interface IServiceCallback{ 
    void notifyToClient(String resp_id);
    void notifyToClientBtAdapterOff();
    void notifyToClientBtAdapterOn();
    void notifyToClientDeviceConnected(String dev_type, String dev_name);
    void notifyToClientDeviceDisconnected(String dev_type, String dev_name);
    
}