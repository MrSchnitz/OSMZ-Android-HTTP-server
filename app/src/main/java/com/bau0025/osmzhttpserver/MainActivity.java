package com.bau0025.osmzhttpserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s;
    private Integer threadCount = 0;
    private Map<Integer, String> messageMap = new HashMap<>();
    private List<String> messageList = new ArrayList<>();

    TextView threadTextView;
    TextView messageTextView;

    private static final int READ_EXTERNAL_STORAGE = 1;


    /**
     * HTTP Server service
     */
    private HttpServerService httpServerService = null;
    private Intent httpServerServiceIntent = null;
    private boolean isServiceBound;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);

        Button cameraSwitchButton = (Button)findViewById(R.id.camera_switch_button);


        this.threadTextView = (TextView)findViewById(R.id.threadTextView);
        this.messageTextView = (TextView)findViewById(R.id.messageTextView);

        this.messageTextView.setMovementMethod(new ScrollingMovementMethod());

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        cameraSwitchButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button1) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {

                // WITH SERVICE
                this.httpServerServiceIntent = new Intent(this.getBaseContext(), HttpServerService.class);
                startService(httpServerServiceIntent);
                doBindService();

                // WITHOUT SERVICE
//                s = new SocketServer(this.messagehandler);
//                s.start();
            }
        }
        if (v.getId() == R.id.button2) {

            // WITH SERVICE
            if(this.httpServerServiceIntent != null) {
                this.httpServerService.stopService();
            }

            // WITHOUT SERVICE
//            s.close();
//            try {
//                s.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        if(v.getId() == R.id.camera_switch_button) {
            if(s != null && s.bRunning) {
                s.close();
                try {
                    s.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    s = new SocketServer(this.messagehandler);
                    s.start();
                }
                break;

            default:
                break;
        }
    }

    private Handler messagehandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the image task from the incoming Message object.
            super.handleMessage(inputMessage);
            if(inputMessage.what == 1 || inputMessage.what == -1 && threadCount > 0) {
//                if(inputMessage.what == -1){
//                    messageMap.remove(threadCount);
//                }
                threadCount += inputMessage.what;
                threadTextView.setText("Active Threads: " + threadCount);
            }


            if(inputMessage.getData().get(ClientHandler.DATA) != null){
                messageMap.put(threadCount, String.valueOf(inputMessage.getData().get(ClientHandler.DATA)));
                messageList.add(String.valueOf(inputMessage.getData().get(ClientHandler.DATA)));
                StringBuilder stringBuilder = new StringBuilder();
//                for(String data : messageMap.values()){
//                    stringBuilder.append(data);
//                }
                for(String data : messageList){
                    stringBuilder.append(data);
                }
                messageTextView.setText(stringBuilder.toString());
            }

        }
    };




    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            httpServerService = ((HttpServerService.LocalBinder)iBinder).getInstance();
            httpServerService.setHandler(messagehandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            httpServerService = null;
        }
    };


    private void doBindService()
    {
        bindService(new Intent(this,
                HttpServerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        isServiceBound = true;
    }

    private void doUnbindService()
    {
        if (isServiceBound)
        {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        doUnbindService();
    }
}
