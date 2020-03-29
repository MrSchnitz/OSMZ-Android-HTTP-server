package com.bau0025.osmzhttpserver;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class CameraClientHandler extends Thread {

    Socket s;


    public String SNAPSHOT_URL = "snapshot";
    public String STREAM_URL = "stream";


    public CameraClientHandler(Socket socket){
        this.s = socket;
    }


    @Override
    public void run() {
        try {
            OutputStream o = s.getOutputStream();
            InputStream i = s.getInputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));


            String tmp = in.readLine();
            String filename = "osmz_camera_refresh_index.html";
            boolean fromByteStreamImage = false;
            boolean multipart = false;


            while (tmp != null && !tmp.isEmpty()){
                String req[] = tmp.split(" ");
//                Log.d("SERVER", "HTTP REQ : " + req[0]);
                if(req[1].contains(".jpg")){
                    Log.d("IMAGE REQUEST", req[1]);
                    filename = "IMG.jpg";
                }
                if(tmp.contains("Referer")) {
                    if (tmp.contains(SNAPSHOT_URL) || tmp.contains(STREAM_URL)) {
                        CameraActivity.savePicture = false;
                        Log.d("Ref", tmp);
                    } else {
                        CameraActivity.savePicture = true;
                    }
                }
                if(req[1].contains(SNAPSHOT_URL) && filename.endsWith(".jpg")){
                    fromByteStreamImage = true;
                    Log.d("FROM BYTE STREAM IMAGE", "TRUE");
                }
                if(tmp.contains(STREAM_URL) && filename.endsWith(".jpg")){
                    multipart = true;
                    fromByteStreamImage = true;
                    out.write("Content-Type: multipart/x-mixed-replace; boundary=\"OSMZ_boundary\"\n");
                    out.write("--OSMZ_boundary\n");
                    out.write("Content-Type: image/jpeg\n");
                }
                tmp = in.readLine();
            }

            out.write("HTTP/1.0 200 OK\n");



            if (fromByteStreamImage){
                byte[] fileBytes = CameraActivity.pictureData;
                if(fileBytes != null) {
                    if(multipart){
//                        out.write("Content-Type: multipart/x-mixed-replace; boundary=\"OSMZ_boundary\"");
//                        out.write("--OSMZ_boundary");
//                        out.write("Content-Type: image/jpeg\n");
                        o.write(fileBytes);
                        out.write("--OSMZ_boundary\n");
                        out.write("Content-Type: image/jpeg\n");
                        out.write("Content-Length: " + String.valueOf(fileBytes.length) + "\n");
                        out.write("\n");
                        out.flush();
                    } else {
                        out.write("Content-Type: image/jpeg\n");
                        out.write("Content-Length: " + String.valueOf(fileBytes.length) + "\n");
                        out.write("\n");
                        out.flush();

                        o.write(fileBytes);
                    }

                }
            } else {

                String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

                try {


                    if (filename.endsWith(".html")) {
                        out.write("Content-Type: text/html\n");
                    }
                    if (filename.endsWith(".jpg")) {
                            out.write("Content-Type: image/jpeg\n");
                    }


                    File file = new File(sdPath + "/OSMZ/SocketCamera/" + filename);

                    out.write("Content-Length: " + String.valueOf(file.length()) + "\n");
                    out.write("\n");
                    out.flush();

                    FileInputStream fileInputStream = new FileInputStream(file);

                    int c;
                    byte[] fileBytes = new byte[2048];

                    while ((c = fileInputStream.read(fileBytes)) != -1) {
                        o.write(fileBytes, 0, c);
                    }
                    fileInputStream.close();

                } catch (FileNotFoundException ex) {
                    Log.d("FILE NOT FOUND", "File not found. ");
                    out.write("HTTP/1.0 404 Not Found\n\n");
                    out.write("Sorry sir/madam, but page was not found.");
                    out.flush();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Log.d("SERVER", "Socket Closed");
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
