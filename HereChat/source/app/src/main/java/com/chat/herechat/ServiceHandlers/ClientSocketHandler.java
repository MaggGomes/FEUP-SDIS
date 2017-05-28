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
	private Socket clientSocket;
	private LocalService service;
	private static final int SERVER_PORT = 4000;
	private boolean isPassive;
	private Peer peer; 		//the peer we're connecting to
	private int mQueryCode;  //will be used to differentiate between 3 possible operation: discover peer / start private chat / join public chat room
	
	private PrintWriter mOut=null;
	private BufferedReader mIn=null;
	
	/**
	 * Constructor for a passive socket
	 * @param service - reference to the LocalService
	 * @param socket - an open socket
	 */
	public ClientSocketHandler(LocalService service, Socket socket) {
		this.clientSocket = socket;
		this.isPassive = true;
		this.service = service;
	}
	
	/**
	 * Constructor for an active socket
	 * @param service - reference to the LocalService
	 * @param peer - a peer to connect to
	 * @param Qcode - the code of the operation to be performed. Taken from Constants.class
	 */
	public ClientSocketHandler(LocalService service, Peer peer, int Qcode) {
		this.service = service;
		this.isPassive = false;
		this.peer = peer;
		this.mQueryCode=Qcode;
	}

	@Override
	public void run() {
		if (isPassive)
			InitiatePassiveTransaction();
		else
	    	InitiateRelevantActiveTransaction();
		
		CloseInputAndOutputStreams();
	}
	
	/**
	 * Closes mIn and mOut
	 */
	private void CloseInputAndOutputStreams() {
		if (mOut!=null)
			mOut.close();
		
		if (mIn!=null){
			try {
				mIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Case handler. Switches between different incoming requests.
	 * Called by ReceiveDiscoveryMessage() if the thread is passive
	 * @param input - A String array, as returned by BreakDiscoveryMessageToStrings(String)
	 */
	private void PassiveDiscoveryQueryCaseHandler(String[] input) {
		if (input.length < 3)
			return;

		boolean SkipUserUpdate = false;
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG))) //if this is a chat message
			SkipUserUpdate = input[3].split("[_]")[0].equalsIgnoreCase(MainScreenActivity.UniqueID)? false : true;
			
		if (!SkipUserUpdate)
			service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), input[2],input[1]);
		
		String PeerIP = clientSocket.getInetAddress().getHostAddress();  // get the peer's IP
		
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_DISCOVER))) {
			//discovery logic
			SendDiscoveryMessage();  //send a discovery message back to the peer
			ArrayList<ChatRoomDetails> Rooms = ConvertDiscoveryStringToChatRoomList(input); //get the chat room data from the input
			service.UpdateChatRoomHashMap(Rooms); //update the chat rooms hash at the service.
		}
		//if this is a peer publication message
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PEER_DETAILS_BCAST))) {
			ParsePeerPublicationMessageAndUpdateDiscoveredPeers(input);
		}
		//if this is a join request for a private chat:
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST))) {


				if (service.mDiscoveredChatRoomsHash.get(input[2])!=null) //if a matching discovered room exists
					service.CreateNewPrivateChatRoom(input);
				else //a matching discovered room doesn't exist
					service.BypassDiscoveryProcedure(input,false,false);
				
				SendReplyForAJoinRequest(PeerIP,true,MainScreenActivity.UniqueID,null,true);

		}
		//if this is a join request for a public chat:	
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST))) {
			ActiveChatRoom targetRoom = service.mActiveChatRooms.get(input[3]);
			//if the target chat room doesn't exist
			if (targetRoom==null)
			{
				SendReplyForAJoinRequest(PeerIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,
						false);
			}
			else //the target is active
			{
				String res = targetRoom.AddUser(Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers),
						input[4]);
				boolean isCool = res.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
				SendReplyForAJoinRequest(PeerIP,isCool,input[3],res, false);
			}
		}
		//if this is a reply for our private chat 'Join' request
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnReceptionOfChatEstablishmentReply(input[2],isAccepted,input[4]); //call the service to handle the result
		}
		//if this is a reply for our public chat 'Join' request
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnReceptionOfChatEstablishmentReply(input[5],isAccepted,input[4]); //call the service to handle the result
		}
		//if this is a new message targeted to on of the active chat rooms
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG))) {
			//if this is a private message
			if (input[3].equalsIgnoreCase(MainScreenActivity.UniqueID)) {
				String result = CheckIfNotIgnored(input);
				//if this message came from a user which is not in the ignore list:
				if (result.equals(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST))
					service.OnNewChatMessageArrvial(input, clientSocket.getInetAddress().getHostAddress());  //let the service handle the new message
			} else { //this message came for a public chat room
				ActiveChatRoom room = service.mActiveChatRooms.get(input[3]);
				boolean isHostedByMe=input[3].split("[_]")[0].equals(MainScreenActivity.UniqueID);
				
				//if this chat room doesn't exist and the sender expects us to be it's host
				if (room==null && isHostedByMe) {
					SendReplyForAJoinRequest(PeerIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,false);
					return;
				}

				//if this room exists and is hosted by us
				if (isHostedByMe) {
				//try adding the user who sent this message to the room
				String result = room.AddUser(
						Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers), "");
					//if the user is not approved to send messages to this room
					if (!result.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST)) {
						SendReplyForAJoinRequest(PeerIP,false,input[3],result,false);
					} else {
						service.OnNewChatMessageArrvial(input, PeerIP);  //let the service handle the new message
					}
				} else {
					service.OnNewChatMessageArrvial(input, PeerIP);  //let the service handle the new message
				}
				
			}
		}
	}
	
	/**
	 * Parses a peer publication string (that comes from the group owner) and updates the 
	 * discovered peers list
	 * @param input
	 */
	private void ParsePeerPublicationMessageAndUpdateDiscoveredPeers(String[] input) {
		int index=3;   							//skip the first 3 fields of this message
		int numOfPeers = (input.length-3)/3;      //each peer comes with 3 info fields
		
		for (int j=0; j<numOfPeers ;j++)
		{
			//check if this published user is us. if so, skip to the next user.
			if (!input[index+2].equalsIgnoreCase(MainScreenActivity.UniqueID))
				service.UpdateDiscoveredUsersList(input[index], input[index+2], input[index+1]);
			index+=3;
		}
	}

	/**
	 * Sends a reply for a join request (
	 * Format: PC reply opcode$(accepted/denied)$denial reason$self unique
	 * @param peerIP
	 * @param isApproved
	 * @param RoomID
	 * @param reason
	 * @param isPrivateChat
	 */
	static public void SendReplyForAJoinRequest (String peerIP, boolean isApproved, String RoomID, String reason, boolean isPrivateChat) {
		StringBuilder msg = new StringBuilder();
		if (isPrivateChat) {
			msg.append(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY) + Constants.STANDART_FIELD_SEPERATOR); //set opcode		
			msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
			msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
			if (isApproved)
			{
				msg.append(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set positive result
				msg.append(" " + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
			}
			else //user is ignored
			{
				msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set negative result
				msg.append(reason + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
			}
			
		} else { // Public chat
			msg.append(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY) + Constants.STANDART_FIELD_SEPERATOR); //set opcode		
			msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add the self name
			msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);   //add the self unique
			if (isApproved) {
				msg.append(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set positive result
				msg.append(" " + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
			} else {
				msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);  //set negative result
				msg.append(reason + Constants.STANDART_FIELD_SEPERATOR);  //set denial reason
			}
			
			msg.append(RoomID + Constants.STANDART_FIELD_SEPERATOR);  //set room's ID
		}
		
		new SendControlMessage(peerIP,msg.toString()).start();  //send the reply
		
	}
	
	/**
	 * Checks if the user is approved to join the desired chat room
	 * @param input - incoming request string after parsing
	 * @return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST if the user is not ignored
	 */
	private String CheckIfNotIgnored(String[] input) {
		return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
	}
	/**
	 * Passive transaction
	 */
	private void InitiatePassiveTransaction() {
		if (!ReceiveDiscoveryMessage()) //if query message reception was unsuccessful. 
			return;
	}
	
	/**
	 * active discovery
	 */
	private void ActiveDiscoveryProcedure() {
		if (!SendDiscoveryMessage()) //if query message sending was unsuccessful
			return;
		
		ReceiveDiscoveryMessage();
		
		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a discovery message over the socket
	 * @return - true if successful, false otherwise
	 */
	private boolean SendDiscoveryMessage () {
		//DISCOVERY QUERY SEND LOGIC:
		if (mOut==null || mIn==null) {
			try {
				 mOut =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
				 mIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		String toSend = BuildDiscoveryString(); //get the query string to be send
		mOut.println(toSend); //send via socket
		mOut.flush();
			
		return true;
	}
	
	/**
	 * Tries to receive a discovery string from the socket.
	 * If this thread is in Active mode:
	 * 		On success: parses the string and invokes an update method at the service
	 * 		On failure (Socket crash or timeout): aborts and closes the socket
	 * If this thread is in Passive mode:
	 * 	 	On success: parses the string and calls PassiveDiscoveryQueryCaseHandler()
	 * 		On failure (Socket crash or timeout): aborts and closes the socket
	 */
	private boolean ReceiveDiscoveryMessage () {
		int numberOfReadTries=0;
		String receivedMsg=null;
		
		if (mOut==null || mIn==null) {
			try {
				 mOut =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
				 mIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
				if (mIn.ready()) {
					receivedMsg = mIn.readLine();
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
		 
		String[] parsedInput = BreakDiscoveryMessageToStrings(receivedMsg); //parse the input
		
		//if this thread is passive, parse the message and allow us to respond to it's opcode
		if (isPassive) {
			PassiveDiscoveryQueryCaseHandler(parsedInput);
		} else {
			//update the discovered user details:
			service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), parsedInput[2], parsedInput[1]);
			ArrayList<ChatRoomDetails> Rooms = ConvertDiscoveryStringToChatRoomList(parsedInput); //create a discovered chat room list
			service.UpdateChatRoomHashMap(Rooms); //update the chat rooms hash at the service.
		}
		
		return true;
	}
	
	/**
	 * Converts the discovery String that was received from a peer to a list of discovered chat rooms
	 * @param input - A String array, as returned by BreakDiscoveryMessageToStrings(String)
	 * @return An ArrayList of discovered chat rooms
	 */
	private ArrayList<ChatRoomDetails> ConvertDiscoveryStringToChatRoomList (String[] input) {
		ArrayList<ChatRoomDetails> ChatRooms = new ArrayList<ChatRoomDetails>();
		Peer host = Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers); //get the user from the service
		
		//Create an array list to hold this single user
		ArrayList<Peer> user = new ArrayList<Peer>(); //create a list with a single peer
		user.add(host);
		
		Date currentTime = Constants.GetTime();
		ChatRoomDetails PrivateChatRoom = new ChatRoomDetails(host.uniqueID, host.name, currentTime, user,true); //create a new private chat room detail 
		ChatRooms.add(PrivateChatRoom);  //add to the chat room list
		
		int index=3;  
		int numOfPublishedRooms = (input.length-3)/4;  //calc the number of advertised public rooms
		ChatRoomDetails PublicChatRoom = null;
		
		for (int k = 0; k < numOfPublishedRooms; k++) {
			user = new ArrayList<Peer>(); //create a list with a single peer
			user.add(host);
			PublicChatRoom = new ChatRoomDetails(
					input[index+1],input[index],currentTime,user,input[index+3],
					input[index+2].equalsIgnoreCase(" ")? host.name : host.name+", "+input[index+2]); //create a new public chat room detail
			ChatRooms.add(PublicChatRoom);  //add to the chat room list
			index+=4; //move to the next room
		}
		
		return ChatRooms;
	}
	
	/**
	 * Switches between Active operations (Discover / Start private chat / Join a chat room).
	 */
	private void InitiateRelevantActiveTransaction() {
		try {
		    /**
		     * Create a client socket with the host,
		     * port, and timeout information.
		     */
			clientSocket = new Socket();
			clientSocket.bind(null);
		    clientSocket.connect((new InetSocketAddress(peer.IPaddr, SERVER_PORT)), 10000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if(clientSocket==null)
			return;
		
		switch (mQueryCode) {
		case Constants.CONNECTION_CODE_DISCOVER:
			{
				ActiveDiscoveryProcedure();
				break;
			}
		}
	}
	
	/**
	 * Splits a string with our special character used as a delimiter
	 * @param input - a discovery String that was received from a peer
	 * @return String array that was parsed by our special character
	 */
	private String[] BreakDiscoveryMessageToStrings(String input) {
		return input.split("["+Constants.STANDART_FIELD_SEPERATOR+"]"); //parse the string by the separator char
	}

	/**
	 * Creates a discovery string, containing data about all hosted rooms on this device, to be sent via socket
	 * @return String
	 */
	private String BuildDiscoveryString() {
		//Build the 1st mandatory part of the discovery string: information about this user that'll enable a private chat
		StringBuilder res = new StringBuilder(Integer.toString(Constants.CONNECTION_CODE_DISCOVER) + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
		//Now we'll add info about all hosted chat rooms:
		
		Collection<ActiveChatRoom> ActiveChatRooms = service.mActiveChatRooms.values();  //get all available hosted chat rooms
		for (ActiveChatRoom room : ActiveChatRooms) {
			if (room.isHostedGroupChat) {
				res.append(room.toString());
			}
		}
		return res.toString();
	}
}
