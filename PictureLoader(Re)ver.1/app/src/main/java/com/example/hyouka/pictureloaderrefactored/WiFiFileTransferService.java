package com.example.hyouka.pictureloaderrefactored;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Created by Hyouka on 4/7/2016.
 */
public class WiFiFileTransferService extends IntentService {
    String PC = "PictureLoader";

    public static final String ACTION_SEND_FACE_DATA = "com.example.hyouka.pictureloaderrefactored.SEND_FACE_DATA";
    public static final String EXTRAS_FACE_DATA = "face_data";

    public static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.hyouka.pictureloaderrefactored.SEND_FILE";
    public static final String ACTION_SEND_IP = "com.example.hyouka.pictureloaderrefactored.SEND_IP";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_IP_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";

    public WiFiFileTransferService(){
        super("WiFiFileTransferService");
    }
    public WiFiFileTransferService(String name){super(name);}
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(PC,"FileTransfer.onHandleIntent: begin transfer");
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);

            try {
                //Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                Log.d(PC,"FileTransfer.onHandleIntent: Opening a client socket");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(PC,"FileTransfer.onHandleIntent: Client socket - " + socket.isConnected());
                //Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));

                    Log.d(PC,"FileTransfer.onHandleIntent: file transfer executed");
                    Log.d(PC,"FileTransfer.onHandleIntent: file path: "+Uri.parse(fileUri).toString());
                } catch (FileNotFoundException e) {
                    Log.d(PC,"FileTransfer.onHandleIntent: FileNotFoundException e path: "+Uri.parse(fileUri).toString());
                }
                copyFile(is, stream);
                //Log.d(WiFiDirectActivity.TAG, "Client: Data written");
                Log.d(PC,"FileTransfer.onHandleIntent: END OF file transfer");
            } catch (IOException e) {
                //Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }else if(intent.getAction().equals(ACTION_SEND_IP)){
            try {
                String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
                int port = intent.getExtras().getInt(EXTRAS_PORT);
                Socket socket = new Socket();
                DataOutputStream outputStream = null;
                Log.d(PC,"WiFiDeviceManager.sendClientIP: client socket constructed");
                //socket.setReuseAddress(true);
                //Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: client reused address set to true");
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(PC,"WiFiDeviceManager.sendClientIP: connect to server socket");
                outputStream = new DataOutputStream(socket.getOutputStream());
                Log.d(PC,"WiFiDeviceManager.sendClientIP: get output stream");
                outputStream.writeUTF("IP");
                Log.d(PC,"WiFiDeviceManager.sendClientIP: write object");
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.d(PC,"WiFiDeviceManager.sendClientIP: Client IP sent");
            }catch (SocketException e){
                Log.d(PC,"WiFiDeviceManager.sendClientIP: sendIP SocketException e");
            } catch (IOException e) {
                Log.d(PC,"WiFiDeviceManager.sendClientIP: sendIP IOException e");
                e.printStackTrace();
            }
        }else if(intent.getAction().equals(ACTION_SEND_FACE_DATA)){
            try {
                Log.d(PC,"ACTION_SEND_FACE_DATA 0");
                String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
                int port = intent.getExtras().getInt(EXTRAS_PORT);
                String face_data = intent.getExtras().getString(EXTRAS_FACE_DATA);
                DataOutputStream output = null;
                Socket socket = new Socket();
                Log.d(PC,"ACTION_SEND_FACE_DATA 1");
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);// not connected ...
                Log.d(PC,"ACTION_SEND_FACE_DATA 2");
                output = new DataOutputStream(socket.getOutputStream());
                Log.d(PC,"ACTION_SEND_FACE_DATA 3");
                output.writeUTF(face_data);
                Log.d(PC,"ACTION_SEND_FACE_DATA 5");
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }



    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();

        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            //Log.v("","Time taken to transfer all bytes is : "+endTime);

        } catch (IOException e) {
            //Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
