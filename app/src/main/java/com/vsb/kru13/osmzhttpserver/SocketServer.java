package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.os.Handler;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;


public class SocketServer extends Thread {

    private SocketServerType serverType;

    ServerSocket serverSocket;
    private Handler messageHandler;
    public final int port = 12345;
    boolean bRunning;

    private int MAX_THREADS = 10;
    private int SEMAPHORE_TIMEOUT = 2000;


    private Semaphore semaphore = new Semaphore(MAX_THREADS);

    public SocketServer(SocketServerType socketServerType){
        this.serverType = socketServerType;
    }

    public SocketServer(Handler messageHandler) {
        this.messageHandler = messageHandler;
        this.serverType = SocketServerType.MAIN;
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("SERVER", "Socket Accepted");

                if(serverType == SocketServerType.CAMERA){
                    CameraClientHandler cameraClientHandler = new CameraClientHandler(s);
                    cameraClientHandler.start();
                } else {
                    if (semaphore.tryAcquire()) {
                        ClientHandler clientHandler = new ClientHandler(s, this.messageHandler, semaphore);
                        clientHandler.start();
                    } else {
                        Log.d("ERROR", "Not available permits");
                    }

//                    CGIClientHandler cgiClientHandler = new CGIClientHandler(s);
//                    cgiClientHandler.start();
                }

            }
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }

}

