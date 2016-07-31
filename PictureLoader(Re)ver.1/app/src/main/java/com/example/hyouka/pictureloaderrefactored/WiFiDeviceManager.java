package com.example.hyouka.pictureloaderrefactored;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Hyouka on 4/6/2016.
 */
public class WiFiDeviceManager implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    static String PC = "PictureLoader";

    public void enableFileTransfer(boolean state){
        transfer_state=state;
        if(fileTransfer!=null)fileTransfer.setEnable(state);
        if(facedata!=null)facedata.setEnable(state);
    }
    boolean transfer_state = true;

    private static final int SEND_FILE_PORT_NUM = 8988;
    private static final int IP_PORT_NUM = 6666;
    private static final int FACE_DATA_PORT = 6000;
    protected String targetIPAddress = null;
    public boolean isTargetIPNULL(){
        return targetIPAddress==null;
    }
    // unused
    public void setTargetIPAddress(String IP){
        targetIPAddress = IP;Log.d(PC, "target IP: "+targetIPAddress);
    }
    public String getTargetIPAddress(){
        return targetIPAddress;
    }

    protected WifiP2pDevice currentDevice = null;
    public String getCurrentDeviceName(){
        if(currentDevice!=null)return currentDevice.deviceName;
        else return null;
    }
    public boolean isCurrentDeviceNULL(){return currentDevice==null;}
    protected WifiP2pInfo currentInfo = null;
    public boolean isCurrentInfoNULL(){return currentInfo==null;}
    public void resetCurrent(){
        currentDevice = null;currentInfo = null;targetIPAddress=null;fileTransfer=null;facedata=null;
        Log.d(PC,"WiFiDeviceManager.resetCurrent: ALL DATA SET TO NULL");
    }

    MainActivity mActivity;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    FileServerAsyncTask fileTransfer = null;
    FaceDataAsyncTask facedata = null;
    public WiFiDeviceManager(MainActivity activity,WifiP2pManager manager, WifiP2pManager.Channel channel){
        mActivity=activity;
        mManager=manager;
        mChannel=channel;
    }
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        //Toast.makeText(mActivity.getApplicationContext(),"STAGE B",Toast.LENGTH_SHORT).show();
        List<WifiP2pDevice> deviceList = new ArrayList<>();
        deviceList.addAll(peers.getDeviceList());
        if(deviceList.size()==0){
            Log.d(PC,"WiFiDeviceManager.onPeersAvailable: No peers discovered");
        }else {
            Log.d(PC,"WiFiDeviceManager.onPeersAvailable: "+Integer.toString(deviceList.size()) + " Peers found");
        }
        for(int i=0;i<deviceList.size();++i){
            WifiP2pDevice device = deviceList.get(i);
            if(device.status == WifiP2pDevice.AVAILABLE){
                connect(device);
                break;
            }
        }
    }

    protected void connect(final WifiP2pDevice device){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(mActivity.getApplicationContext(), "Connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
                Log.d(PC,"WiFiDeviceManager.connect: connecting to "+device.deviceName);
                currentDevice = device;
                currentInfo = null;
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(mActivity.getApplicationContext(), "Connection Failure(reason code: " + Integer.toString(reason) + ")", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        currentInfo = info;
        Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: set connection information");
        if(info.groupFormed){
            if(targetIPAddress==null) {
                if (!info.isGroupOwner) {
                    targetIPAddress = info.groupOwnerAddress.getHostAddress();
                    Log.d(PC, "target IP: "+targetIPAddress);
                    Intent serviceIntent = new Intent(mActivity, WiFiFileTransferService.class);
                    serviceIntent.setAction(WiFiFileTransferService.ACTION_SEND_IP);
                    serviceIntent.putExtra(WiFiFileTransferService.EXTRAS_IP_ADDRESS, targetIPAddress);
                    serviceIntent.putExtra(WiFiFileTransferService.EXTRAS_PORT, IP_PORT_NUM);
                    mActivity.startService(serviceIntent);
                } else {
                    new ClientIPAsyncTask(this).execute();
                }
            }
            Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: server setup begin");
            if(fileTransfer==null || fileTransfer.getStatus() != AsyncTask.Status.RUNNING) {
                fileTransfer = new FileServerAsyncTask(mActivity,this);
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: just before filetransfer execute");
                fileTransfer.execute();
            }
            Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: null filetransfer constructed");
            fileTransfer.setEnable(transfer_state);
            Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: filetransfer execute");
            if(facedata==null || facedata.getStatus() != AsyncTask.Status.RUNNING) {
                facedata = new FaceDataAsyncTask(mActivity);
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: just before facedata execute");
                facedata.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: null facedata constructed");
            facedata.setEnable(transfer_state);
            Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: server setup END");
        }
    }

    public void sendFile(String path){
        if(isCurrentDeviceNULL()){
            Log.d(PC,"WiFiDirectBroadcastReceiver.isConnected: current device NULL");
        }
        if(isCurrentInfoNULL()){
            Log.d(PC,"WiFiDirectBroadcastReceiver.isConnected: current connection information NULL");
        }
        if(isTargetIPNULL()) {
            Log.d(PC, "WiFiDirectBroadcastReceiver.isConnected: target IP Address NULL");return;
        }
        if(path==null){
            Log.d(PC, "WiFiDirectBroadcastReceiver.isConnected: Image File Path NULL");return;
        }
        File f = new File(path);
        Uri uri = Uri.fromFile(f);
        Log.d(PC, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(mActivity, WiFiFileTransferService.class);
        serviceIntent.setAction(WiFiFileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(WiFiFileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(WiFiFileTransferService.EXTRAS_IP_ADDRESS,
                targetIPAddress);
        serviceIntent.putExtra(WiFiFileTransferService.EXTRAS_PORT, SEND_FILE_PORT_NUM);
        mActivity.startService(serviceIntent);
    }

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private MainActivity activity;
        private WiFiDeviceManager manager;
        private DatabaseHelper helper;
        boolean enable=true;
        public void setEnable(boolean state){enable=state;}
        //private TextView statusText;

        public FileServerAsyncTask(MainActivity activity,WiFiDeviceManager manager) {
            this.activity = activity;
            this.manager = manager;
            this.helper = new DatabaseHelper(activity.getApplicationContext());
            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            if(!enable)return null;
            try {

                Log.d(PC,"WiFiDeviceManager.FileServerAsyncTask.doInBackground: doInBackground begin");
                ServerSocket serverSocket = new ServerSocket(SEND_FILE_PORT_NUM);
                Socket client = serverSocket.accept();
                
                Log.d(PC,"WiFiDeviceManager.FileServerAsyncTask.doInBackground: serversocket accecpt receive");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + activity.getApplicationContext().getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists()) dirs.mkdirs();
                f.createNewFile();
                Log.d(PC,"WiFiDeviceManager.FileServerAsyncTask.doInBackground: file created");
                InputStream inputstream = client.getInputStream();
                WiFiFileTransferService.copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                Log.d(PC,"WiFiDeviceManager.FileServerAsyncTask.doInBackground: doInBackground END");
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.d(PC,"WiFiDeviceManager.FileServerAsyncTask.doInBackground: IOException e");
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(activity.getApplicationContext(),"File Received",Toast.LENGTH_SHORT).show();
                Log.d(PC,"FILE RECEIVED");
                // check whether this file has been detected
                String md5 = Util.MD5(new File(result));
                Cursor file = helper.getFileRecords(md5);

                String face_data = new String();
                if(file==null || file.getCount()==0) {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(result, opt);
                    opt.inSampleSize = PictureManager.calculateInSampleSize(opt, 350, 400);
                    opt.inJustDecodeBounds = false;
                    opt.inPreferredConfig = Bitmap.Config.RGB_565;
                    Bitmap image = BitmapFactory.decodeFile(result, opt);
                    FaceDetector detector = new FaceDetector(image.getWidth(), image.getHeight(), 10);
                    FaceDetector.Face faces[] = new FaceDetector.Face[10];
                    int detected = detector.findFaces(image, faces);
                    if (detected <= 0) return;
                    for (int i = 0; i < detected; ++i) {
                        PointF middle = new PointF();
                        faces[i].getMidPoint(middle);
                        face_data += Float.toString(middle.x) + "/";
                        face_data += Float.toString(middle.y) + "/";
                        face_data += Float.toString(faces[i].eyesDistance()) + "/";
                    }
                    // insert data
                    Calendar calendar = Calendar.getInstance();
                    helper.addImageFileRecord(md5,calendar.toString(),result,detected);
                    for(int i=0;i<detected;++i){
                        String eye_dis = Float.toString(faces[i].eyesDistance());
                        PointF mid = new PointF();
                        faces[i].getMidPoint(mid);
                        String mid_x = Float.toString(mid.x);
                        String mid_y = Float.toString(mid.y);
                        String confidence = Float.toString(faces[i].confidence());
                        String e_x = Float.toString(faces[i].pose(0));
                        String e_y = Float.toString(faces[i].pose(1));
                        String e_z = Float.toString(faces[i].pose(2));
                        helper.addFaceRecord(
                                md5,"face"+i,opt.inSampleSize,
                                eye_dis,mid_x,mid_y,confidence,e_x,e_y,e_z
                        );
                    }
                }else{
                    file.moveToFirst();
                    Cursor face = helper.getFaceRecords(md5);
                    int face_num = file.getInt(3);
                    face.moveToFirst();
                    for(int i=0;i<face_num;++i){
                        String eye_dis = face.getString(3);
                        String mid_x = face.getString(4);
                        String mid_y = face.getString(5);
                        face_data += mid_x +"/";
                        face_data += mid_y +"/";
                        face_data += eye_dis +"/";
                        face.moveToNext();
                    }
                }
                Log.d(PC, "WiFiDeviceManager.FileServerAsyncTask.onPostExecute: preparation of data end");
                Intent service = new Intent(activity, WiFiFileTransferService.class);
                service.setAction(WiFiFileTransferService.ACTION_SEND_FACE_DATA);
                service.putExtra(WiFiFileTransferService.EXTRAS_IP_ADDRESS, manager.targetIPAddress);
                service.putExtra(WiFiFileTransferService.EXTRAS_PORT, FACE_DATA_PORT);
                service.putExtra(WiFiFileTransferService.EXTRAS_FACE_DATA, face_data);
                Toast.makeText(activity.getApplicationContext(), "Send Face Data", Toast.LENGTH_SHORT).show();
                Log.d(PC, "WiFiDeviceManager.FileServerAsyncTask.onPostExecute: just before Service start");
                activity.startService(service);
            }

        }

    }
    public static class FaceDataAsyncTask extends AsyncTask<Void,Void,String>{
        private boolean enable =  true;
        private MainActivity activity;
        float x[] = null;
        float y[] = null;
        float eye[] = null;
        int num = 0;

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public FaceDataAsyncTask(MainActivity activity){
            this.activity = activity;
        }
        @Override
        protected String doInBackground(Void... params) {
            if(!enable)return null;
            try {
                Log.d(PC,"FaceData doINBack");
                ServerSocket serverSocket = new ServerSocket(FACE_DATA_PORT);
                Socket client = serverSocket.accept();
                DataInputStream input = new DataInputStream(client.getInputStream());
                String recieved = input.readUTF();
                Log.d(PC,"Data received");
                String face_data[] = recieved.split("/");
                int num_of_face = face_data.length / 3;
                if(3 * num_of_face < face_data.length)num_of_face--;
                if(num_of_face<=0)return null;
                num=num_of_face;
                Log.d(PC,"Data Manipulation");
                x = new float[num_of_face];
                y = new float[num_of_face];
                eye = new float[num_of_face];
                for(int i=0;i<num_of_face;++i){
                    x[i] = Float.parseFloat(face_data[i*3]);
                    y[i] = Float.parseFloat(face_data[i*3+1]);
                    eye[i] = Float.parseFloat(face_data[i*3+2]);
                }
                input.close();
                if(client.isConnected())client.close();
                if(!serverSocket.isClosed())serverSocket.close();
                return "Data Received";
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(activity.getApplicationContext(),s,Toast.LENGTH_SHORT).show();
            activity.myPictureManager.DrawRectAndSetData(x,y,eye,num);
            activity.offloadingtime();
        }
    }
    public static class ClientIPAsyncTask extends AsyncTask<Void,Void,Void>{
        public ClientIPAsyncTask(WiFiDeviceManager manager){this.manager=manager;}
        WiFiDeviceManager manager;
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: prepare to receive Client IP");
            // prepare to receive IP Address from client
            try {
                Socket client = null;
                ServerSocket serverSocket = null;
                DataInputStream inputStream = null;
                serverSocket = new ServerSocket(IP_PORT_NUM);
                //serverSocket.setReuseAddress(true);
                //Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: address reused set to true");
                client = serverSocket.accept();
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: accept receive");
                inputStream = new DataInputStream(client.getInputStream());
                String object = inputStream.readUTF();
                if (object.equals("IP")) {
                    String IP = client.getInetAddress().getHostAddress();
                    manager.setTargetIPAddress(IP);
                }
                inputStream.close();
                if (client.isConnected()) client.close();
                if (!serverSocket.isClosed()) serverSocket.close();
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: Client IP receive END");
            } catch (SocketException e) {
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: SocketException receiveIP e");
            } catch (IOException e) {
                Log.d(PC, "WiFiDeviceManager.onConnectionInfoAvailable: IOException receiveIP e");
            }
            return null;
        }
    }
}
