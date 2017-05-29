package com.chat.herechat.ChatManager;

import com.chat.herechat.Peer.Peer;

import java.util.ArrayList;
import java.util.Date;

public class ChatRoomDetails
{

	public String roomID =null;
	public String name =null;
	public String password =null;  //a suggested password by a user trying to connect to this room
	public Date lastSeen =null;
	public boolean isPrivateChatRoom;
	public ArrayList<Peer> users =null;
	public String userNamesString = null;  //will be used to hold the users string for not-hosted chat rooms
	public boolean hasNewMsg=false;

	public ChatRoomDetails(String Name, String RoomID, String Password, ArrayList<Peer> mUsers, Date LastSeen, boolean isPrivate)
	{

		this.password =Password;
		this.roomID =RoomID;
		this.name =Name;
		this.lastSeen =LastSeen;
		this.users =mUsers;
		this.isPrivateChatRoom=isPrivate;
	}

	public ChatRoomDetails(String RoomID, String Name, Date LastSeen, ArrayList<Peer> mUsers, boolean isPrivate)
	{
		this(Name, RoomID, null, mUsers, LastSeen, isPrivate);
	}

	public ChatRoomDetails(String RoomID, String Name, Date LastSeen, ArrayList<Peer> mUsers, String isPwRequired, String usersString)
	{

		this.password = isPwRequired.equals("true")? "yup" : null;
		this.roomID =RoomID;
		this.name =Name;
		this.lastSeen =LastSeen;
		this.users =mUsers;
		this.isPrivateChatRoom=false;
		this.userNamesString =usersString;
	}

}//end of class
