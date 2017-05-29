package com.chat.herechat.ServiceHandlers;

import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.LocalService;
import com.chat.herechat.MainScreenActivity;
import com.chat.herechat.Peer.Peer;
import com.chat.herechat.ChatManager.ActiveChatRoom;
import com.chat.herechat.ChatManager.ChatRoomDetails;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class ClientSocketHandler extends Thread {
	private BufferedReader reader =null;
	private PrintWriter writer =null;




	private Socket clientSocket;
	private LocalService service;
	private static final int SERVER_PORT = 4000;
	private boolean inactive;
	private Peer peer;
	private int codeOp;
	

	

	public ClientSocketHandler(LocalService service, Socket socket) {
		this.clientSocket = socket;
		this.inactive = true;
		this.service = service;
	}
	

	public ClientSocketHandler(LocalService service, Peer peer, int opCode) {
		this.service = service;
		this.inactive = false;
		this.peer = peer;
		this.codeOp = opCode;
	}

	@Override
	public void run() {
		if (inactive)
			InitiatePassiveTransaction();
		else
	    	openCommunications();
		
		CloseInputAndOutputStreams();
	}
	

	private void CloseInputAndOutputStreams() {
		if (writer !=null)
			writer.close();
		
		if (reader !=null){
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	private void PassiveDiscoveryQueryCaseHandler(String[] input) {
		if (input.length < 3)
			return;

		boolean SkipUserUpdate = false;
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG)))
			SkipUserUpdate = !input[3].split("[_]")[0].equalsIgnoreCase(MainScreenActivity.UniqueID);
			
		if (!SkipUserUpdate)
			service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), input[2],input[1]);
		
		String currIP = clientSocket.getInetAddress().getHostAddress();
		
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_DISCOVER))) {

			SendDiscoveryMessage();
			ArrayList<ChatRoomDetails> Rooms = convertStringtoChatList(input);
			service.UpdateChatroom(Rooms);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PEER_DETAILS_BCAST))) {
			ParsePeerAndDiscoveryMessage(input);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST))) {


				if (service.hashChatroom.get(input[2])!=null)
					service.CreateNewPrivateChatRoom(input);
				else // room doesn't exist
					service.SkipDiscovery(input,false,false);
				
				replyToRequest(currIP,true,MainScreenActivity.UniqueID,null,true);

		}
		//Joirequest for public
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST))) {
			ActiveChatRoom targetRoom = service.hashChatroomActive.get(input[3]);

			if (targetRoom==null)
			{
				replyToRequest(currIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,
						false);
			}
			else
			{
				String res = targetRoom.AddUser(Constants.CheckUniqueID(input[2], service.userDiscovered),
						input[4]);
				boolean isCool = res.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
				replyToRequest(currIP,isCool,input[3],res, false);
			}
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnRecieveReply(input[2], input[4], isAccepted);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnRecieveReply(input[5], input[4], isAccepted);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG))) {

			if (input[3].equalsIgnoreCase(MainScreenActivity.UniqueID)) {
				String result = CheckIfNotIgnored(input);

				if (result.equals(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST))
					service.OnMessageRecieved(input, clientSocket.getInetAddress().getHostAddress());
			} else {
				ActiveChatRoom room = service.hashChatroomActive.get(input[3]);
				boolean isHost =input[3].split("[_]")[0].equals(MainScreenActivity.UniqueID);
				

				if (room==null && isHost) {
					replyToRequest(currIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,false);
					return;
				}


				if (isHost) {
				String res = room.AddUser(
						Constants.CheckUniqueID(input[2], service.userDiscovered), "");

					if (!res.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST)) {
						replyToRequest(currIP,false,input[3], res,false);
					} else {
						service.OnMessageRecieved(input, currIP);
					}
				} else {
					service.OnMessageRecieved(input, currIP);
				}
				
			}
		}
	}
	

	private void ParsePeerAndDiscoveryMessage(String[] input) {
		int index=3;
		int avaliablePeers = (input.length-3)/3;
		
		for (int j = 0; j< avaliablePeers; j++)
		{

			if (!input[index+2].equalsIgnoreCase(MainScreenActivity.UniqueID))
				service.UpdateDiscoveredUsersList(input[index], input[index+2], input[index+1]);
			index+=3;
		}
	}


	static public void replyToRequest(String peerIP, boolean isApproved, String RoomID, String reason, boolean isPrivateChat) {
		StringBuilder msg = new StringBuilder();
		if (isPrivateChat) {
			msg.append(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY) + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
			if (isApproved)
			{
				msg.append(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(" " + Constants.STANDART_FIELD_SEPERATOR);
			}
			else
			{
				msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(reason + Constants.STANDART_FIELD_SEPERATOR);
			}
			
		} else { // Public version of chat
			msg.append(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY) + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
			if (isApproved) {
				msg.append(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(" " + Constants.STANDART_FIELD_SEPERATOR);
			} else {
				msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(reason + Constants.STANDART_FIELD_SEPERATOR);
			}
			
			msg.append(RoomID + Constants.STANDART_FIELD_SEPERATOR);
		}
		
		new SendControlMessage(peerIP,msg.toString()).start();
		
	}
	

	private String CheckIfNotIgnored(String[] input) {
		return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
	}

	private void InitiatePassiveTransaction() {
		if (!ReceiveDiscoveryMessage()) //if query message not sent
			return;
	}
	

	private void DiscoveryProcess() {
		if (!SendDiscoveryMessage()) //if  message sent
			return;
		
		ReceiveDiscoveryMessage();
		
		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private boolean SendDiscoveryMessage () {
		if (writer ==null || reader ==null) {
			try {
				 writer =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
				 reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		String toSend = BuildMessage(); //get the query string to be send
		writer.println(toSend); //send via socket
		writer.flush();
			
		return true;
	}
	

	private boolean ReceiveDiscoveryMessage () {
		int numberOfReadTries=0;
		String receivedMsg=null;
		
		if (writer ==null || reader ==null) {
			try {
				 writer =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
				 reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		while (numberOfReadTries<=Constants.NUM_OF_QUERY_RECEIVE_RETRIES) {
			try {
				if (reader.ready()) {
					receivedMsg = reader.readLine();
					numberOfReadTries=Constants.NUM_OF_QUERY_RECEIVE_RETRIES+1;
				}
				Thread.sleep(100);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			
			numberOfReadTries++;
		}

		if (receivedMsg==null)
			return false;
		 
		String[] parsedInput = SeparateMessage(receivedMsg);

		if (inactive) {
			PassiveDiscoveryQueryCaseHandler(parsedInput);
		} else {
			//update the discovered user info;
			service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), parsedInput[2], parsedInput[1]);
			ArrayList<ChatRoomDetails> Rooms = convertStringtoChatList(parsedInput);

			service.UpdateChatroom(Rooms);
		}
		
		return true;
	}
	

	private ArrayList<ChatRoomDetails> convertStringtoChatList(String[] input) {
		ArrayList<ChatRoomDetails> ChatRooms = new ArrayList<ChatRoomDetails>();
		Peer host = Constants.CheckUniqueID(input[2], service.userDiscovered);
		

		ArrayList<Peer> user = new ArrayList<Peer>();
		user.add(host);
		
		Date currentTime = Constants.GetTime();
		ChatRoomDetails PrivateChatRoom = new ChatRoomDetails(host.uniqueID, host.name, currentTime, user,true);
		ChatRooms.add(PrivateChatRoom);
		
		int index=3;  
		int numOfPublishedRooms = (input.length-3)/4;
		ChatRoomDetails PublicChatRoom = null;
		
		for (int k = 0; k < numOfPublishedRooms; k++) {
			user = new ArrayList<Peer>();
			user.add(host);
			PublicChatRoom = new ChatRoomDetails(
					input[index+1],input[index],currentTime,user,input[index+3],
					input[index+2].equalsIgnoreCase(" ")? host.name : host.name+", "+input[index+2]);
			ChatRooms.add(PublicChatRoom);
			index+=4; //next room
		}
		
		return ChatRooms;
	}
	

	private void openCommunications() {
		try {

			clientSocket = new Socket();
			clientSocket.bind(null);
		    clientSocket.connect((new InetSocketAddress(peer.IPaddress, SERVER_PORT)), 10000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if(clientSocket==null)
			return;
		
		switch (codeOp) {
		case Constants.CONNECTION_CODE_DISCOVER:
			{
				DiscoveryProcess();
				break;
			}
		}
	}
	

	private String[] SeparateMessage(String input) {
		return input.split("["+Constants.STANDART_FIELD_SEPERATOR+"]"); //parse the string by the separator char
	}


	private String BuildMessage() {

		StringBuilder res = new StringBuilder(Integer.toString(Constants.CONNECTION_CODE_DISCOVER) + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);

		Collection<ActiveChatRoom> ActiveChatRooms = service.hashChatroomActive.values();
		for (ActiveChatRoom room : ActiveChatRooms) {
			if (room.isPublicHosted) {
				res.append(room.toString());
			}
		}
		return res.toString();
	}
}
