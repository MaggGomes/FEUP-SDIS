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
	private static final int SERVER_PORT = 4000;
	private boolean passiveSocket;
	private Peer peer; 		// the peer we're connecting to
	private int mQueryCode;  // discover peer / start private chat / join public chat room
	
	private PrintWriter mOut=null;
	private BufferedReader mIn=null;
    private Socket clientSocket;
    private LocalService service;
	
	/**
	 * Constructor for a passive socket
	 * @param service - reference to the LocalService
	 * @param socket - an open socket
	 */
	public ClientSocketHandler(LocalService service, Socket socket) {
        this.service = service;
		this.clientSocket = socket;
		this.passiveSocket = true;
	}
	
	/**
	 * Constructor for an active socket
	 * @param service - reference to the LocalService
	 * @param peer - a peer to connect to
	 * @param Qcode - the code of the operation to be performed
	 */
	public ClientSocketHandler(LocalService service, Peer peer, int Qcode) {
        this.peer = peer;
        this.mQueryCode=Qcode;
		this.service = service;
		this.passiveSocket = false;
	}

	@Override
	public void run() {
		if (!passiveSocket)
            try {
                clientSocket = new Socket();
                clientSocket.bind(null);
                clientSocket.connect((new InetSocketAddress(peer.IPaddr, SERVER_PORT)), 3000);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        if(clientSocket!=null){
            switch (mQueryCode) {
                case Constants.CONNECTION_CODE_DISCOVER: {
                    ActiveDiscoveryProcedure();
                    break;
                }
            }
        } else {
            if (!ReceiveDiscoveryMessage()) //if query message reception was unsuccessful.
                return;
        }

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
	 * @param input
	 */
	private void PassiveDiscoveryQueryCaseHandler(String[] input) {
		if (input.length < 3)
			return;

		boolean SkipUserUpdate = false;
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG)))
			SkipUserUpdate = input[3].split("[_]")[0].equalsIgnoreCase(MainScreenActivity.UniqueID)? false : true;
			
		if (!SkipUserUpdate)
			service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), input[2],input[1]);
		
		String PeerIP = clientSocket.getInetAddress().getHostAddress();
		
		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_DISCOVER))) {
			SendDiscoveryMessage();
			ArrayList<ChatRoomDetails> Rooms = ConvertDiscoveryStringToChatRoomList(input);
			service.UpdateChatRoomHashMap(Rooms);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PEER_DETAILS_BCAST))) {
			ParsePeerPublicationMessageAndUpdateDiscoveredPeers(input);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST))) {
			String result = CheckIfNotIgnored(input);
			
			if (result.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST))
			{
				if (service.mDiscoveredChatRoomsHash.get(input[2]) != null)
					service.CreateNewPrivateChatRoom(input);
				else
					service.BypassDiscoveryProcedure(input,false,false);
				
				SendReplyForAJoinRequest(PeerIP,true,MainScreenActivity.UniqueID,null,true);
			}
			else {
				SendReplyForAJoinRequest(PeerIP,false,MainScreenActivity.UniqueID,result,true);
			}	
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST))) {
			ActiveChatRoom targetRoom = service.mActiveChatRooms.get(input[3]);

			if (targetRoom==null) {
				SendReplyForAJoinRequest(PeerIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,
						false);
			} else {
				String res = targetRoom.AddUser(Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers),
						input[4]);
				boolean isCool = res.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
				SendReplyForAJoinRequest(PeerIP,isCool,input[3],res, false);
			}
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnReceptionOfChatEstablishmentReply(input[2],isAccepted,input[4]); //call the service to handle the result
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REPLY))) {
			boolean isAccepted = input[3].equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST);
			service.OnReceptionOfChatEstablishmentReply(input[5],isAccepted,input[4]); //call the service to handle the result
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_DISCONNECT_FROM_CHAT_ROOM))) {
			service.OnRequestToRemoveFromHostedChat(input[2], input[3]);
		}

		if (input[0].equalsIgnoreCase(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG))) {
			if (input[3].equalsIgnoreCase(MainScreenActivity.UniqueID)) {
				String result = CheckIfNotIgnored(input);

				if (result.equals(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST))
					service.OnNewChatMessageArrvial(input, clientSocket.getInetAddress().getHostAddress());  //let the service handle the new message

				if (result.equals(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_BANNED))
					SendReplyForAJoinRequest(PeerIP,false,MainScreenActivity.UniqueID,result,true);
			} else {
				ActiveChatRoom room = service.mActiveChatRooms.get(input[3]);
				boolean isHostedByMe=input[3].split("[_]")[0].equals(MainScreenActivity.UniqueID);

				if (room==null && isHostedByMe) {
					SendReplyForAJoinRequest(PeerIP,false,input[3],Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM,false);
					return;
				}

				if (isHostedByMe) {

				String result = room.AddUser(
						Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers), "");

					if (!result.equalsIgnoreCase(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST)) {
						SendReplyForAJoinRequest(PeerIP,false,input[3],result,false);
					} else {
						service.OnNewChatMessageArrvial(input, PeerIP);
					}
				} else {
					service.OnNewChatMessageArrvial(input, PeerIP);
				}
			}
		}
	}

	private void ParsePeerPublicationMessageAndUpdateDiscoveredPeers(String[] input) {
		int index = 3;
		int numOfPeers = (input.length-3)/3;
		
		for (int j=0; j<numOfPeers ;j++) {
			if (!input[index+2].equalsIgnoreCase(MainScreenActivity.UniqueID))
				service.UpdateDiscoveredUsersList(input[index], input[index+2], input[index+1]);
			index+=3;
		}
	}

	static public void SendReplyForAJoinRequest (String peerIP, boolean isApproved, String RoomID, String reason, boolean isPrivateChat) {
		StringBuilder msg = new StringBuilder();
		if (isPrivateChat) {
			msg.append(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REPLY) + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);
			msg.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
			if (isApproved) {
				msg.append(Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(" " + Constants.STANDART_FIELD_SEPERATOR);
			}
			else {
				msg.append(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST + Constants.STANDART_FIELD_SEPERATOR);
				msg.append(reason + Constants.STANDART_FIELD_SEPERATOR);
			}
			
		} else {
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
        if (!service.mBannedFromPrivateChatUsers.containsKey(input[2]))
            return Constants.SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST;
        else
            return Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_BANNED;
    }
	
	/**
	 * active discovery
	 */
	private void ActiveDiscoveryProcedure() {
		if (!SendDiscoveryMessage())
			return;
		
		ReceiveDiscoveryMessage();
		
		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
		
		String toSend = BuildDiscoveryString();
		mOut.println(toSend);
		mOut.flush();
			
		return true;
	}

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
		 
		String[] parsedInput = receivedMsg.split("["+Constants.STANDART_FIELD_SEPERATOR+"]");

		if (!passiveSocket) {
            service.UpdateDiscoveredUsersList(clientSocket.getInetAddress().getHostAddress(), parsedInput[2], parsedInput[1]);
            ArrayList<ChatRoomDetails> Rooms = ConvertDiscoveryStringToChatRoomList(parsedInput);
            service.UpdateChatRoomHashMap(Rooms);
		} else {
            PassiveDiscoveryQueryCaseHandler(parsedInput);
		}
		
		return true;
	}

	private ArrayList<ChatRoomDetails> ConvertDiscoveryStringToChatRoomList (String[] input) {
		ArrayList<ChatRoomDetails> ChatRooms = new ArrayList<ChatRoomDetails>();
		Peer host = Constants.CheckIfUserExistsInListByUniqueID(input[2], service.mDiscoveredUsers);

		ArrayList<Peer> user = new ArrayList<Peer>();
		user.add(host);
		
		Date currentTime = Constants.GetTime();
		ChatRoomDetails PrivateChatRoom = new ChatRoomDetails(host.uniqueID, host.name, currentTime, user,true);
		ChatRooms.add(PrivateChatRoom);
		
		int index = 3;
		int numOfPublishedRooms = (input.length-3)/4;
		ChatRoomDetails PublicChatRoom = null;
		
		for (int k = 0; k < numOfPublishedRooms; k++) {
			user = new ArrayList<Peer>();
			user.add(host);
			PublicChatRoom = new ChatRoomDetails(
					input[index+1],input[index],currentTime,user,input[index+3],
					input[index+2].equalsIgnoreCase(" ")? host.name : host.name+", "+input[index+2]);
			ChatRooms.add(PublicChatRoom);
			index+=4;
		}
		
		return ChatRooms;
	}

	private String BuildDiscoveryString() {
		StringBuilder res = new StringBuilder(Integer.toString(Constants.CONNECTION_CODE_DISCOVER) + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR
				     + MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
		
		Collection<ActiveChatRoom> ActiveChatRooms = service.mActiveChatRooms.values();
		for (ActiveChatRoom room : ActiveChatRooms) {
			if (room.isHostedGroupChat) {
				res.append(room.toString());
			}
		}
		return res.toString();
	}
}
