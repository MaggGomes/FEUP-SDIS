
package com.chat.herechat.Chat;

import android.os.Handler;
import android.util.Log;

import com.chat.herechat.HereChatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class ChatManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private InputStream is;
    private OutputStream os;
    private static final String TAG = "ChatManager";

    public ChatManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            handler.obtainMessage(HereChatActivity.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = is.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(HereChatActivity.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            os.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

}
