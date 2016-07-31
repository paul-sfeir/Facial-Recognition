package com.example.hyouka.pictureloaderrefactored;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Hyouka on 4/2/2016.
 */
/*
IMPORTANT NOTE:
THE FILE WiFiP2pServiceImpl.java has a method called notifyInvitationReceived() and I modify it to allow automatically accept the invtation
 */
public class MainActivity extends Activity {
    String PC = "PictureLoader";

    long offloadingstarttime = 0;

    boolean isFileTransferEnabled = true;

    PictureManager myPictureManager;

    private int screenWidth;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WiFiDirectBroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        ImageView iv = (ImageView) findViewById(R.id.face_image);
        myPictureManager = new PictureManager(this, iv);
        myPictureManager.enableZoomAndDrag(true);

        initializeWiFiP2p();

        Button load_btn = (Button) findViewById(R.id.load_btn);
        load_btn.setOnClickListener(new myOnClickListener());
        Button property_btn = (Button) findViewById(R.id.property_btn);
        property_btn.setOnClickListener(new myOnClickListener());
        Button detection_btn = (Button) findViewById(R.id.detection_btn);
        detection_btn.setOnClickListener(new myOnClickListener());
        Button wifi_search_btn = (Button) findViewById(R.id.wifi_search_btn);
        wifi_search_btn.setOnClickListener(new myOnClickListener());
        Button wifi_receive_btn = (Button) findViewById(R.id.wifi_receive_btn);
        wifi_receive_btn.setOnClickListener(new myOnClickListener());
        Button wifi_send_btn = (Button) findViewById(R.id.wifi_send_btn);
        wifi_send_btn.setOnClickListener(new myOnClickListener());

        // resize the component of the layout to get a better scene
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        int btnWidth = screenWidth/3;
        load_btn.setWidth(btnWidth);
        property_btn.setWidth(btnWidth);
        detection_btn.setWidth(btnWidth);
        wifi_receive_btn.setWidth(btnWidth);
        wifi_search_btn.setWidth(btnWidth);
        wifi_send_btn.setWidth(btnWidth);
        load_btn.setHeight(170);
        property_btn.setHeight(170);
        detection_btn.setHeight(170);
        wifi_receive_btn.setHeight(170);
        wifi_search_btn.setHeight(170);
        wifi_send_btn.setHeight(170);

        TextView lux = (TextView) findViewById(R.id.lux);
        TextView luy = (TextView) findViewById(R.id.luy);
        TextView rdx = (TextView) findViewById(R.id.rdx);
        TextView rdy = (TextView) findViewById(R.id.rdy);
        int textWidth = screenWidth/4;
        lux.setWidth(textWidth);
        luy.setWidth(textWidth);
        rdx.setWidth(textWidth);
        rdy.setWidth(textWidth);
    }

    private void initializeWiFiP2p(){
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private class myOnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.load_btn:
                    myPictureManager.selectPicture();
                    break;
                case R.id.property_btn:
                    myPictureManager.showPictureProperty();
                    //Util.test(MainActivity.this,myPictureManager.iv);
                    break;
                case R.id.detection_btn:
                    Log.d(PC,"detect start");
                    long startTime = System.currentTimeMillis();
                    myPictureManager.detectFaces(10);
                    long time = System.currentTimeMillis() - startTime;
                    Log.d(PC,"detect end");
                    Log.d("DATA","local face detection time: "+time);
                    break;
                case R.id.wifi_search_btn:
                    if(mManager!=null){
                        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(),"Discovery Initiated",Toast.LENGTH_SHORT).show();
                                Log.d(PC,"MainActivity.myOnClickListener.wifi_search_btn:\n"+"\t discoverPeers called successfully");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(),"Discovery Failure(reason code: "+Integer.toString(reason)+")",Toast.LENGTH_SHORT).show();
                                Log.d(PC,"MainActivity.myOnClickListener.wifi_search_btn:\n" +
                                        "\t discoverPeers called failure "+Integer.toString(reason));
                            }
                        });
                    }
                    break;
                case R.id.wifi_send_btn:
                    offloadingstarttime = System.currentTimeMillis();
                    //Log.d("DATA","start time: "+offloadingstarttime);
                    Log.d(PC,"wifi_send_btn clicked");
                    mReceiver.sendFile(myPictureManager.getImageFilePath());
                    break;
                case R.id.wifi_receive_btn:
                    mReceiver.enableFileTransfer(isFileTransferEnabled);
                    isFileTransferEnabled=!isFileTransferEnabled;
                    Button receive_btn = (Button) findViewById(R.id.wifi_receive_btn);
                    if(isFileTransferEnabled){
                        receive_btn.setText("Allow Receival");
                    }else{
                        receive_btn.setText("Disallow Receival");
                    }
                    break;
                default:break;
            }
        }
    }
    public void offloadingtime(){
        long time = System.currentTimeMillis() - offloadingstarttime;
        Log.d("DATA","offloading time: "+time);
    }
    protected void setImage(String path){
        myPictureManager.setPicture(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==myPictureManager.FILE_BROWSE_CODE&&resultCode==RESULT_OK&&data!=null){
            try{
                myPictureManager.setPicture(data.getStringExtra("ImageFilePath"));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void setDeviceName(String name){
        ((TextView) findViewById(R.id.device_name)).setText(name);
    }
    public void setDeviceStatus(String status){
        ((TextView) findViewById(R.id.device_status)).setText(status);
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}
