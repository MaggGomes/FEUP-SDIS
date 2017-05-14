
package com.chat.herechat.SocketHandlers;

import android.os.Handler;
import android.util.Log;

import com.chat.herechat.Chat.ChatManager;
import com.chat.herechat.HereChatActivity;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerSocketHandler extends Thread {

    private ServerSocket socket;
    private final int THREAD_COUNT = 10;
    private Handler handler;
    private static final String TAG = "ServerSocketHandler";
    private ThreadPoolExecutor workers;

    public ServerSocketHandler(Handler handler) throws IOException {
        try {
            socket = new ServerSocket(HereChatActivity.SERVER_PORT);
            this.handler = handler;
            workers = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            Log.d("TAG",  "Server socket started");
        } catch (IOException e) {
            e.printStackTrace();
            workers.shutdownNow();
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Log.d(TAG, "Worker started");
                workers.execute(new ChatManager(socket.accept(), handler));
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                e.printStackTrace();
                workers.shutdownNow();
                break;
            }
        }
    }

}
