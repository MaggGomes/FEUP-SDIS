package com.chat.herechat.ServiceHandlers;

import com.chat.herechat.LocalService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerSocketHandler extends Thread {
    private ServerSocket serverSocket;
    private LocalService service;
    private static final int SOCKET_PORT = 4000;


    public ServerSocketHandler(LocalService service) {
        this.service = service;

        try {
            serverSocket = new ServerSocket(SOCKET_PORT);
        } catch (IOException e) {
            this.service.OnServerSocketError();
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            Socket client;
            try {
                client = serverSocket.accept();
            } catch (IOException e) {
                stopSocket();
                e.printStackTrace();
                return;
            }

            ClientSocketHandler worker = new ClientSocketHandler(service, client);
            worker.setPriority(MAX_PRIORITY);
            worker.start();
        }
    }


    public void stopSocket() {
        if (serverSocket != null && (!serverSocket.isClosed())) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }
}
