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

/**
 * This class holds all the functionality of an active chat room, both private and group chat, hosted and not hosted.
 * This class is used by our {@link LocalService} to maintain a single chat room which the user participates in.
 *
 */

public class ActiveChatRoom {
	public ChatRoomDetails mRoomInfo;            //reference to to the details object of this room
	public boolean isHostedGroupChat;	          //indicates whether this is a hosted public chat or not
	private LocalService mService;				  //reference to the service
	private ArrayList<Peer> mBannedUsers=null;	  //a list of users that are banned from joining this room
	public Handler mSingleSendThreadMessageReceiver=null;	//used for receiving messages from a 'SendControlMessage'
	private Handler mFileWriterResultHandler=null;			//used for receiving messages from a 'FileHandler'
	private Semaphore semaphore = new Semaphore(1, true);   //used to synch between file writers

	@SuppressLint("HandlerLeak")
	public ActiveChatRoom(LocalService srv,boolean isHosted, ChatRoomDetails info) {
		mService = srv;
		this.isHostedGroupChat = isHosted;
		mRoomInfo = info;
		mBannedUsers = new ArrayList<Peer>();

		Vector<ChatRoomDetails> temp = mService.getChatRooms();

		//define a handler to be triggered when the file-writer completes the job
		mFileWriterResultHandler = new Handler(mService.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				ActiveChatRoom.this.semaphore.release(); //release the semaphore when the file is read
			}
		};

	}

	/**
	 * Forwards a newly received message to all relevant users
	 * @param msg - the string, as came via the socket
	 * @param isSelfMsg - indicating if this message came from us or a peer
	 */
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

	/**
	 * Will be called by the service when a new message for this room arrives.
	 * This method is used only for messaging coming from peers. Self generated messages are handled
	 * by the 'ChatActivity'
	 * @param msg - the string to write to the file
	 */
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

	/**
	 * Used only by a hosted group chat. Adds a user to the chat group if he's not banned and the password matches
	 * @param user - reference to the user to be added
	 * @return result - Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST if the user already exists or was added successfully,
	 * Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_BANNED if the user is banned
	 * and Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW if the offered password is wrong
	 */
	public String AddUser (Peer user, String suggestedPw) {
		//check if this user already exists in the room:
		if ( Constants.CheckIfUserExistsInListByUniqueID(user.uniqueID, mRoomInfo.Users)!=null)
			return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
		//check if not banned:
		if (CheckIfUserIsBanned(user.uniqueID))
			return Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_BANNED;
		//check if the pw is correct:
		if (mRoomInfo.Password!=null && !suggestedPw.equalsIgnoreCase(mRoomInfo.Password))
			return Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW;
		//add
		mRoomInfo.Users.add(user);  //add this user to the mailing list
		//return positive reply
		return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
	}

	/**
	 * Deletes and re-initiates the history file associated with this chat.
	 */
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

	/**
	 * Removes a user from the chat room
	 * @param userUniqueId - the unique id of the user to be removed
	 */
	public void RemoveUserFromTheUsersList(String userUniqueId) {
		HandleKickOrBanRequest(userUniqueId, false, false);
	}


	/**
	 * Handles kick or ban requests made by a host of a public room
	 * @param userUniqueId - the user's unique id
	 * @param isBanned - indicating whether this user is also banned
	 * @param isToSendMsgToPeer - indicates whether a reply message should be sent to the peer
	 */
	public void HandleKickOrBanRequest (String userUniqueId, boolean isBanned, boolean isToSendMsgToPeer) {
		Peer userToKick=null;

		for (Peer user : mRoomInfo.Users) {
			if (user.uniqueID.equalsIgnoreCase(userUniqueId)) {
				userToKick=user;
				break;
			}
		}

		if (userToKick!=null) {
			mRoomInfo.Users.remove(userToKick);

			if (isToSendMsgToPeer)
				SendKickOrBanMsgToUser(userToKick.IPaddr, isBanned);
		}
	}

	/**
	 * Sends a kick or ban message to a peer that participates in this room
	 * @param IPaddr - peer's IP address
	 * @param isBanned - true indicates that the user is banned, false indicates that he's kicked.
	 */
	private void SendKickOrBanMsgToUser(String IPaddr, boolean isBanned) {
		if (isBanned) {
			ClientSocketHandler.SendReplyForAJoinRequest(
					IPaddr, false, mRoomInfo.RoomID, Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_BANNED, false);
		} else {
			ClientSocketHandler.SendReplyForAJoinRequest(
					IPaddr, false, mRoomInfo.RoomID, Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_KICKED, false);
		}
	}

	/**
	 * Bans and kicks a user from this chat room
	 * @param userUniqueId - the user's unique id
	 */
	public void BanUser (String userUniqueId) {
		if (!CheckIfUserIsBanned(userUniqueId)) //if this user isn't banned already
		{
			Peer toBan =  Constants.CheckIfUserExistsInListByUniqueID(userUniqueId, mService.mDiscoveredUsers); //find the user to ban
			mBannedUsers.add(toBan);  //add to the shitlist
			HandleKickOrBanRequest(userUniqueId, true, true);
		}
	}

	/**
	 * Kicks a user out of the room and send an appropriate message
	 * @param userUniqueId - the user's unique id
	 */
	public void KickUser (String userUniqueId) {
		HandleKickOrBanRequest(userUniqueId, false, true);
	}

	/**
	 * Checks if a user is banned. Used when a user tries to join this room or send a message to it.
	 * @param userUniqueId - the peer's unique ID
	 * @return true if he's banned, false otherwise.
	 */
	public boolean CheckIfUserIsBanned (String userUniqueId) {
		if ( Constants.CheckIfUserExistsInListByUniqueID(userUniqueId, mBannedUsers)!=null)
			return true;

		return false;
	}

	/**
	 * this function is called when we discovered that the room's name was changed and should be updated.
	 * IT IS ONLY RELEVANT TO A PRIVATE CHAT ROOM!
	 * it uses a handler to read the history file. when the handler's    "handleMessage(Message msg)" function is called ,it calls
	 * the UpdateFileWithNewMsg(msg) with an up to date log file based on the old one
	 */
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


	/**
	 *
	 * @param input- an array of strings
	 * @return a new string, built from the input[] members, separated by "\r\n" so it can be written to a history log file
	 */
	private String StringArrayToStringWithSeperatedWith_lines(String [] input){
		StringBuilder buffer = new StringBuilder();
		int length = input.length;
		for (int i=0; i<length ;i++) {
			buffer.append(input[i]);
			buffer.append("\r\n");
		}
		return new String(buffer.toString());
	}

	/**
	 * Used to convert the chat room's info to a string that can be sent to other users
	 * String structure: RoomName$RoomID$UserList$RequiresPw(True/False)
	 */
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

	/**
	 * Clears the banned users list
	 */
	public void ClearBannedUsersList()
	{
		mBannedUsers.clear();
	}//end of ClearBannedUsersList()

	/**
	 * Valid only for a not-hosted public chat room
	 * Closes a connection to a not-hosted public chat room
	 */
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
