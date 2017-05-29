package com.chat.herechat.ServiceHandlers;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.chat.herechat.Utilities.Constants;


public class FileHandler extends Thread {
	private Handler mHandler = null;
	private String mRoomID = null;
	private Context mContext = null;
	private FileOutputStream mOutputStream = null;
	private BufferedReader mReader = null;
	private String mFileName = null;
	public Boolean mIsDataToWrite=false;
	public StringBuilder mDataToWrite = new StringBuilder("");  //will hold the data to be written to the file
	boolean mIsReader = false; //our thread can be a reader or a writer
	boolean isToKill=false;
	

	public FileHandler(String RoomID, Handler handler, boolean isReader, Context con) {
		mRoomID = RoomID;
		mIsReader = isReader;
		mFileName = mRoomID+".txt";
		mContext = con;
		mHandler = handler;

		try {
			if (isReader)
				mReader = new BufferedReader(new InputStreamReader(mContext.openFileInput(mFileName)));		
			else
				mOutputStream = mContext.openFileOutput(mFileName, Context.MODE_APPEND); //Open a file for appending. Create it if necessary                                                           
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (!isToKill) {
			if (mIsReader) {
				ReadEntireFile();
				break; 			//end the thread
			}
			if (mIsDataToWrite) {
				WriteToFile();
			}
			
			try {
				sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (mIsDataToWrite)
			WriteToFile();
		
		if (mHandler!=null && !mIsReader)
			mHandler.sendEmptyMessage(0);
		
		try {
			if (mOutputStream!=null)
				mOutputStream.close();
			if (mReader!=null)
				mReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void Kill() {
		isToKill=true;
	}


	private void WriteToFile () {
		synchronized (mDataToWrite) {
			try {
				mOutputStream.write(mDataToWrite.toString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			mDataToWrite.setLength(0);
			mIsDataToWrite=false;
		}
	}
	
	public void UpdateDataToWriteBuffer (String data) {
		synchronized (mDataToWrite) {
			mDataToWrite.append(data);
			mIsDataToWrite=true;
		}
	}
	

	private void ReadEntireFile () {
		boolean isDataFileNotEmpty=false;
		String inputString=null;
		StringBuffer buffer = new StringBuffer();
		
		if (mReader!=null) {
		    try {
				while ((inputString = mReader.readLine()) != null) {
				    buffer.append(inputString + Constants.STANDART_FIELD_SEPERATOR);
					isDataFileNotEmpty=true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	    Bundle bundle = new Bundle();
	    Message msg = mHandler.obtainMessage();  //get a new message from the handler
	    if (!isDataFileNotEmpty) {
	    	bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false);  //mark that no data was read
	    }
	    else {
	    	bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, true);  //mark that the data was read
	    	bundle.putString(Constants.FILE_THREAD_DATA_CONTENT_KEY, buffer.toString());  //insert the entire read data
	    }

	    msg.setData(bundle);
	    mHandler.sendMessage(msg);
	}
}
