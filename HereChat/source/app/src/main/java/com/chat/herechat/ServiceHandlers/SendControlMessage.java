package com.chat.herechat.ServiceHandlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.chat.herechat.Utilities.Constants;


public class SendControlMessage extends Thread {
	private Socket Socket;
	private Handler Handle;
	private String message;
	private String peerIp;
	private String RoomUniqueID;
	private static final int SOCKET_PORT = 4000;
	
	public SendControlMessage(Handler h, String peerIP, String msg, String RoomUniqueID) {
		Handle = h;
		peerIp = peerIP;
		message = msg;
		this.RoomUniqueID =RoomUniqueID;
	}
	
	public SendControlMessage(String peerIP, String msg) {
		this(null,peerIP,msg,null);
	}

	@Override
	public void run() {
		PrintWriter toSend =null;
		try {

			Socket = new Socket();
			Socket.bind(null);
		    Socket.connect((new InetSocketAddress(peerIp, SOCKET_PORT)), 3000);
		    toSend =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(Socket.getOutputStream())), true);
		} catch (IOException e) {
			SendHandleMessage(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED);
			e.printStackTrace();
			return;
		}
			
		toSend.println(message); //send via socket
		toSend.flush();
		toSend.close();
		
		if (Handle !=null) //if we have a handler to return the result to
			SendHandleMessage(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS); //notify that the send was successful
	}
	
	private void SendHandleMessage(String result) {
		if (Handle !=null) {
			Message msg = Handle.obtainMessage();
			Bundle data = new Bundle();
			data.putString(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, RoomUniqueID);  //set the room's ID
			data.putString(Constants.SINGLE_SEND_THREAD_KEY_RESULT, result);  //put the result in the data
			msg.setData(data);
			Handle.sendMessage(msg);
		}
	}
}
