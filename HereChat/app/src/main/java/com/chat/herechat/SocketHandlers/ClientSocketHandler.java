
package com.chat.herechat.SocketHandlers;

import android.os.Handler;
import android.util.Log;

import com.chat.herechat.Chat.ChatManager;
import com.chat.herechat.HereChatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private ChatManager chat;
    private InetAddress address;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(address.getHostAddress(), HereChatActivity.SERVER_PORT), HereChatActivity.TIMEOUT);
            Log.d(TAG, "Launching the I/O handler");
            chat = new ChatManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public ChatManager getChat() {
        return chat;
    }

}
