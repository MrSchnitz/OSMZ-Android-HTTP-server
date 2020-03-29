package com.bau0025.osmzhttpserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class HttpServerService extends Service {

    private SocketServer s;

    private final IBinder mIBinder = new LocalBinder();

    private Handler mHandler = null;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("HTTP SERVICE", "created");

    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId)
    {
        Log.d("HTTP SERVICE", "started");
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(mHandler != null)
        {
            mHandler = null;
        }

        if(s != null && s.bRunning) {
            s.close();
            try {
                s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("HTTP SERVICE", "stopped");
        Toast.makeText(this, "OSMZ Http server service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mIBinder;
    }

    public class LocalBinder extends Binder
    {
        public HttpServerService getInstance()
        {
            return HttpServerService.this;
        }
    }

    public void setHandler(Handler handler)
    {
        if(handler != null) {
            mHandler = handler;

            s = new SocketServer(this.mHandler);
            s.start();
        } else {
            s = new SocketServer(SocketServerType.CAMERA);
            s.start();
        }
    }


    public void stopService() {
        if(mHandler != null)
        {
            mHandler = null;
        }

        if(s != null && s.bRunning) {
            s.close();
            try {
                s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stopForeground(true);
        stopSelf();
    }

}
