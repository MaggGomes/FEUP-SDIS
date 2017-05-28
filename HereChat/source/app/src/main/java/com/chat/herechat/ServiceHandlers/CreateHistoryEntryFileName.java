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
	private Handler mResultHanlder = null;
	private FileHandler mFileHandler = null;
	private Activity mActivity = null;
	private ChatHistoryScreenFrag.HistoryEntry mEntry = null;
	private Handler mWorkerHandler = null;
	
	public CreateHistoryEntryFileName(String fileFullName, ChatHistoryScreenFrag.HistoryEntry entery, FragmentActivity fragmentActivity, Handler resHandler) {
		mResultHanlder = resHandler;
		mActivity = fragmentActivity;
		mEntry = entery;
		mEntry.mID = fileFullName.split("[.]")[0];

		mWorkerHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				Bundle msgData = msg.getData();
				if (msgData.getBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false)){ //if a history exists
					String tmp= msgData.getString(Constants.FILE_THREAD_DATA_CONTENT_KEY, null);
					String[] data= tmp.split("["+Constants.STANDART_FIELD_SEPERATOR+"]");
					mEntry.mRoomName= new String(data[0]);
					mEntry.isEmpty = data.length <= 2;
					mEntry.isPrivate= (data[1].equalsIgnoreCase("private"))?true:false;
					Message resultMsg = mResultHanlder.obtainMessage();
					/* Notifies that a file was read */
					mResultHanlder.sendMessage(resultMsg);  //send an empty message, just to notify that a file was read.
				}
			}
		};
	}

	@Override
	public void run() {
		super.run();
		mFileHandler= new FileHandler(mEntry.mID, mWorkerHandler, true, mActivity);
		mFileHandler.start();

		try {
			mFileHandler.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
