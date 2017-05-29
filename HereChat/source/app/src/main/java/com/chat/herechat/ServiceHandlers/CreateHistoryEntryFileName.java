package com.chat.herechat.ServiceHandlers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;

import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.ChatManager.ChatHistoryScreenFrag;




@SuppressLint("HandlerLeak")
public class CreateHistoryEntryFileName extends Thread {
	private FileWritter fHandler =null;
	private Handler workHandler =null;
	private Handler resultHandler =null;

	private Activity action =null;
	private ChatHistoryScreenFrag.HistoryEntry historyEntry =null;

	
	public CreateHistoryEntryFileName(String fileName, FragmentActivity fragmentActivity, Handler resHandler, ChatHistoryScreenFrag.HistoryEntry entery) {
		action = fragmentActivity;
		historyEntry =entery;
		historyEntry.mID= fileName.split("[.]")[0]; //name.txt => name
		resultHandler = resHandler;
		
		workHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {

				Bundle msgData = msg.getData();
				if (msgData.getBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false)){
					String tmp= msgData.getString(Constants.FILE_THREAD_DATA_CONTENT_KEY, null);
					String[] data= tmp.split("["+Constants.STANDART_FIELD_SEPERATOR+"]") ;

					historyEntry.mRoomName= new String( data[0] );
					historyEntry.isPrivate= (data[1].equalsIgnoreCase("private"));
					historyEntry.isEmpty = data.length <= 2;
					Message resultMsg = resultHandler.obtainMessage();
					resultHandler.sendMessage(resultMsg);
				}
			}
		};
	}

	@Override
	public void run() {
		super.run();
		fHandler = new FileWritter(historyEntry.mID, workHandler, true, action);
		fHandler.start();

		try {
			fHandler.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
