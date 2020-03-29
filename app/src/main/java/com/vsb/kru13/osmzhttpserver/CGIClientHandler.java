package com.vsb.kru13.osmzhttpserver;

import android.os.Build;
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
import java.util.Map;
import java.util.concurrent.Semaphore;

import androidx.annotation.RequiresApi;

public class CGIClientHandler extends Thread {

    Socket s;

    public static final String DATA = "DATA";

    public CGIClientHandler(Socket socket){
        this.s = socket;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        try {

            OutputStream o = s.getOutputStream();
            InputStream i = s.getInputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));


            String tmp = in.readLine();
            String filename = "";
            boolean isCGI = false;


            while (tmp != null && !tmp.isEmpty()) {
                String req[] = tmp.split(" ");
                Log.d("SERVER", "HTTP REQ : " + tmp);
                if (req[1].contains("cgi-bin")) {
                    Log.d("IMAGE REQUEST", req[1]);
                    filename = "IMG.jpg";
                    isCGI = true;
                }
//                if(req[1].contains(".jpg")){
//                    Log.d("IMAGE REQUEST", req[1]);
//                    filename = "IMG.jpg";
//                }
//                if(req[1].contains(".html")){
//                    Log.d("HTML REQUEST", req[1]);
//                    filename = "index.html";
//                }
                tmp = in.readLine();
            }

            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

            if (isCGI) {
                try {

                    Log.d("CGI", "TRUEEEEEE");

                    File file;

                    ProcessBuilder pb =
                            new ProcessBuilder("uptime", "myArg1", "myArg2");
                    Map<String, String> env = pb.environment();
                    env.put("VAR1", "myValue");
                    env.remove("OTHERVAR");
                    env.put("VAR2", env.get("VAR1") + "suffix");
                    pb.directory(new File(sdPath+"/OSMZ/CGI/"));
                    File log = new File("log");
                    pb.redirectErrorStream(true);
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
                    Process p = pb.start();
                    assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
                    assert pb.redirectOutput().file() == log;
                    assert p.getInputStream().read() == -1;


                    out.write("HTTP/1.0 200 OK\n");

//                if (filename.endsWith(".html")) {
//                    out.write("Content-Type: text/html\n");
//                    file = new File(sdPath+"/OSMZ/index.html");
//                } else if ((filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
//                    out.write("Content-Type: image/jpeg\n");
//                    file = new File(sdPath+"/OSMZ/how_to_do_coding.jpg");
//                } else if(filename.contains("error")){
//                    throw new FileNotFoundException();
//                }
//                else {
                    out.write("Content-Type: text/html\n");
                    file = new File(sdPath + "/OSMZ/index.html");
//                throw new FileNotFoundException();
//                }

                    out.write("Content-Length: " + String.valueOf(file.length()) + "\n");
                    out.write("\n");
                    out.flush();


//                    FileInputStream fileInputStream = new FileInputStream(file);
//
//                    int c;
//                    byte[] fileBytes = new byte[2048];
//
//                    while ((c = fileInputStream.read(fileBytes)) != -1) {
//                        o.write(fileBytes, 0, c);
//                    }
//                    fileInputStream.close();

                } catch (FileNotFoundException ex) {
                    Log.d("FILE NOT FOUND", "File not found. ");
                    out.write("HTTP/1.0 404 Not Found\n\n");
                    out.write("Sorry sir/madam, but page was not found.");
                    out.flush();
                }

            }
        } catch(IOException e){
            e.printStackTrace();
        } finally{

            try {
                Log.d("SERVER", "Socket Closed");
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
