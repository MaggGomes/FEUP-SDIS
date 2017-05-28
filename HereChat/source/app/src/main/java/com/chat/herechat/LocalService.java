package com.chat.herechat;


import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.TaskStackBuilder;

import com.chat.herechat.ChatManager.ActiveChatRoom;
import com.chat.herechat.ChatManager.ChatActivity;
import com.chat.herechat.ChatManager.ChatMessage;
import com.chat.herechat.ChatManager.ChatRoomDetails;
import com.chat.herechat.ChatManager.ChatSearchScreenFrag;
import com.chat.herechat.Peer.Peer;
import com.chat.herechat.Receiver.WiFiDirectBroadcastReceiver;
import com.chat.herechat.ServiceHandlers.ClientSocketHandler;
import com.chat.herechat.ServiceHandlers.SendControlMessage;
import com.chat.herechat.ServiceHandlers.ServerSocketHandler;
import com.chat.herechat.Utilities.Constants;

@SuppressLint("HandlerLeak")
public class LocalService extends Service {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    private ServerSocketHandler serverSocketHandler = null;
    private Handler mSocketSendResultHandler = null;
    public Handler mRefreshHandler = null;
    public WiFiDirectBroadcastReceiver mWifiP2PReceiver = null;
    public IntentFilter mIntentFilter;
    public WifiP2pDevice[] devices;
    public HashMap<String, ActiveChatRoom> mActiveChatRooms = null;
    public HashMap<String, ChatRoomDetails> mDiscoveredChatRoomsHash = null;
    public ArrayList<Peer> mDiscoveredUsers = null;
    public boolean mIsWifiGroupOwner = false;
    public ArrayList<ChatMessage> mListContent;


    public static NotificationManager mNotificationManager = null;
    public boolean isChatActivityActive = false;
    public ChatRoomDetails DisplayedAtChatActivity = null;

    private boolean mIsWifiPeerValid = false;
    public final int Handler_WHAT_valueForActivePeerTO = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        InitClassMembers();

        if (mSocketSendResultHandler == null)
            mSocketSendResultHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    String result = data.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT);
                    String RoomID = data.getString(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID);

                    HandleSendAttemptAndBroadcastResult(result, RoomID);
                }
            };

        if (mRefreshHandler == null) {
            mRefreshHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //if this is a wifi peer validation TO
                    if (msg.what == Handler_WHAT_valueForActivePeerTO) {
                        if (!mIsWifiPeerValid) {
                            ChatSearchScreenFrag.mIsConnectedToGroup = false;
                            ChatSearchScreenFrag.mManager.removeGroup(ChatSearchScreenFrag.mChannel, null);
                        }
                    } else {
                        if (ChatSearchScreenFrag.mIsWifiDirectEnabled)
                            LocalService.this.OnRefreshButtonclicked();
                        DeleteTimedOutRooms();

                        sendEmptyMessageDelayed(0, MainScreenActivity.refreshPeriod);
                    }
                }
            };

            mRefreshHandler.sendEmptyMessageDelayed(0, 500);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int superReturnedVal = super.onStartCommand(intent, flags, startId);

        if (serverSocketHandler == null) {
            serverSocketHandler = new ServerSocketHandler(this);
            serverSocketHandler.start();
        }

        return superReturnedVal;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mWifiP2PReceiver == null) {
            UpdateIntentFilter();

            mWifiP2PReceiver = new WiFiDirectBroadcastReceiver(ChatSearchScreenFrag.mManager, ChatSearchScreenFrag.mChannel, this);
            getApplication().registerReceiver(mWifiP2PReceiver, mIntentFilter);
        }
        return binder;
    }

    /**
     * Creates and broadcasts an intent after occurrence of a wifi event
     *
     * @param wifiEventCode  - taken from Constants.class
     * @param failReasonCode - as given by the system
     */
    public void CreateAndBroadcastWifiP2pEvent(int wifiEventCode, int failReasonCode) {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ACTION_WIFI_EVENT_VALUE);
        intent.putExtra(Constants.SERVICE_BROADCAST_WIFI_EVENT_KEY, wifiEventCode);
        intent.putExtra(Constants.SERVICE_BROADCAST_WIFI_EVENT_FAIL_REASON_KEY, failReasonCode);
        sendBroadcast(intent);
    }

    public void EstablishChatConnection(String roomUniqueID, String password, boolean isPrivateChat) {
       /* Verifies if there is already an active room */
        if (mActiveChatRooms.get(roomUniqueID) == null) {
            String msg;

            if (isPrivateChat)
                msg = ConstructJoinMessage(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST, null, null);

            else
                msg = ConstructJoinMessage(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST, roomUniqueID, password);

            String peerIP = mDiscoveredChatRoomsHash.get(roomUniqueID).Users.get(0).IPaddr; //get the target's IP
            new SendControlMessage(mSocketSendResultHandler, peerIP, msg, roomUniqueID).start(); //start the thread
        } else {
            Intent intent = CreateBroadcastIntent();
            intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT); //the opcode is connection result event
            //mark that an active room already exists:
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT, Constants.SINGLE_SEND_THREAD_ACTION_RESULT_ALREADY_CONNECTED);
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, roomUniqueID); //set the room's ID
            sendBroadcast(intent);
        }
    }

    public void OnServerSocketError() {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_WELCOME_SOCKET_CREATE_FAIL);
    }

    /**
     * Called by a worker thread when a new chat message arrives.
     * Arriving messages are checked for source and destination.
     *
     * @param msg    - the message as was received via socket
     * @param peerIP - the sender's IP address
     */
    public void OnNewChatMessageArrvial(String[] msg, String peerIP) {
        boolean isPrivateMsg = false;
        ActiveChatRoom targetRoom = null;
        if (msg[3].equalsIgnoreCase(MainScreenActivity.UniqueID)) {
            msg[3] = msg[2];
            isPrivateMsg = true;
        }

        targetRoom = mActiveChatRooms.get(msg[3]);

        if (targetRoom != null) {
            targetRoom.ForwardMessage(msg, false);

            if (!targetRoom.isHostedGroupChat)
                UpdateTimeChatRoomLastSeenTimeStamp(msg[2], msg[3]);
        } else {
            BypassDiscoveryProcedure(msg, true, isPrivateMsg);
        }

        if (MainScreenActivity.isToNotifyOnNewMsg &&
                (DisplayedAtChatActivity == null || !msg[3].equalsIgnoreCase(DisplayedAtChatActivity.RoomID))) {

            Intent resultIntent = new Intent(this, MainScreenActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainScreenActivity.class);

            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            String notificationString = CreateNewMsgNotificationString();
            if (notificationString != null)
                Constants.ShowNotification(notificationString, resultPendingIntent); //show notification
        }

        BroadcastRoomsUpdatedEvent();
    }

    private void UpdateTimeChatRoomLastSeenTimeStamp(String peerUnique, String roomUnique) {
        Date currentTime = Constants.GetTime();
        ChatRoomDetails ChatDetails = mDiscoveredChatRoomsHash.get(peerUnique);

        if (ChatDetails != null)
            ChatDetails.LastSeen = currentTime;

        if (!peerUnique.equalsIgnoreCase(roomUnique)) {
            ChatDetails = mDiscoveredChatRoomsHash.get(roomUnique);
            if (ChatDetails != null)
                ChatDetails.LastSeen = currentTime;
        }
    }

    /**
     * Called when an undiscovered peer sends us a private message, meaning that a proper discovery procedure never happened.
     *
     * @param msg       - the msg as it was received via socket
     * @param isChatMsg - indicating what kind of message it is. Since we haven't discovered the sending peer properly,
     *                  this can be a 'chat request message' or a 'chat message'
     */
    public void BypassDiscoveryProcedure(String[] msg, boolean isChatMsg, boolean isPrivateChat) {
        if (isPrivateChat) {
            ArrayList<ChatRoomDetails> ChatRooms = new ArrayList<ChatRoomDetails>();
            Peer host = Constants.CheckIfUserExistsInListByUniqueID(msg[2], mDiscoveredUsers);

            ArrayList<Peer> user = new ArrayList<Peer>();
            user.add(host);

            ChatRoomDetails PrivateChatRoom = new ChatRoomDetails(host.uniqueID, host.name, Constants.GetTime(), user, true);
            ChatRooms.add(PrivateChatRoom);
            UpdateChatRoomHashMap(ChatRooms);
            CreateNewPrivateChatRoom(msg);
            ActiveChatRoom targetRoom = mActiveChatRooms.get(msg[2]);

            if (isChatMsg)
                targetRoom.ForwardMessage(msg, false);
        } else {
            ChatRoomDetails details = mDiscoveredChatRoomsHash.get(msg[3]);
            if (details == null) {
                OnRefreshButtonclicked();
            } else {
                ActiveChatRoom activeRoom = new ActiveChatRoom(this, false, details);
                InitHistoryFileIfNecessary(details.RoomID, details.Name, false);
                mActiveChatRooms.put(details.RoomID, activeRoom);
                BroadcastRoomsUpdatedEvent();
            }
        }
    }

    /**
     * Sends a message that was created by this user. Bypasses the 'ActiveChatRoom's sending method.
     *
     * @param msg            - the text msg as it was typed by the user
     * @param chatRoomUnique
     */
    public void SendMessage(String msg, String chatRoomUnique) {
        String toSend = ConvertMsgTextToSocketStringFormat(msg, chatRoomUnique);

        ActiveChatRoom activeRoom = mActiveChatRooms.get(chatRoomUnique);

        if (activeRoom != null && activeRoom.isHostedGroupChat) {
            activeRoom.ForwardMessage(toSend.split("[" + Constants.STANDART_FIELD_SEPERATOR + "]"), true);
        } else {
            String peerIP = mDiscoveredChatRoomsHash.get(chatRoomUnique).Users.get(0).IPaddr;
            new SendControlMessage(mSocketSendResultHandler, peerIP, toSend, chatRoomUnique).start();
        }
    }

    /**
     * Converts a text message to a format that can be passed via socket to a peer
     *
     * @param msg            - the text message
     * @param chatRoomUnique - the target chat room's unique ID
     * @return the converted string
     */
    public String ConvertMsgTextToSocketStringFormat(String msg, String chatRoomUnique) {
        StringBuilder toSend = new StringBuilder();
        toSend.append(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG) + Constants.STANDART_FIELD_SEPERATOR); //add opcode
        toSend.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);   //add self name
        toSend.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);    //add unique
        toSend.append(chatRoomUnique + Constants.STANDART_FIELD_SEPERATOR);                //add chat room ID
        toSend.append(msg + Constants.STANDART_FIELD_SEPERATOR);                            //add msg
        return toSend.toString();
    }

    /**
     * Will be called by a 'ClientSocketHandler' when a reply comes from a peer we've requested to chat with.
     * Sends a broadcast with the result's details
     *
     * @param isApproved - true / false
     * @param reason     - the reason in the case of denial of access
     */
    public void OnReceptionOfChatEstablishmentReply(String RoomID, boolean isApproved, String reason) {
        if (isApproved) {
            if (mActiveChatRooms.get(RoomID) == null) {
                ChatRoomDetails details = mDiscoveredChatRoomsHash.get(RoomID); //get the room's details
                ActiveChatRoom room = new ActiveChatRoom(this, false, details);  //create a new chat room
                mActiveChatRooms.put(RoomID, room);  //add to the active chats hash
            }
        }

        //Broadcast the result
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT); //put opcode
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT,
                isApproved ? Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS : Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED);
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_REASON, reason);  //add the reason for the case of a failure
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, RoomID); //set the room's ID

        sendBroadcast(intent);
    }

    /**
     * Handles the result of the physical attempt to send a connection message.
     * A positive result only means that the message was sent, not that the peer accepted our char request
     *
     * @param result - the result of the send procedure
     * @param RoomID - the ID of the room this result is regarding
     */
    private void HandleSendAttemptAndBroadcastResult(String result, String RoomID) {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_JOIN_SENDING_RESULT); //the opcode is connection result event
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, RoomID); //set the room's ID

        if (result.equalsIgnoreCase(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS))
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT, Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS); //set a 'success' result
        else
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT, Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED); //set a 'failed' result value

        sendBroadcast(intent);
    }

    /**
     * Constructs a 'Join' message that can be sent via socket
     *
     * @param opcode       - the desired opcode (join public/private chat room)
     * @param RoomUniqueID - the room's unique ID
     * @param pw           - the room's password. NULL indicates that the room doesn't require a password
     * @return - a valid 'Join' string
     */
    private String ConstructJoinMessage(int opcode, String RoomUniqueID, String pw) {
        StringBuilder ans = new StringBuilder();

        if (opcode == Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST) {
            ans.append(Integer.toString(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST) + Constants.STANDART_FIELD_SEPERATOR); //add the opcode
            ans.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR); //add the username
            ans.append(MainScreenActivity.UniqueID + "\r\n"); //add our uniqueID
        } else {
            ans.append(Integer.toString(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST) + Constants.STANDART_FIELD_SEPERATOR); //add the opcode
            ans.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR); //add the username
            ans.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR); //add the user's uniqueID
            ans.append(RoomUniqueID + Constants.STANDART_FIELD_SEPERATOR); //add the room's uniqueID
            ans.append((pw == null ? "nopw" : pw) + "\r\n"); //add the password for the room
        }

        return ans.toString();
    }

    /**
     * Creates a new broadcast intent
     *
     * @return Intent
     */
    public Intent CreateBroadcastIntent() {
        return new Intent(Constants.SERVICE_BROADCAST);
    }

    /**
     * Updates the discovered chat rooms hash map
     *
     * @param chatRoomList - A discovered chat room (not necessarily new)
     */
    public void UpdateChatRoomHashMap(ArrayList<ChatRoomDetails> chatRoomList) {
        for (ChatRoomDetails refreshed : chatRoomList) {
            synchronized (mDiscoveredChatRoomsHash) {
                ChatRoomDetails existing = mDiscoveredChatRoomsHash.get(refreshed.RoomID);
                if (existing != null) {
                    existing.Name = refreshed.Name;
                    existing.Password = refreshed.Password;
                    existing.LastSeen = refreshed.LastSeen;
                    existing.Users = refreshed.Users;
                } else {
                    mDiscoveredChatRoomsHash.put(refreshed.RoomID, refreshed);    //update the hash map with the new chat room
                }

            }
        }

        BroadcastRoomsUpdatedEvent();
    }

    /**
     * Broadcasts an event informing the activities that the available rooms list was updated
     */
    public void BroadcastRoomsUpdatedEvent() {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ACTION_CHAT_ROOM_LIST_CHANGED);
        sendBroadcast(intent);
    }

    /**
     * Updates the peer list
     *
     * @param peerIP - A string containing the peer's IP address
     * @param unique - the peer's unqiue ID
     * @param name   - the peer's name
     */
    public void UpdateDiscoveredUsersList(String peerIP, String unique, String name) {
        mIsWifiPeerValid = true;
        boolean isFound = false;
        if (unique == null) {
            synchronized (mDiscoveredUsers) {
                for (Peer user : mDiscoveredUsers) {
                    if (user.IPaddr.equalsIgnoreCase(peerIP)) {
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    Peer peer = new Peer(null, peerIP, null); // creates a new peer
                    mDiscoveredUsers.add(peer);
                }
            }
        } else {
            synchronized (mDiscoveredUsers) {
                for (Peer user : mDiscoveredUsers) {
                    //if we found the user
                    if ((user.uniqueID != null && user.uniqueID.equalsIgnoreCase(unique)) || user.IPaddr.equalsIgnoreCase(peerIP)) {
                        user.IPaddr = peerIP;  //update the IP address
                        user.name = name;     //update the name
                        user.uniqueID = unique; //update the unique ID
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    Peer peer = new Peer(unique, peerIP, name); //create a new peer
                    mDiscoveredUsers.add(peer);
                }

            }
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocalService getService() {
            return LocalService.this;
        }
    }

    private long timeStampAtLastResfresh = 0;
    private long timeStampAtLastConnect = 0;

    /**
     * Called by the fragment when the user clicks the refresh button
     * Performs a query against all existing users in the mDiscoveredUsers list
     */
    public void OnRefreshButtonclicked() {
        String allDiscovredUsers = null;

        if (mIsWifiGroupOwner)
            allDiscovredUsers = BuildUsersPublicationString();

        if (!mDiscoveredUsers.isEmpty()) {
            for (Peer user : mDiscoveredUsers) {
                new ClientSocketHandler(this, user, Constants.CONNECTION_CODE_DISCOVER).start(); //start a new query
                //if this is the group's owner: send a peer publication string
                if (mIsWifiGroupOwner && allDiscovredUsers != null)
                    new SendControlMessage(user.IPaddr, allDiscovredUsers).start(); //send a peer publication message
            }
        }

        long currentTime = Constants.GetTime().getTime();

        if (((currentTime - timeStampAtLastResfresh) > Constants.MIN_TIME_BETWEEN_WIFI_DISCOVER_OPERATIONS_IN_MS)
                && ChatSearchScreenFrag.mIsConnectedToGroup == false) {
            timeStampAtLastResfresh = currentTime;
            DiscoverPeers();
            return;
        }

        //anyway, if we're not connected,
        //even if we don't do start a new 'discoverPeers()' procedure, we want to keep trying to connect to a peer
        if (ChatSearchScreenFrag.mIsConnectedToGroup == false)
            onPeerDeviceListAvailable();

    }

    /**
     * Initializes peer discovery. Results are handled by the broadcast receiver.
     * When the peer list is available, onPeerDeviceListAvailable() is called by the b-cast receiver
     */
    @SuppressLint("NewApi")
    public void DiscoverPeers() {
        //if the search fragment is bound to this service (the receiver is created in onBind())
        if (mWifiP2PReceiver != null) {
            mWifiP2PReceiver.isPeersDiscovered = false; //lower a flag in the receiver that'll allow peer discovery

            //start a new discovery
            ChatSearchScreenFrag.mManager.discoverPeers(ChatSearchScreenFrag.mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_SUCCESS, -1);
                }

                @Override
                public void onFailure(int reasonCode) {
                    CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_FAILED, reasonCode);
                }

            });
        }
    }

    private static int index = 0;

    public void onPeerDeviceListAvailable() {
        boolean isGroupOwnerDiscovered = false;
        long currentTime = Constants.GetTime().getTime();
        //if the device list is valid
        if (devices != null && devices.length != 0 && !ChatSearchScreenFrag.mIsConnectedToGroup
                && ((currentTime - timeStampAtLastConnect) > Constants.MIN_TIME_BETWEEN_WIFI_CONNECT_ATTEMPTS_IN_MS)) {
            timeStampAtLastConnect = currentTime; //save the time stamp at the last entry
            WifiP2pDevice device = null;
			/*Now we want to attempt a connection with another device*/
            synchronized (devices) {
                //in case one of the peers is the owner of a valid group, he's the one want want to connect to.
                //go over all available devices and try to find a group owner:
                for (WifiP2pDevice dev : devices) {
                    if (dev.isGroupOwner()) {
                        device = dev;
                        isGroupOwnerDiscovered = true;
                        break;
                    }
                }

                //If non of the devices is a group owner, just keep on trying to connect with one of them:
                if (!isGroupOwnerDiscovered) {
                    for (int k = 0; k < devices.length; k++) {
                        index %= devices.length;      //set the index to a legal value
                        if (devices[index] != null && devices[index].status == WifiP2pDevice.AVAILABLE) {
                            device = devices[index]; //get one of the discovered devices
                            break;
                        }
                        index++;
                    }

                    if (device == null)
                        return;
                }
            }

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;               //set security properties to 'Display push button to approve peer'
            ChatSearchScreenFrag.mManager.connect(ChatSearchScreenFrag.mChannel, config, new ActionListener() {

                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reason) {
                    ChatSearchScreenFrag.mIsConnectedToGroup = false;
                }
            });
        }
    }

    /**
     * Updates the intent-filter member variable
     */
    private void UpdateIntentFilter() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    /**
     * Terminates the Welcome-socket thread and self-kills the service
     */
    public void kill() {
        if (serverSocketHandler != null) {
            serverSocketHandler.stopSocket();
            serverSocketHandler.interrupt(); //close the welcome socket thread
        }

        try {
            if (mWifiP2PReceiver != null)
                unregisterReceiver(mWifiP2PReceiver);
        } catch (Exception e) {
        }

        stopSelf();
    }

    /**
     * Initializes member variables
     */
    private void InitClassMembers() {
        if (mDiscoveredChatRoomsHash == null) {
            mDiscoveredChatRoomsHash = new HashMap<String, ChatRoomDetails>();
        }

        if (mDiscoveredUsers == null) {
            mDiscoveredUsers = new ArrayList<Peer>();
        }

        if (mActiveChatRooms == null) {
            mActiveChatRooms = new HashMap<String, ActiveChatRoom>();
        }

        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Creates a new hosted public chat room
     */
    public void CreateNewHostedPublicChatRoom(String name, String password) {
        CreateNewHostedPublicChatRoom(name, password, null, null);
    }

    /**
     * Creates a new hosted public chat room
     *
     * @param name         - the room's desired name
     * @param password     - the room's desired password. NULL means that there's no password
     * @param mListContent
     */
    public void CreateNewHostedPublicChatRoom(String name, String password, ArrayList<ChatMessage> mListContent, ChatRoomDetails details) {
        ChatRoomDetails newDetails;
        if (details != null) {
            newDetails  = details;
        }else{
            newDetails = new ChatRoomDetails(
                    MainScreenActivity.UniqueID + "_" + (++MainScreenActivity.ChatRoomAccumulatingSerialNumber),
                    name, null, new ArrayList<Peer>(), password, false);
        }

        //create a new active room:
        ActiveChatRoom newActiveRoom = new ActiveChatRoom(this, true, newDetails);
        //init a history file if necessary:
        InitHistoryFileIfNecessary(newActiveRoom.mRoomInfo.RoomID, newActiveRoom.mRoomInfo.Name, false);
        //put the room into the hash map:
        mActiveChatRooms.put(newActiveRoom.mRoomInfo.RoomID, newActiveRoom);
        //b-cast an event that'll cause the chat-search-frag to refresh the list view
        BroadcastRoomsUpdatedEvent();
        //start a new logic discovery procedure to update all peers about the new room list
        OnRefreshButtonclicked();

        this.mListContent = mListContent;
    }

    /**
     * Creates a new active chat room, if it doesn't exist already
     *
     * @param input - incoming request string after parsing
     */
    public void CreateNewPrivateChatRoom(String[] input) {
        if (mActiveChatRooms.get(input[2]) == null) {
            ChatRoomDetails details = mDiscoveredChatRoomsHash.get(input[2]); //get a reference to the chat's details
            ActiveChatRoom room = new ActiveChatRoom(this, false, details);  //create a new chat room

            //check if a history file should be created for this new room
            InitHistoryFileIfNecessary(details.RoomID, details.Name, details.isPrivateChatRoom);

            mActiveChatRooms.put(input[2], room);
        }
    }

    /**
     * Initializes a history file if it doesn't exist already
     *
     * @param roomID    - the chat room's ID
     * @param RoomName  - the chat room's name
     * @param isPrivate - true if this room is private, false otherwise
     */
    private void InitHistoryFileIfNecessary(String roomID, String RoomName, boolean isPrivate) {
        //We want to know if a history file exists already
        String path = getFilesDir().getPath() + "/" + roomID + ".txt";
        File f = new File(path);
        if (!f.isFile()) {
            //create a new history file:
            ChatActivity.InitHistoryFile(roomID, null, RoomName, isPrivate, this);
        }
    }

    /**
     * The user wished to leave a public chat room which he's not the host of
     *
     * @param info - the room's details
     */
    public void CloseNotHostedPublicChatRoom(ChatRoomDetails info) {
        ActiveChatRoom activeRoom = mActiveChatRooms.get(info.RoomID);
        if (activeRoom != null) {
            activeRoom.DisconnectFromHostingPeer();
            mActiveChatRooms.remove(activeRoom.mRoomInfo.RoomID);  //remove from the active rooms list
            BroadcastRoomsUpdatedEvent();
        }
    }

    /**
     * The host wished to leave and close a public chat room
     *
     * @param info - the room's details
     */
    public void CloseHostedPublicChatRoom(ChatRoomDetails info) {
        ActiveChatRoom activeRoom = mActiveChatRooms.get(info.RoomID);
        if (activeRoom != null) {
            activeRoom.CloseRoomAndNotifyUsers();
            mActiveChatRooms.remove(activeRoom.mRoomInfo.RoomID);
            BroadcastRoomsUpdatedEvent();
        }
    }

    /**
     * Relevant only for a private chat
     * Updates the peer's name in the history log file
     *
     * @param info - the room's details
     */
    public void UpdatePrivateChatRoomNameInHistoryFile(ChatRoomDetails info) {
        ActiveChatRoom room = mActiveChatRooms.get(info.RoomID);
        if (room != null)
            room.updateUserNameInTheHistoryLogFile();
    }


    /**
     * Creates a string with all relevant active chat rooms that have new messages.
     * This string is later shown to the user via notification
     *
     * @return string
     */
    private String CreateNewMsgNotificationString() {
        StringBuilder builder = new StringBuilder();
        int numOfRoomsWithNewMsgs = 0;

        Collection<ActiveChatRoom> chatRooms = mActiveChatRooms.values();  //get all available active chat rooms
        for (ActiveChatRoom room : chatRooms) {
            if ((DisplayedAtChatActivity == null || !room.mRoomInfo.RoomID.equalsIgnoreCase(DisplayedAtChatActivity.RoomID))
                    && room.mRoomInfo.hasNewMsg) {
                builder.append(room.mRoomInfo.Name + ", "); //add the room's name
                numOfRoomsWithNewMsgs++;
            }
        }

        if (numOfRoomsWithNewMsgs == 0) //if we don't need to raise a notification
            return null;

        int length = builder.length();
        builder.delete(length - 2, length); //remove the ", " at the end

        if (numOfRoomsWithNewMsgs == 1)  //if there's only one chat room with new messages
            builder.insert(0, "New messages available: ");
        else //we have new messages in more than 1 room
            builder.insert(0, "At " + Integer.toString(numOfRoomsWithNewMsgs) +
                    " chat rooms: ");

        return builder.toString();
    }

    /**
     * Removes a single chat room from the discovered rooms list
     * Called by the {@link ChatActivity} when a 'Room closed' or 'Room not exisitng' message is received
     *
     * @param RoomID
     */
    public void RemoveFromDiscoveredChatRooms(String RoomID) {
        synchronized (mDiscoveredChatRoomsHash) {
            ChatRoomDetails toRemove = mDiscoveredChatRoomsHash.get(RoomID);
            if (toRemove != null)
                mDiscoveredChatRoomsHash.remove(RoomID);
        }

        BroadcastRoomsUpdatedEvent();
    }

    /**
     * Removes a single discovered chat room on the event of timeout
     *
     * @param details           - the room's details
     * @param isCalledByService - true if called from the service, false otherwise
     */
    public void RemoveSingleTimedOutRoom(ChatRoomDetails details, boolean isCalledByService) {
        if (isCalledByService) {
            //if no room is displayed or the room to be deleted isn't the displayed room
            if (DisplayedAtChatActivity == null || !DisplayedAtChatActivity.RoomID.equalsIgnoreCase(details.RoomID)) {
                synchronized (mActiveChatRooms) {
                    synchronized (mDiscoveredChatRoomsHash) {
                        if (mActiveChatRooms.containsKey(details.RoomID))
                            mActiveChatRooms.remove(details.RoomID);
                        // remove it from the has as well
                        mDiscoveredChatRoomsHash.remove(details.RoomID);  //remove from the discovered chat rooms hash
                    }
                }
            } else {
                Intent intent = CreateBroadcastIntent();
                intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, details.RoomID);
                intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ROOM_TIMED_OUT);
                sendBroadcast(intent);
            }

        } else {
            synchronized (mActiveChatRooms) {
                synchronized (mDiscoveredChatRoomsHash) {
                    if (mActiveChatRooms.containsKey(details.RoomID))
                        mActiveChatRooms.remove(details.RoomID);

                    mDiscoveredChatRoomsHash.remove(details.RoomID);  //remove from the discovered chat rooms hash
                }
            }
        }

        if (!isCalledByService)
            BroadcastRoomsUpdatedEvent();
    }

    /**
     * Deletes all timed out chat rooms
     */
    private void DeleteTimedOutRooms() {
        Date currentTime = Constants.GetTime();
        long timeDiff = 0;

        Collection<ChatRoomDetails> chatRooms = mDiscoveredChatRoomsHash.values();  //get all discovered chat rooms

        int numOfRooms = chatRooms.size();
        ChatRoomDetails[] allRooms = new ChatRoomDetails[numOfRooms];
        int i = 0;

        synchronized (mDiscoveredChatRoomsHash) {
            //get the hash's content
            for (ChatRoomDetails room : chatRooms) {
                allRooms[i++] = room;
            }

            for (i = 0; i < numOfRooms; i++) {
                timeDiff = currentTime.getTime() - allRooms[i].LastSeen.getTime();
                if (timeDiff >= MainScreenActivity.refreshPeriod * Constants.TO_FACTOR) {
                    RemoveSingleTimedOutRoom(allRooms[i], true);
                }
            }
        }

        BroadcastRoomsUpdatedEvent();
    }

    /**
     * Used by a peer who's also the groups owner. Creates a string of all available users to be sent across the group.
     *
     * @return valid publication string, NULL if there are no discovered users
     */
    private String BuildUsersPublicationString() {
        StringBuilder res = new StringBuilder(Integer.toString(Constants.CONNECTION_CODE_PEER_DETAILS_BCAST) + Constants.STANDART_FIELD_SEPERATOR
                + MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR
                + MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
        boolean isUserListNotEmpty = false;

        for (Peer user : mDiscoveredUsers) {
            if (user.IPaddr != null && user.name != null && user.uniqueID != null) {
                res.append(user.IPaddr + Constants.STANDART_FIELD_SEPERATOR
                        + user.name + Constants.STANDART_FIELD_SEPERATOR
                        + user.uniqueID + Constants.STANDART_FIELD_SEPERATOR);
                isUserListNotEmpty = true;
            }
        }
        if (!isUserListNotEmpty)
            return null;

        return res.toString();
    }

    public Vector<ChatRoomDetails> getChatRooms() {
        Vector<ChatRoomDetails> ret = new Vector<ChatRoomDetails>();
        Collection<ChatRoomDetails> tempRoomCol = mDiscoveredChatRoomsHash.values();

        for (ChatRoomDetails tempRoom : tempRoomCol) {
            ret.add(tempRoom);
        }

        return ret;
    }
}