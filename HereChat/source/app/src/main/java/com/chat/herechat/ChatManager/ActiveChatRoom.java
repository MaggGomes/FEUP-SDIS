package com.chat.herechat.ChatManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.Peer.Peer;
import com.chat.herechat.ServiceHandlers.ClientSocketHandler;
import com.chat.herechat.ServiceHandlers.FileHandler;
import com.chat.herechat.LocalService;
import com.chat.herechat.MainScreenActivity;
import com.chat.herechat.ServiceHandlers.SendControlMessage;



public class ActiveChatRoom {
	public ChatRoomDetails mRoomInfo;            //reference to to the details object of this room
	public boolean isHostedGroupChat;	          //indicates whether this is a hosted public chat or not
	private LocalService mService;				  //reference to the service
	public Handler mSingleSendThreadMessageReceiver=null;	//used for receiving messages from a 'SendControlMessage'
	private Handler mFileWriterResultHandler=null;			//used for receiving messages from a 'FileHandler'
	private Semaphore semaphore = new Semaphore(1, true);   //used to synch between file writers

	@SuppressLint("HandlerLeak")
	public ActiveChatRoom(LocalService srv,boolean isHosted, ChatRoomDetails info) {
		mService = srv;
		this.isHostedGroupChat = isHosted;
		mRoomInfo = info;

		Vector<ChatRoomDetails> temp = mService.getChatRooms();

		//define a handler to be triggered when the file-writer completes the job
		mFileWriterResultHandler = new Handler(mService.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				ActiveChatRoom.this.semaphore.release(); //release the semaphore when the file is read
			}
		};

	}


	public void ForwardMessage(String[] msg, boolean isSelfMsg) {
		System.out.println(213);
		if (!isSelfMsg) {
			if (!mRoomInfo.hasNewMsg) //on a state change of this flag
				mService.BroadcastRoomsUpdatedEvent(); //notify that this room has an unread message
			mRoomInfo.hasNewMsg=true; //mark that this room has received a message. Will be reset by the 'ChatAcvitiy'
		}
		String senderUnique = msg[2];  //get the sender's unique ID
		String entireMsg = Constants.StringArrayToStringWithSeperators(msg,Constants.STANDART_FIELD_SEPERATOR);  //convert back to the original string

		// we need to forward an incoming message only if this chat is hosted or this chat isn't hosted and it's a self message
		if (isHostedGroupChat || (!isHostedGroupChat && isSelfMsg)) {
			for (Peer user : mRoomInfo.Users)  //for every participating user
			{
				if (!user.uniqueID.equalsIgnoreCase(senderUnique)) //if this user isn't the one who sent this message (done to avoid loopbacks)
					{
					new SendControlMessage(null,user.IPaddr,entireMsg,mRoomInfo.RoomID).start(); //forward the msg
					}
			}
		}

		if (!isSelfMsg) {
			//No we change the incoming string to a file writeable format
			String[] temp = new String[4];
			temp[0] = msg[1];
			temp[1] = msg[2];
			temp[2] = msg[4];
			temp[3] = Constants.getTimeString();

			UpdateFileWithNewMsg( Constants.StringArrayToStringWithSeperators(temp,Constants.CHAT_MSG_ENTRY_SEPARATOR_CHAR)); //write to history file

			//launch a broadcast with the received msg:
			Intent intent = mService.CreateBroadcastIntent();
			intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.CONNECTION_CODE_NEW_CHAT_MSG); //set opcode to new msg
			intent.putExtra(Constants.SERVICE_BROADCAST_MSG_CONTENT_KEY, msg);  //set the msg as it came via the socket
			intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, mRoomInfo.RoomID); //set the room's ID
			mService.sendBroadcast(intent);
		}
	}


	public void UpdateFileWithNewMsg (String msg) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		FileHandler fh = new FileHandler(mRoomInfo.RoomID,mFileWriterResultHandler,false,mService);
		fh.UpdateDataToWriteBuffer(msg);
		fh.start();
		fh.Kill();
	}


	public String AddUser (Peer user, String suggestedPw) {
		//check if this user already exists in the room:
		if ( Constants.CheckIfUserExistsInListByUniqueID(user.uniqueID, mRoomInfo.Users)!=null)
			return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
		//check if the pw is correct:
		if (mRoomInfo.Password!=null && !suggestedPw.equalsIgnoreCase(mRoomInfo.Password))
			return Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW;
		//add
		mRoomInfo.Users.add(user);  //add this user to the mailing list
		//return positive reply
		return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
	}


	public void DeleteHistory() {
		String path  = mService.getFilesDir().getPath()+ "/" +mRoomInfo.RoomID + ".txt";
		File f = new File(path);
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (f.delete())  //delete the file. if successful, rebuild a new file template
			ChatActivity.InitHistoryFile(mRoomInfo.RoomID, mFileWriterResultHandler, mRoomInfo.Name, mRoomInfo.isPrivateChatRoom, mService);
		else //if the file deletion has failed
			semaphore.release();
	}


	@SuppressLint("HandlerLeak")
	public void updateUserNameInTheHistoryLogFile(){
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
					fileContent[0]= mRoomInfo.Users.get(0).name;
					String ans= StringArrayToStringWithSeperatedWith_lines(fileContent);

					File f = new  File(mService.getFilesDir().getPath()+ "/" + mRoomInfo.RoomID+".txt");
					//is the history was deleted we want to create new one
					if(f.delete())
						UpdateFileWithNewMsg(ans);
				}
			}// handleMessage(Message msg)
		};

		//read file
		new FileHandler(mRoomInfo.RoomID, msgHandler, true, ActiveChatRoom.this.mService).start();
	}



	private String StringArrayToStringWithSeperatedWith_lines(String [] input){
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
		ans.append(mRoomInfo.Name+Constants.STANDART_FIELD_SEPERATOR); //set the name
		ans.append(mRoomInfo.RoomID+Constants.STANDART_FIELD_SEPERATOR); //set the ID
		ans.append(mRoomInfo.Users.isEmpty()? " " : Constants.UserListToString(mRoomInfo.Users)); //set the users string
		ans.append(Constants.STANDART_FIELD_SEPERATOR);
		ans.append((mRoomInfo.Password==null? "false" : "true")+Constants.STANDART_FIELD_SEPERATOR); //set pw requirement

		return ans.toString();
	}

	public void CloseRoomAndNotifyUsers() {
		StringBuilder msg = new StringBuilder();

		msg.append(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY) + Constants.STANDART_FIELD_SEPERATOR); //set opcode
		msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
		msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
		msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set negative result
		msg.append(Constants.SERVICE_NEGATIVE_REPLY_REASON_ROOM_CLOSED + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
		msg.append(mRoomInfo.RoomID + Constants.STANDART_FIELD_SEPERATOR);  //set room's ID

		for (Peer user : mRoomInfo.Users) {
			new SendControlMessage(null,user.IPaddr,msg.toString(),mRoomInfo.RoomID).start(); //send the msg
		}
	}


	public void DisconnectFromHostingPeer() {
		StringBuilder msg = new StringBuilder();
		 //set the 'Leave room' opcode:
		msg.append(Integer.toString(Constants.CONNECTION_CODE_DISCONNECT_FROM_CHAT_ROOM) + Constants.STANDART_FIELD_SEPERATOR);
		msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
		msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
		msg.append(mRoomInfo.RoomID + Constants.STANDART_FIELD_SEPERATOR);  //set the room's ID

		new SendControlMessage(mRoomInfo.Users.get(0).IPaddr, msg.toString()).start();  //send the reply
	}
}
