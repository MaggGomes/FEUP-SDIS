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
	private Handler handler = null;
	private FileOutputStream outputStream = null;
	private BufferedReader reader = null;
	private String fileName = null;
	public Boolean mIsDataToWrite = false;
	private String roomID = null;
	private Context context = null;
	boolean isReader = false;
	boolean isToStop = false;
	public StringBuilder dataToWrite = new StringBuilder("");
	
	/**
	 * Handles read from file and write to file operations.
	 * @param RoomID
	 * @param handler - a reader thread will use this handler to return a result
	 * @param isReader
	 */
	public FileHandler(String RoomID, Handler handler, boolean isReader, Context con) {
		this.roomID = RoomID;
		this.context = con;
		this.isReader = isReader;
		this.handler = handler;
		fileName = roomID+".txt";

		initReader();
	}

	public void initReader(){
		try {
			if (!isReader)
				outputStream = context.openFileOutput(fileName, Context.MODE_APPEND);
			else
				reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Kill() {
		isToStop = true;
	}
	
	@Override
	public void run() {
		while (!isToStop) {
			if (mIsDataToWrite) {
				WriteToFile();
			}

			if (isReader) {
				ReadEntireFile();
				break;
			}
			
			try {
				sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (mIsDataToWrite)
			WriteToFile();
		
		if (handler!=null && !isReader)
			handler.sendEmptyMessage(0);
		
		try {
			if (outputStream!=null)
				outputStream.close();
			if (reader!=null)
				reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void WriteToFile () {
		synchronized (dataToWrite) {
			try {
				outputStream.write(dataToWrite.toString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			dataToWrite.setLength(0);
			mIsDataToWrite = false;
		}
	}

	/**
	 * Read from a file and send a msg with the content to the handler
	 */
	private void ReadEntireFile () {
		String inputString = null;
		StringBuffer buffer = new StringBuffer();
		boolean isDataFileNotEmpty = false;
		
		if (reader!=null) {
		    try {
				while ((inputString = reader.readLine()) != null) {
				    buffer.append(inputString + Constants.STANDART_FIELD_SEPERATOR);
					isDataFileNotEmpty = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	    Bundle bundle = new Bundle();
	    Message msg = handler.obtainMessage();  //get a new message from the handler
	    if (!isDataFileNotEmpty) {
			bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, true);
			bundle.putString(Constants.FILE_THREAD_DATA_CONTENT_KEY, buffer.toString());
	    }
	    else {
			bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false);  //mark that no data was read
	    }

	    msg.setData(bundle);
	    handler.sendMessage(msg);
	}

	public void UpdateDataToWriteBuffer (String data) {
		synchronized (dataToWrite) {
			dataToWrite.append(data);
			mIsDataToWrite=true;
		}
	}
}
