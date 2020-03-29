package com.bau0025.osmzhttpserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private WebCamera webCamera;

    private SocketServer socketServer;

    Button captureButton;

    public static byte[] pictureData = null;
    public static boolean savePicture = true;


    private static String CAMERA_TAG = "CAMERA";
    private static String CAMERA_ERROR_TAG = "CAMERA_ERROR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        setContentView(R.layout.activity_camera);


        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        webCamera = new WebCamera(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(webCamera);


        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(CAMERA_TAG, "Taking picture...");

                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );


        socketServer = new SocketServer(SocketServerType.CAMERA);
        socketServer.start();

        startTimer(4500);
    }

    private void startTimer(long time){
        CountDownTimer counter = new CountDownTimer(3600000, time){
            public void onTick(long millisUntilDone){

//                Log.d("counter_label", "Counter text should be changed");
                mCamera.takePicture(null, null, mPicture);
            }

            public void onFinish() {
                Log.d("COUNTER", "DONE");

            }
        }.start();
    }


    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
        }
        return c;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            camera.startPreview();

            if (pictureFile == null){
                Log.d(CAMERA_ERROR_TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                pictureData = data;

                if(savePicture) {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Log.d(CAMERA_TAG, "Picture saved " + pictureFile.toString());
                }

            } catch (FileNotFoundException e) {
                Log.d(CAMERA_ERROR_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(CAMERA_ERROR_TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){

        String socketCameraPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/OSMZ/SocketCamera";

//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        File mediaStorageDir = new File(socketCameraPath);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
//            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
//                    "IMG_"+ timeStamp + ".jpg");
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG.jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
