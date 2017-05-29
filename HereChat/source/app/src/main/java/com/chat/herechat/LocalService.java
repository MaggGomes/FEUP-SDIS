package com.chat.herechat;


import java.io.File;
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

    public HashMap<String, ChatRoomDetails> hashChatroom = null;
    public HashMap<String, ActiveChatRoom> hashChatroomActive = null;

    public WiFiDirectBroadcastReceiver WifiP2PReceiver = null;

    private final IBinder binder = new LocalBinder();
    private ServerSocketHandler serverSocketHandler = null;
    private Handler socketResultHandler = null;
    public Handler refreshHandler = null;

    public IntentFilter IntentFilter;
    public WifiP2pDevice[] devices;

    public ArrayList<Peer> userDiscovered = null;
    public boolean groupOwner = false;
    public ArrayList<ChatMessage> listContent;


    public static NotificationManager notifications = null;
    public boolean activeChat = false;
    public ChatRoomDetails DisplayedAtChatActivity = null;

    private boolean validPeer = false;
    public final int timeoutPeer = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        InitClassMembers();

        if (socketResultHandler == null)
            socketResultHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    String result = data.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT);
                    String RoomID = data.getString(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID);

                    HandleSendAttemptAndBroadcastResult(result, RoomID);
                }
            };

        if (refreshHandler == null) {
            refreshHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //if this is a wifi peer validation TO
                    if (msg.what == timeoutPeer) {
                        if (!validPeer) {
                            ChatSearchScreenFrag.groupConnect = false;
                            ChatSearchScreenFrag.Manage.removeGroup(ChatSearchScreenFrag.cChannel, null);
                        }
                    } else {
                        if (ChatSearchScreenFrag.wifiDirect)
                            LocalService.this.OnRefreshButtonclicked();
                        DeleteTimedOutRooms();

                        sendEmptyMessageDelayed(0, MainScreenActivity.refreshPeriod);
                    }
                }
            };

            refreshHandler.sendEmptyMessageDelayed(0, 500);
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
        if (WifiP2PReceiver == null) {
            UpdateIntentFilter();

            WifiP2PReceiver = new WiFiDirectBroadcastReceiver(ChatSearchScreenFrag.cChannel, this, ChatSearchScreenFrag.Manage);
            getApplication().registerReceiver(WifiP2PReceiver, IntentFilter);
        }
        return binder;
    }


    public void CreateAndBroadcastWifiP2pEvent(int wifiEventCode, int failReasonCode) {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ACTION_WIFI_EVENT_VALUE);
        intent.putExtra(Constants.SERVICE_BROADCAST_WIFI_EVENT_KEY, wifiEventCode);
        intent.putExtra(Constants.SERVICE_BROADCAST_WIFI_EVENT_FAIL_REASON_KEY, failReasonCode);
        sendBroadcast(intent);
    }

    public void EstablishChatConnection(String roomUniqueID, String password, boolean isPrivateChat) {

        if (hashChatroomActive.get(roomUniqueID) == null) {
            String msg;

            if (isPrivateChat)
                msg = ConstructJoinMessage(Constants.CONNECTION_CODE_PRIVATE_CHAT_REQUEST, null, null);

            else
                msg = ConstructJoinMessage(Constants.CONNECTION_CODE_JOIN_ROOM_REQUEST, roomUniqueID, password);

            String peerIP = hashChatroom.get(roomUniqueID).users.get(0).IPaddress;
            new SendControlMessage(socketResultHandler, peerIP, msg, roomUniqueID).start();
        } else {
            Intent intent = CreateBroadcastIntent();
            intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT); //the opcode is connection result event
            //mark that an active room already exists:
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT, Constants.SINGLE_SEND_THREAD_ACTION_RESULT_ALREADY_CONNECTED);
            intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, roomUniqueID);
            sendBroadcast(intent);
        }
    }

    public void OnServerSocketError() {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_WELCOME_SOCKET_CREATE_FAIL);
    }


    public void OnMessageRecieved(String[] msg, String peerIP) {
        boolean isPrivateMsg = false;
        ActiveChatRoom targetRoom = null;
        if (msg[3].equalsIgnoreCase(MainScreenActivity.UniqueID)) {
            msg[3] = msg[2];
            isPrivateMsg = true;
        }

        targetRoom = hashChatroomActive.get(msg[3]);

        if (targetRoom != null) {
            targetRoom.SendMessage(false, msg);

            if (!targetRoom.isPublicHosted)
                UpdateTimeChatRoomLastSeenTimeStamp(msg[2], msg[3]);
        } else {
            SkipDiscovery(msg, true, isPrivateMsg);
        }

        if (MainScreenActivity.isToNotifyOnNewMsg &&
                (DisplayedAtChatActivity == null || !msg[3].equalsIgnoreCase(DisplayedAtChatActivity.roomID))) {

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
                Constants.ShowNotification(resultPendingIntent, notificationString);
        }

        BroadcastRoomsUpdatedEvent();
    }

    private void UpdateTimeChatRoomLastSeenTimeStamp(String peerUnique, String roomUnique) {
        Date currentTime = Constants.GetTime();
        ChatRoomDetails ChatDetails = hashChatroom.get(peerUnique);

        if (ChatDetails != null)
            ChatDetails.lastSeen = currentTime;

        if (!peerUnique.equalsIgnoreCase(roomUnique)) {
            ChatDetails = hashChatroom.get(roomUnique);
            if (ChatDetails != null)
                ChatDetails.lastSeen = currentTime;
        }
    }


    public void SkipDiscovery(String[] msg, boolean isChatMsg, boolean isPrivateChat) {
        if (isPrivateChat) {
            ArrayList<ChatRoomDetails> ChatRooms = new ArrayList<ChatRoomDetails>();
            Peer host = Constants.CheckUniqueID(msg[2], userDiscovered);

            ArrayList<Peer> user = new ArrayList<Peer>();
            user.add(host);

            ChatRoomDetails PrivateChatRoom = new ChatRoomDetails(host.uniqueID, host.name, Constants.GetTime(), user, true);
            ChatRooms.add(PrivateChatRoom);
            UpdateChatroom(ChatRooms);
            CreateNewPrivateChatRoom(msg);
            ActiveChatRoom targetRoom = hashChatroomActive.get(msg[2]);

            if (isChatMsg)
                targetRoom.SendMessage(false, msg);
        } else {
            ChatRoomDetails details = hashChatroom.get(msg[3]);
            if (details == null) {
                OnRefreshButtonclicked();
            } else {
                ActiveChatRoom activeRoom = new ActiveChatRoom(this, false, details);
                InitHistoryFileIfNecessary(details.roomID, details.name, false);
                hashChatroomActive.put(details.roomID, activeRoom);
                BroadcastRoomsUpdatedEvent();
            }
        }
    }


    public void SendMessage(String msg, String chatRoomUnique) {
        String toSend = ConvertMsgTextToSocketStringFormat(msg, chatRoomUnique);

        ActiveChatRoom activeRoom = hashChatroomActive.get(chatRoomUnique);

        if (activeRoom != null && activeRoom.isPublicHosted) {
            activeRoom.SendMessage(true, toSend.split("[" + Constants.STANDART_FIELD_SEPERATOR + "]"));
        } else {
            String peerIP = hashChatroom.get(chatRoomUnique).users.get(0).IPaddress;
            new SendControlMessage(socketResultHandler, peerIP, toSend, chatRoomUnique).start();
        }
    }


    public String ConvertMsgTextToSocketStringFormat(String msg, String chatRoomUnique) {
        StringBuilder toSend = new StringBuilder();
        toSend.append(Integer.toString(Constants.CONNECTION_CODE_NEW_CHAT_MSG) + Constants.STANDART_FIELD_SEPERATOR);
        toSend.append(MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR);
        toSend.append(MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
        toSend.append(chatRoomUnique + Constants.STANDART_FIELD_SEPERATOR);
        toSend.append(msg + Constants.STANDART_FIELD_SEPERATOR);
        return toSend.toString();
    }

    public Vector<ChatRoomDetails> getChatrooms() {
        Vector<ChatRoomDetails> ret = new Vector<ChatRoomDetails>();
        Collection<ChatRoomDetails> tempRoomCol = hashChatroom.values();

        for (ChatRoomDetails tempRoom : tempRoomCol) {
            ret.add(tempRoom);
        }

        return ret;
    }

    public void OnRecieveReply(String RoomID, String reason, boolean approved) {
        if (approved) {
            if (hashChatroomActive.get(RoomID) == null) {
                ChatRoomDetails details = hashChatroom.get(RoomID); //get the room's details
                ActiveChatRoom room = new ActiveChatRoom(this, false, details);  //create a new chat room
                hashChatroomActive.put(RoomID, room);  //add to the active chats hash
            }
        }


        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT); //put opcode
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_RESULT,
                approved ? Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS : Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED);
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_REASON, reason);  //add the reason for the case of a failure
        intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, RoomID); //set the room's ID

        sendBroadcast(intent);
    }


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


    public Intent CreateBroadcastIntent() {
        return new Intent(Constants.SERVICE_BROADCAST);
    }


    public void UpdateChatroom(ArrayList<ChatRoomDetails> chatRoomList) {
        for (ChatRoomDetails refreshed : chatRoomList) {
            synchronized (hashChatroom) {
                ChatRoomDetails existing = hashChatroom.get(refreshed.roomID);
                if (existing != null) {
                    existing.name = refreshed.name;
                    existing.password = refreshed.password;
                    existing.lastSeen = refreshed.lastSeen;
                    existing.users = refreshed.users;
                } else {
                    hashChatroom.put(refreshed.roomID, refreshed);    //update the hash map with the new chat room
                }

            }
        }

        BroadcastRoomsUpdatedEvent();
    }


    public void BroadcastRoomsUpdatedEvent() {
        Intent intent = CreateBroadcastIntent();
        intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ACTION_CHAT_ROOM_LIST_CHANGED);
        sendBroadcast(intent);
    }


    public void UpdateDiscoveredUsersList(String peerIP, String unique, String name) {
        validPeer = true;
        boolean isFound = false;
        if (unique == null) {
            synchronized (userDiscovered) {
                for (Peer user : userDiscovered) {
                    if (user.IPaddress.equalsIgnoreCase(peerIP)) {
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    Peer peer = new Peer(null, null, peerIP); // creates a new peer
                    userDiscovered.add(peer);
                }
            }
        } else {
            synchronized (userDiscovered) {
                for (Peer user : userDiscovered) {
                    //if we found the user
                    if ((user.uniqueID != null && user.uniqueID.equalsIgnoreCase(unique)) || user.IPaddress.equalsIgnoreCase(peerIP)) {
                        user.IPaddress = peerIP;  //update the IP address
                        user.name = name;     //update the name
                        user.uniqueID = unique; //update the unique ID
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    Peer peer = new Peer(name, unique, peerIP); //create a new peer
                    userDiscovered.add(peer);
                }

            }
        }
    }


    public class LocalBinder extends Binder {
        public LocalService getService() {
            return LocalService.this;
        }
    }

    private long timeStampAtLastResfresh = 0;
    private long timeStampAtLastConnect = 0;


    public void OnRefreshButtonclicked() {
        String discoveredUser = null;

        if (groupOwner)
            discoveredUser = PublishString();

        if (!userDiscovered.isEmpty()) {
            for (Peer user : userDiscovered) {
                new ClientSocketHandler(this, user, Constants.CONNECTION_CODE_DISCOVER).start(); //start a new query
                //if this is the group's owner: send a peer publication string
                if (groupOwner && discoveredUser != null)
                    new SendControlMessage(user.IPaddress, discoveredUser).start(); //send a peer publication message
            }
        }

        long currentTime = Constants.GetTime().getTime();

        if (((currentTime - timeStampAtLastResfresh) > Constants.MIN_TIME_BETWEEN_WIFI_DISCOVER_OPERATIONS_IN_MS)
                && ChatSearchScreenFrag.groupConnect == false) {
            timeStampAtLastResfresh = currentTime;
            DiscoverPeers();
            return;
        }

        //anyway, if we're not connected,
        //even if we don't do start a new 'discoverPeers()' procedure, we want to keep trying to connect to a peer
        if (ChatSearchScreenFrag.groupConnect == false)
            onPeerDeviceListAvailable();

    }


    @SuppressLint("NewApi")
    public void DiscoverPeers() {

        if (WifiP2PReceiver != null) {
            WifiP2PReceiver.isPeersDiscovered = false; //lower a flag in the receiver that'll allow peer discovery

            //start a new discovery
            ChatSearchScreenFrag.Manage.discoverPeers(ChatSearchScreenFrag.cChannel, new WifiP2pManager.ActionListener() {
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

        if (devices != null && devices.length != 0 && !ChatSearchScreenFrag.groupConnect
                && ((currentTime - timeStampAtLastConnect) > Constants.MIN_TIME_BETWEEN_WIFI_CONNECT_ATTEMPTS_IN_MS)) {
            timeStampAtLastConnect = currentTime; //save the time stamp at the last entry
            WifiP2pDevice device = null;

            synchronized (devices) {
                for (WifiP2pDevice dev : devices) {
                    if (dev.isGroupOwner()) {
                        device = dev;
                        isGroupOwnerDiscovered = true;
                        break;
                    }
                }


                if (!isGroupOwnerDiscovered) {
                    for (int k = 0; k < devices.length; k++) {
                        index %= devices.length;
                        if (devices[index] != null && devices[index].status == WifiP2pDevice.AVAILABLE) {
                            device = devices[index];
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
            ChatSearchScreenFrag.Manage.connect(ChatSearchScreenFrag.cChannel, config, new ActionListener() {

                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int reason) {
                    ChatSearchScreenFrag.groupConnect = false;
                }
            });
        }
    }


    private void UpdateIntentFilter() {
        IntentFilter = new IntentFilter();
        IntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        IntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        IntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        IntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }


    public void kill() {
        if (serverSocketHandler != null) {
            serverSocketHandler.stopSocket();
            serverSocketHandler.interrupt(); //close the welcome socket thread
        }

        try {
            if (WifiP2PReceiver != null)
                unregisterReceiver(WifiP2PReceiver);
        } catch (Exception e) {
        }

        stopSelf();
    }


    private void InitClassMembers() {
        if (hashChatroom == null) {
            hashChatroom = new HashMap<String, ChatRoomDetails>();
        }

        if (userDiscovered == null) {
            userDiscovered = new ArrayList<Peer>();
        }

        if (hashChatroomActive == null) {
            hashChatroomActive = new HashMap<String, ActiveChatRoom>();
        }

        if (notifications == null)
            notifications = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }


    public void CreateNewHostedPublicChatRoom(String name, String password) {
        CreateNewHostedPublicChatRoom(name, password, null, null);
    }

    public void CloseHostedPublicChatRoom(ChatRoomDetails info) {
        ActiveChatRoom activeRoom = hashChatroomActive.get(info.roomID);
        if (activeRoom != null) {
            activeRoom.CloseRoomNotification();
            hashChatroomActive.remove(activeRoom.roomInfo.roomID);
            BroadcastRoomsUpdatedEvent();
        }
    }


    public void UpdatePrivateChatRoomNameInHistoryFile(ChatRoomDetails info) {
        ActiveChatRoom room = hashChatroomActive.get(info.roomID);
        if (room != null)
            room.UpdateUserInHistory();
    }


    public void CreateNewHostedPublicChatRoom(String name, String password, ArrayList<ChatMessage> mListContent, ChatRoomDetails details) {
        ChatRoomDetails newDetails;
        if (details != null) {
            newDetails  = details;
        }else{
            newDetails = new ChatRoomDetails(
                    name, MainScreenActivity.UniqueID + "_" + (++MainScreenActivity.ChatRoomAccumulatingSerialNumber),
                    password, new ArrayList<Peer>(), null, false);
        }

        //create a new active room:
        ActiveChatRoom newActiveRoom = new ActiveChatRoom(this, true, newDetails);
        //init a history file if necessary:
        InitHistoryFileIfNecessary(newActiveRoom.roomInfo.roomID, newActiveRoom.roomInfo.name, false);
        //put the room into the hash map:
        hashChatroomActive.put(newActiveRoom.roomInfo.roomID, newActiveRoom);
        //b-cast an event that'll cause the chat-search-frag to refresh the list view
        BroadcastRoomsUpdatedEvent();
        //start a new logic discovery procedure to update all peers about the new room list
        OnRefreshButtonclicked();

        this.listContent = mListContent;
    }


    public void CreateNewPrivateChatRoom(String[] input) {
        if (hashChatroomActive.get(input[2]) == null) {
            ChatRoomDetails details = hashChatroom.get(input[2]); //get a reference to the chat's details
            ActiveChatRoom room = new ActiveChatRoom(this, false, details);  //create a new chat room

            //check if a history file should be created for this new room
            InitHistoryFileIfNecessary(details.roomID, details.name, details.isPrivateChatRoom);

            hashChatroomActive.put(input[2], room);
        }
    }


    private void InitHistoryFileIfNecessary(String roomID, String RoomName, boolean isPrivate) {

        String path = getFilesDir().getPath() + "/" + roomID + ".txt";
        File f = new File(path);
        if (!f.isFile()) {

            ChatActivity.initHistoryFile(null, roomID, RoomName, this, isPrivate);
        }
    }


    public void CloseNotHostedPublicChatRoom(ChatRoomDetails info) {
        ActiveChatRoom activeRoom = hashChatroomActive.get(info.roomID);
        if (activeRoom != null) {
            activeRoom.DisconnectFromHostPeer();
            hashChatroomActive.remove(activeRoom.roomInfo.roomID);  //remove from the active rooms list
            BroadcastRoomsUpdatedEvent();
        }
    }


    private String CreateNewMsgNotificationString() {
        StringBuilder builder = new StringBuilder();
        int numOfRoomsWithNewMsgs = 0;

        Collection<ActiveChatRoom> chatRooms = hashChatroomActive.values();  //get all available active chat rooms
        for (ActiveChatRoom room : chatRooms) {
            if ((DisplayedAtChatActivity == null || !room.roomInfo.roomID.equalsIgnoreCase(DisplayedAtChatActivity.roomID))
                    && room.roomInfo.hasNewMsg) {
                builder.append(room.roomInfo.name + ", ");
                numOfRoomsWithNewMsgs++;
            }
        }

        if (numOfRoomsWithNewMsgs == 0)
            return null;

        int length = builder.length();
        builder.delete(length - 2, length);

        if (numOfRoomsWithNewMsgs == 1)
            builder.insert(0, "New messages available: ");
        else
            builder.insert(0, "At " + Integer.toString(numOfRoomsWithNewMsgs) +
                    " chat rooms: ");

        return builder.toString();
    }


    public void RemoveFromDiscoveredChatRooms(String RoomID) {
        synchronized (hashChatroom) {
            ChatRoomDetails toRemove = hashChatroom.get(RoomID);
            if (toRemove != null)
                hashChatroom.remove(RoomID);
        }

        BroadcastRoomsUpdatedEvent();
    }


    public void RemoveSingleTimedOutRoom(ChatRoomDetails details, boolean serviceRequest) {
        if (serviceRequest) {
            if (DisplayedAtChatActivity == null || !DisplayedAtChatActivity.roomID.equalsIgnoreCase(details.roomID)) {
                synchronized (hashChatroomActive) {
                    synchronized (hashChatroom) {
                        if (hashChatroomActive.containsKey(details.roomID))
                            hashChatroomActive.remove(details.roomID);
                        // remove it from the has as well
                        hashChatroom.remove(details.roomID);  //remove from the discovered chat rooms hash
                    }
                }
            } else {
                Intent intent = CreateBroadcastIntent();
                intent.putExtra(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID, details.roomID);
                intent.putExtra(Constants.SERVICE_BROADCAST_OPCODE_KEY, Constants.SERVICE_BROADCAST_OPCODE_ROOM_TIMED_OUT);
                sendBroadcast(intent);
            }

        } else {
            synchronized (hashChatroomActive) {
                synchronized (hashChatroom) {
                    if (hashChatroomActive.containsKey(details.roomID))
                        hashChatroomActive.remove(details.roomID);

                    hashChatroom.remove(details.roomID);
                }
            }
        }

        if (!serviceRequest)
            BroadcastRoomsUpdatedEvent();
    }


    private void DeleteTimedOutRooms() {
        Date currentTime = Constants.GetTime();
        long countTime = 0;

        Collection<ChatRoomDetails> chatRooms = hashChatroom.values();

        int numOfRooms = chatRooms.size();
        ChatRoomDetails[] allRooms = new ChatRoomDetails[numOfRooms];
        int i = 0;

        synchronized (hashChatroom) {
            for (ChatRoomDetails room : chatRooms) {
                allRooms[i++] = room;
            }

            for (i = 0; i < numOfRooms; i++) {
                countTime = currentTime.getTime() - allRooms[i].lastSeen.getTime();
                if (countTime >= MainScreenActivity.refreshPeriod * Constants.TO_FACTOR) {
                    RemoveSingleTimedOutRoom(allRooms[i], true);
                }
            }
        }

        BroadcastRoomsUpdatedEvent();
    }


    private String PublishString() {
        StringBuilder res = new StringBuilder(Integer.toString(Constants.CONNECTION_CODE_PEER_DETAILS_BCAST) + Constants.STANDART_FIELD_SEPERATOR
                + MainScreenActivity.UserName + Constants.STANDART_FIELD_SEPERATOR
                + MainScreenActivity.UniqueID + Constants.STANDART_FIELD_SEPERATOR);
        boolean isUserListNotEmpty = false;

        for (Peer user : userDiscovered) {
            if (user.IPaddress != null && user.name != null && user.uniqueID != null) {
                res.append(user.IPaddress + Constants.STANDART_FIELD_SEPERATOR
                        + user.name + Constants.STANDART_FIELD_SEPERATOR
                        + user.uniqueID + Constants.STANDART_FIELD_SEPERATOR);
                isUserListNotEmpty = true;
            }
        }
        if (!isUserListNotEmpty)
            return null;

        return res.toString();
    }

}