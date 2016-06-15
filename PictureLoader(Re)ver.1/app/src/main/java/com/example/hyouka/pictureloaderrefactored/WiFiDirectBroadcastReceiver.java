package com.example.hyouka.pictureloaderrefactored;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Hyouka on 4/6/2016.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    String PC = "PictureLoader";

    protected WifiP2pManager mManager;
    protected WifiP2pManager.Channel mChannel;
    protected MainActivity mActivity;
    protected WiFiDeviceManager mWiFiDeviceManager;
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity){
        super();
        mManager=manager;
        mChannel=channel;
        mActivity=activity;
        mWiFiDeviceManager = new WiFiDeviceManager(mActivity,mManager,mChannel);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(mActivity.getApplicationContext(),"WiFiP2p Enabled",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(mActivity.getApplicationContext(),"WiFiP2p Disabled",Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            //Toast.makeText(mActivity.getApplicationContext(),"STAGE A",Toast.LENGTH_SHORT).show();
            if(mManager!=null){
                Log.d(PC,"WiFiDirectBroadcastReceiver.WIFI_P2P_PEERS_CHANGED_ACTION: " +
                        "requestPeers called");
                mManager.requestPeers(mChannel, mWiFiDeviceManager);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.d(PC,"WiFiDirectBroadcastReceiver.WIFI_P2P_CONNECTION_CHANGED_ACTION");
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection information
                mManager.requestConnectionInfo(mChannel,mWiFiDeviceManager);
                Toast.makeText(mActivity.getApplicationContext(),"Connected to " + mWiFiDeviceManager.getCurrentDeviceName(),Toast.LENGTH_SHORT).show();
                Log.d(PC,"WiFiDirectBroadcastReceiver.WIFI_P2P_CONNECTION_CHANGED_ACTION: Connected to " + mWiFiDeviceManager.getCurrentDeviceName());
                //mWiFiDeviceManager.dealWithClientIP();
            } else if(networkInfo.getDetailedState()== NetworkInfo.DetailedState.DISCONNECTED ||
                       networkInfo.getDetailedState()== NetworkInfo.DetailedState.DISCONNECTING) {
                // It's a disconnect
                Log.d(PC,"disconnect");
                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(mActivity.getApplicationContext(),"It is a disconnect",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        //Toast.makeText(mActivity.getApplicationContext(),"Fail to disconnect(reason code:"+Integer.toString(reason)+")",Toast.LENGTH_SHORT).show();
                    }
                });
                mWiFiDeviceManager.resetCurrent();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if(device!=null) {
                mActivity.setDeviceName(device.deviceName);
                mActivity.setDeviceStatus(getDeviceStatus(device.status));
                Log.d(PC,"WiFiDirectBroadcastReceiver.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: " + device.deviceName+"\t"+getDeviceStatus(device.status));
                if(device.status == WifiP2pDevice.CONNECTED){
                    mManager.requestConnectionInfo(mChannel,mWiFiDeviceManager);
                }
            }else Log.d(PC, "WiFiDirectBroadcastReceiver.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: NULL device");
        }
    }

    public void enableFileTransfer(boolean state){mWiFiDeviceManager.enableFileTransfer(state);}
    private String getDeviceStatus(int status){
        switch (status){
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown Status";
        }
    }
    public void sendFile(String path){
        if(mWiFiDeviceManager.isTargetIPNULL()){
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reason) {

                }
            });
            return;
        }
        mWiFiDeviceManager.sendFile(path);
    }
}
