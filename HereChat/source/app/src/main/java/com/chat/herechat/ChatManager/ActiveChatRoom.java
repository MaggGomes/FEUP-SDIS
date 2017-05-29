package com.chat.herechat.ChatManager;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.chat.herechat.ServiceHandlers.FileWritter;
import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.Peer.Peer;
import com.chat.herechat.LocalService;
import com.chat.herechat.MainScreenActivity;
import com.chat.herechat.ServiceHandlers.SendControlMessage;



public class ActiveChatRoom {
	public ChatRoomDetails roomInfo;            //reference to to the details object of this room
	public boolean isPublicHosted;	          //indicates whether this is a hosted public chat or not
	private LocalService service;
	private Handler fileWriterResultHandler =null;			//used for receiving messages from a 'FileWritter'
	private Semaphore semaphore = new Semaphore(1, true);   //used to sync between file writers

	@SuppressLint("HandlerLeak")
	public ActiveChatRoom(LocalService serv, boolean isHosted, ChatRoomDetails info) {
		service = serv;
		this.isPublicHosted = isHosted;
		roomInfo = info;

		Vector<ChatRoomDetails> temp = service.getChatRooms();

		//define a handler to be triggered when the file-writer completes the job
		fileWriterResultHandler = new Handler(service.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				ActiveChatRoom.this.semaphore.release(); //release the semaphore when the file is read
			}
		};

	}


	public void SendMessage(boolean self, String[] msg) {
		System.out.println(213);
		if (!self) {
			if (!roomInfo.hasNewMsg) //on a state change of this flag
				service.BroadcastRoomsUpdatedEvent(); //notify that this room has an unread message
			roomInfo.hasNewMsg=true; //mark this room has received a message
		}
		String senderUniqueID = msg[2];  //get the sender unique ID
		String entireMsg = Constants.SeparateArray2String(msg,Constants.STANDART_FIELD_SEPERATOR);  //convert back to the original string

		// we need to forward an incoming message only if this chat is hosted or this chat isn't hosted and it's a self message
		if (isPublicHosted || (!isPublicHosted && self)) {
			for (Peer user : roomInfo.users)  //for every participating user
			{
				if (!user.uniqueID.equalsIgnoreCase(senderUniqueID)) //if this user isn't the one who sent this message (done to avoid loopbacks)
					{
					new SendControlMessage(null,user.IPaddress,entireMsg, roomInfo.roomID).start(); //forward the msg
					}
			}
		}

		if (!self) {
			//No we change the incoming string to a file writeable format
			String[] temp = new String[4];
			temp[0] = msg[1];
			temp[1] = msg[2];
			temp[2] = msg[4];
			temp[3] = Constants.getTimeString();

			UpdateFileWithNewMessage( Constants.SeparateArray2String(temp,Constants.CHAT_MSG_ENTRY_SEPARATOR_CHAR)); //write to history file

			//launch a broadcast with the received msg:
			Intent intent = service.CreateBroadcastIntent();
			intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.CONNECTION_CODE_NEW_CHAT_MSG); //set opcode to new msg
			intent.putExtra(Constants.SERVICE_BROADCAST_MSG_CONTENT_KEY, msg);  //set the msg as it came via the socket
			intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, roomInfo.roomID); //set the room's ID
			service.sendBroadcast(intent);
		}
	}


	public void UpdateFileWithNewMessage(String msg) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		FileWritter fw = new FileWritter(roomInfo.roomID, fileWriterResultHandler,false, service);
		fw.UpdateDataToWriteBuffer(msg);
		fw.start();
		fw.Kill();
	}


	public String AddUser (Peer user, String suggestedPass) {
		//check if this user already exists in the room:
		if ( Constants.CheckUniqueID(user.uniqueID, roomInfo.users)!=null)
			return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
		//check if the pw is correct:
		if (roomInfo.password !=null && !suggestedPass.equalsIgnoreCase(roomInfo.password))
			return Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW;
		//add
		roomInfo.users.add(user);  //add this user to the mailing list
		//return positive reply
		return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
	}


	public void DeleteHistory() {
		String path  = service.getFilesDir().getPath()+ "/" + roomInfo.roomID + ".txt";
		File file = new File(path);
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (file.delete())  //delete the file. if successful, rebuild a new file template
			ChatActivity.initHistoryFile(fileWriterResultHandler, roomInfo.roomID, roomInfo.name, service, roomInfo.isPrivateChatRoom);
		else //if the file deletion has failed
			semaphore.release();
	}


	@SuppressLint("HandlerLeak")
	public void UpdateUserInHistory(){
		Handler msgHandler = new Handler(){ //define a new message handler for the file thread
			//Here we'll receive the content of the history file that was read by a thread
			@Override
			public void handleMessage(Message msg) {

				String []fileContent=new String[1]; 	//parse the data and update the list view

				//load the file into fileContent[0] as one long string. lines are separated by STANDART_FIELD_SEPERATOR
				if (msg.getData().getBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false)){ //if a history exists
					fileContent[0]= ((msg.getData().getString(Constants.FILE_THREAD_DATA_CONTENT_KEY, null)));

					//now each member in fileContent[] array contains one line from the file
					fileContent= fileContent[0].split("["+Constants.STANDART_FIELD_SEPERATOR+"]");
					fileContent[0]= roomInfo.users.get(0).name;
					String atos = StringArraySeperated(fileContent);

					File file = new  File(service.getFilesDir().getPath()+ "/" + roomInfo.roomID +".txt");
					//is the history was deleted we want to create new one
					if(file.delete())
						UpdateFileWithNewMessage(atos);
				}
			}// handleMessage(Message msg)
		};

		//read file
		new FileWritter(roomInfo.roomID, msgHandler, true, ActiveChatRoom.this.service).start();
	}



	private String StringArraySeperated(String [] input){
		StringBuilder buffer = new StringBuilder();
		int length = input.length;
		for (int i=0; i<length ;i++) {
			buffer.append(input[i]);
			buffer.append("\r\n");
		}
		return new String(buffer.toString());
	}


	@Override
	public String toString() {
		StringBuilder ans = new StringBuilder();
		ans.append(roomInfo.name +Constants.STANDART_FIELD_SEPERATOR); //set the name
		ans.append(roomInfo.roomID +Constants.STANDART_FIELD_SEPERATOR); //set the ID
		ans.append(roomInfo.users.isEmpty()? " " : Constants.UserListToString(roomInfo.users)); //set the users string
		ans.append(Constants.STANDART_FIELD_SEPERATOR);
		ans.append((roomInfo.password ==null? "false" : "true")+Constants.STANDART_FIELD_SEPERATOR); //set password

		return ans.toString();
	}

	public void CloseRoomNotification() {
		StringBuilder msg = new StringBuilder();

		msg.append(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY) + Constants.STANDART_FIELD_SEPERATOR); //set opcode
		msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
		msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
		msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set negative result
		msg.append(Constants.SERVICE_NEGATIVE_REPLY_REASON_ROOM_CLOSED + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
		msg.append(roomInfo.roomID + Constants.STANDART_FIELD_SEPERATOR);  //set room's ID

		for (Peer user : roomInfo.users) {
			new SendControlMessage(null,user.IPaddress,msg.toString(), roomInfo.roomID).start(); //send the msg
		}
	}


	public void DisconnectFromHostPeer() {
		StringBuilder msg = new StringBuilder();
		 //set the 'Leave room' opcode:
		msg.append(Integer.toString(Constants.CONNECTION_CODE_DISCONNECT_FROM_CHAT_ROOM) + Constants.STANDART_FIELD_SEPERATOR);
		msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
		msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
		msg.append(roomInfo.roomID + Constants.STANDART_FIELD_SEPERATOR);  //set the room's ID

		new SendControlMessage(roomInfo.users.get(0).IPaddress, msg.toString()).start();  //send the reply
	}
}
