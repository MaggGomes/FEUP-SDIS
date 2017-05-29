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



public class FileWritter extends Thread {
	private FileOutputStream fileOut = null;
	private BufferedReader fileIn = null;

	private Handler Handle = null;
	private String RoomID = null;
	private Context Context = null;

	private String FileName = null;
	public Boolean writable =false;
	public StringBuilder DataToWrite = new StringBuilder("");
	boolean isRead = false;
	boolean isKill =false;
	

	public FileWritter(String RoomID, Handler handler, boolean isReader, Context con) {
		this.RoomID = RoomID;
		isRead = isReader;
		FileName = this.RoomID +".txt";
		Context = con;
		Handle = handler;

		try {
			if (isReader)
				fileIn = new BufferedReader(new InputStreamReader(Context.openFileInput(FileName)));
			else
				fileOut = Context.openFileOutput(FileName, Context.MODE_APPEND); //Open a file for appending. Create it if necessary
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (!isKill) {
			if (isRead) {
				ReadFile();
				break;
			}
			if (writable) {
				WriteToFile();
			}
			
			try {
				sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (writable)
			WriteToFile();
		
		if (Handle !=null && !isRead)
			Handle.sendEmptyMessage(0);
		
		try {
			if (fileOut !=null)
				fileOut.close();
			if (fileIn !=null)
				fileIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void Kill() {
		isKill =true;
	}


	private void WriteToFile () {
		synchronized (DataToWrite) {
			try {
				fileOut.write(DataToWrite.toString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			DataToWrite.setLength(0);
			writable =false;
		}
	}
	
	public void UpdateDataToWriteBuffer (String data) {
		synchronized (DataToWrite) {
			DataToWrite.append(data);
			writable =true;
		}
	}
	

	private void ReadFile() {
		boolean isDataFileNotEmpty=false;
		String inputString=null;
		StringBuffer buffer = new StringBuffer();
		
		if (fileIn !=null) {
		    try {
				while ((inputString = fileIn.readLine()) != null) {
				    buffer.append(inputString + Constants.STANDART_FIELD_SEPERATOR);
					isDataFileNotEmpty=true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	    Bundle bundle = new Bundle();
	    Message msg = Handle.obtainMessage();  //get a new message from the handler
	    if (!isDataFileNotEmpty) {
	    	bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false);  //mark that no data was read
	    }
	    else {
	    	bundle.putBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, true);  //mark that the data was read
	    	bundle.putString(Constants.FILE_THREAD_DATA_CONTENT_KEY, buffer.toString());  //insert the entire read data
	    }

	    msg.setData(bundle);
	    Handle.sendMessage(msg);
	}
}
