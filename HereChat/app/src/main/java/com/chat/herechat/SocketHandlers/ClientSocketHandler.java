package com.chat.herechat.SocketHandlers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ServerSocketHandler";
    private static final int PORT = 4445;
    private InetAddress serverAddr;

    public ClientSocketHandler(InetAddress serverAddr){
        this.serverAddr = serverAddr;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(serverAddr, PORT));
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
