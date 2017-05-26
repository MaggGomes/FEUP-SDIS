package com.chat.herechat.servicehandlers;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.chat.herechat.Constants;

/**
 * Handles file read/write operations.
 * If this read is run as a reader, it reads the entire file, replaces all "\r\n"
 * occurrences with 'Constants.STANDART_FIELD_SEPERATOR', sends it as a single string via handler and finishes.
 * If this thread is run as a writer, it runs infinitely until stopped.
 * In writer mode, the data to write should be inserted via UpdateDataToWriteBuffer(String). When this writer thread is no
 * longer needed, Kill() should be called. If a handler was passed to a writer thread, an empty message will be sent just before
 * termination, indicating that the writing operation is complete.
 */
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
	
	/**
	 * Handles read from file and write to file operations. 
	 * If the thread is constructed as a reader, it reads the entire file and sends the result via handler
	 * If the thread is constructed as a writer, it runs infinitely and writes data to the file as it becomes available
	 * @param RoomID - will be used for the file's name
	 * @param handler - a reader thread will use this handler to return a result
	 * @param isReader - defines if it's a reader or a writer
	 */
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

	/**
	 * Writes the entire buffer to the file
	 */
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
	
	/**
	 * Read from a file and send a msg with the content to the handler
	 */
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
