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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.Semaphore;

import androidx.annotation.RequiresApi;

public class ClientHandler extends Thread {

    Socket s;

    private Handler messageHandler;

    private Message message;

    private Bundle messageBundle;

    private StringBuilder messageStringBuilder;


    private final Semaphore semaphore;


    public static final String DATA = "DATA";


    public static final String CGI_URL = "cgi-bin";

    public ClientHandler(Socket socket, Handler messageHandler, Semaphore semaphore){
        this.s = socket;
        this.messageHandler = messageHandler;
        this.semaphore = semaphore;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        try {
            this.messageStringBuilder = new StringBuilder();
            this.message = new Message();
            this.messageBundle = new Bundle();


            this.messageStringBuilder.append("-------------------------------------------------------------------------------------" +
                    "\n" + "IP adress: " + s.getInetAddress().getHostAddress() + "\n");

        OutputStream o = s.getOutputStream();
        InputStream i = s.getInputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));


        this.messageHandler.sendEmptyMessage(1);

        String tmp = in.readLine();
        String filename = "";

        // CGI
        boolean isCGI = false;
        String cgiCommand = "";


        while (tmp != null && !tmp.isEmpty()){
            String req[] = tmp.split(" ");
            Log.d("SERVER", "HTTP REQ : " + tmp);

            if (req[1].contains(CGI_URL)) {
                String request = req[1];
                Log.d("CGI request", request);
                isCGI = true;

                int cgiIndex = request.indexOf(CGI_URL)+CGI_URL.length()+1;
                int endIndex = request.indexOf(" ", cgiIndex);
                if (endIndex == -1) {
                    endIndex = request.length();
                }
                String encodedCommandToExecute = request.substring(cgiIndex, endIndex);


                StringBuilder decodedCommandToExecute = new StringBuilder();
                decodedCommandToExecute.append(URLDecoder.decode(encodedCommandToExecute, "UTF-8"));
                Log.d("CGI decoded command", decodedCommandToExecute.toString());

                cgiCommand = decodedCommandToExecute.toString();

            }

            if(req[1].contains(".jpg")){
                Log.d("IMAGE REQUEST", req[1]);
                filename = "IMG.jpg";
            }
            if(req[1].contains(".html")){
                Log.d("HTML REQUEST", req[1]);
                filename = "index.html";
            }
            if(req[0].contains("Host") || req[0].contains("Accept")) {
                this.messageStringBuilder.append(tmp + "\n");
            }
            tmp = in.readLine();
        }

        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        if(isCGI) {


            if(cgiCommand.length() > 0) {
                StringBuilder cgiStringBuilder = new StringBuilder();

                try {
                    // Create the CGI storage directory if it does not exist
                    File cgiDir = new File(sdPath + "/OSMZ/CGI");
                    if (! cgiDir.exists()){
                        if (! cgiDir.mkdirs()){
                            Log.d("CGI", "failed to create directory");
                            return;
                        }
                    }


                    ProcessBuilder pb;
                    if(cgiCommand.contains("cat")){
                        pb = new ProcessBuilder("cat", cgiCommand.split(" ")[1]);
                    } else {
                        pb = new ProcessBuilder(cgiCommand);
                    }
                    final Process p = pb.start();

                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    BufferedWriter bw = new BufferedWriter(
                            new FileWriter(new File(sdPath + "/OSMZ/CGI/cgiOutput.txt")));

                    String line;
                    while ((line = br.readLine()) != null) {
                        bw.write(line);
                        cgiStringBuilder.append(line + "\n");
                    }
                    bw.close();


                    if(cgiStringBuilder.length() > 0) {
                        Log.d("CGI OUTPUT", cgiStringBuilder.toString());


                        out.write("HTTP/1.0 200 OK\n");
                        out.write("CGI OUTPUT:\n\n");
                        out.write(cgiStringBuilder.toString());
                        out.flush();
                    } else {
                        Log.d("CGI EMPTY", "Requested command is not valid to extract");
                        out.write("HTTP/1.0 404 Not Found\n\n");
                        out.write("CGI OUTPUT ERROR:\n\n");
                        out.write("Sorry sir/madam,\nRequested command is not valid to extract.");
                        out.flush();
                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }

        } else {
            try {

                File file;


                Log.d("FILE", sdPath);

                out.write("HTTP/1.0 200 OK\n");

                if (filename.endsWith(".html")) {
                    out.write("Content-Type: text/html\n");
                    file = new File(sdPath + "/OSMZ/index.html");
                } else if ((filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
                    out.write("Content-Type: image/jpeg\n");
                    file = new File(sdPath + "/OSMZ/how_to_do_coding.jpg");
                } else if (filename.contains("error")) {
                    throw new FileNotFoundException();
                } else {
                    out.write("Content-Type: text/html\n");
                    file = new File(sdPath + "/OSMZ/index.html");
//                throw new FileNotFoundException();
                }

                out.write("Content-Length: " + String.valueOf(file.length()) + "\n");
                out.write("\n");
                out.flush();


                this.messageStringBuilder.append("Objem dat: " + String.valueOf(file.length()) + " bytes\n");

                this.messageBundle.putString(DATA, this.messageStringBuilder.toString());
                this.message.setData(this.messageBundle);
                this.messageHandler.sendMessage(message);

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
                messageHandler.sendEmptyMessage(-1);
                semaphore.release();
            }
        }


        } catch (IOException e) {
            e.printStackTrace();
            messageHandler.sendEmptyMessage(-1);
        } finally {

            try {
                Log.d("SERVER", "Socket Closed");
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            messageHandler.sendEmptyMessage(-1);

            semaphore.release();
        }
    }
}
