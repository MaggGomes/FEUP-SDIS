package com.chat.herechat.SocketHandlers;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerSocketHandler extends Thread {

    private static final String TAG = "ServerSocketHandler";
    private static final int SERVER_PORT = 4445;
    private ArrayList<InetAddress> clients = new ArrayList<>();
    private ServerSocket serverSocket;

    public ServerSocketHandler(){
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            Log.d(TAG, "Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket clientSocket = serverSocket.accept();
                if(!clients.contains(clientSocket.getInetAddress())){
                    clients.add(clientSocket.getInetAddress());
                    Log.d(TAG, "New client: " + clientSocket.getInetAddress().getHostAddress());
                }

                clientSocket.close();
            } catch(IOException e){
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            serverSocket.close();
            Log.d(TAG, "Server handler interrupted");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<InetAddress> getClients() {
        return clients;
    }
}
