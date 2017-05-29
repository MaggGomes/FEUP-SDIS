package com.chat.herechat.Utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.chat.herechat.ChatManager.ChatSearchScreenFrag;
import com.chat.herechat.LocalService;
import com.chat.herechat.Peer.Peer;

public final class Constants {
	public static final char STANDART_FIELD_SEPERATOR = (char)222;
	public static final char CHAT_MSG_ENTRY_SEPARATOR_CHAR = (char)223;
	public static final char ENTER_REPLACEMENT_CHAR = (char)224;

	//A chat rooms is defined as timed-out after it wasn't seen for TO_FACTOR * MainScreenActivity.RefreshPeriodInMs
	public static int TO_FACTOR = 2;

	public static long MIN_TIME_BETWEEN_WIFI_DISCOVER_OPERATIONS_IN_MS = 60000; //60 seconds between discoveries
	public final static long MIN_TIME_BETWEEN_WIFI_CONNECT_ATTEMPTS_IN_MS = 30000; 	//30 seconds between connection attempts
	//from the time of connection, we set a TO to check if this peer is responsive and runs our app correctly
	public final static long VALID_COMM_WITH_WIFI_PEER_TO = 40000;

	public final static int NUM_OF_QUERY_RECEIVE_RETRIES = 30;

	public static final String SERVICE_POSTIVE_REPLY_FOR_JOIN_REQUEST = "ACCEPTED";
	public static final String SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST = "DENIED";
	public static final String SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW = "WRONG PW";
	public static final String SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM = "NOT EXIST";
	public static final String SERVICE_NEGATIVE_REPLY_REASON_ROOM_CLOSED = "CLOSED";

	public static final int CONNECTION_CODE_PEER_DETAILS_BCAST = 2;
	public static final int CONNECTION_CODE_DISCOVER = 3;
	public static final int CONNECTION_CODE_JOIN_ROOM_REQUEST = 4;
	public static final int CONNECTION_CODE_PRIVATE_CHAT_REQUEST = 5;
	public static final int CONNECTION_CODE_JOIN_ROOM_REPLY = 6;
	public static final int CONNECTION_CODE_PRIVATE_CHAT_REPLY = 7;
	public static final int CONNECTION_CODE_NEW_CHAT_MSG = 8;
	public static final int CONNECTION_CODE_DISCONNECT_FROM_CHAT_ROOM = 9;

	public static final String SERVICE_BROADCAST = "com.example.android_final_proj.SERVICE_BCAST";

	public static final String WIFI_BCAST_RCVR_WIFI_OFF_EVENT_INTENT_EXTRA_KEY = "key";

	public static final String SHARED_PREF_CHAT_ROOM_SERIAL_NUM = "SN";
	public static final String SHARED_PREF_USER_NAME = "NAME";
	public static final String SHARED_PREF_UNIQUE_ID = "ID";
	public static final String SHARED_PREF_ENABLE_NOTIFICATION = "ENABLE_NOTIFICATION";
	public static final String SHARED_PREF_REFRESH_PERIOD = "PERIOD";
	public static final String SHARED_PREF_IS_FIRST_RUN = "FIRST RUN";

	public static final String SINGLE_SEND_THREAD_ACTION_RESULT_FAILED = "0";
	public static final String SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS= "1";
	public static final String SINGLE_SEND_THREAD_ACTION_RESULT_ALREADY_CONNECTED = "2";

	public static final String SINGLE_SEND_THREAD_KEY_RESULT= "RES";
	public static final String SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID= "ID";
	public static final String SINGLE_SEND_THREAD_KEY_REASON= "REASON";

	public static final String HASH_MAP_KEY_SEARCH_FRAG_CHAT_NAME = "NAME";
	public static final String HASH_MAP_KEY_SEARCH_FRAG_PARTICIPANTS = "PARTICIPANTS";
	public static final String HASH_MAP_KEY_SEARCH_FRAG_ICON = "ICON";
	public static final String HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE = "UNIQUE";
	public static final String HASH_MAP_KEY_SEARCH_HOSTED_PUBLIC_ROOM_ICON = "HOSTED ICON";
	public static final String HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON = "LOCK ICON";
	public static final String HASH_MAP_KEY_SEARCH_NEW_MSG_ICON = "NEW MSG";

	public final static String SERVICE_BROADCAST_OPCODE_KEY = "OPCODE";

	public final static String SERVICE_BROADCAST_WIFI_EVENT_KEY = "WIFI EVENT";

	public final static String SERVICE_BROADCAST_WIFI_EVENT_FAIL_REASON_KEY = "FAIL KEY";
	public final static String SERVICE_BROADCAST_MSG_CONTENT_KEY = "MSG KEY";

	public final static String FILE_THREAD_WAS_DATA_READ_KEY = "DATA READ";
	public final static String FILE_THREAD_DATA_CONTENT_KEY = "DATA";

	public final static int SERVICE_BROADCAST_OPCODE_ACTION_WIFI_EVENT_VALUE = 1;
	public final static int SERVICE_BROADCAST_OPCODE_ACTION_DO_TOAST = 110;
	public final static int SERVICE_BROADCAST_OPCODE_ACTION_CHAT_ROOM_LIST_CHANGED=113;
	public final static int SERVICE_BROADCAST_OPCODE_JOIN_SENDING_RESULT=115;
	public final static int SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT=116;
	public final static int SERVICE_BROADCAST_OPCODE_ROOM_TIMED_OUT = 118;
	public final static int SERVICE_BROADCAST_WELCOME_SOCKET_CREATE_FAIL = 117;


	//Wifi p2p event values
	public final static int SERVICE_BROADCAST_WIFI_EVENT_P2P_ENABLED = 10;
	public final static int SERVICE_BROADCAST_WIFI_EVENT_P2P_DISABLED = 11;
	public final static int SERVICE_BROADCAST_WIFI_EVENT_PEER_CHANGED = 12;
	public final static int SERVICE_BROADCAST_WIFI_EVENT_PEERS_AVAILABLE = 13;
	public final static int SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_SUCCESS = 14;
	public final static int SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_FAILED = 15;


	@SuppressLint("SimpleDateFormat")
	public static String getTimeString(){//in this format: MM/dd HH:mm
		 Calendar calendar = Calendar.getInstance();
		 SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm");
	    return df.format(calendar.getTime());
	}

	public static Date GetTime() {
		 return Calendar.getInstance().getTime();
	}

	public static String UserListToString(ArrayList<Peer> list) {
		StringBuilder ans = new StringBuilder();
		if (list!=null && list.size()>0) {
			for (Peer user : list) {
				ans.append(user.name + ", ");
			}

		ans.delete(ans.length()-2,ans.length());

		return ans.toString();
		}
		else
			return "";
	}


	public static String SeparateArray2String(String[] input, char separator) {
		int length = input.length;
		StringBuilder buffer = new StringBuilder();

		for (int i=0; i<length ;i++) {
			buffer.append(input[i]);

			if (i<length-1)
				buffer.append(separator);
			else
				buffer.append("\r\n");
		}
		return buffer.toString();
	}


	public static Peer CheckUniqueID(String userUniqueId, ArrayList<Peer> list) {
		if (list==null)
			return null;
		for (Peer user : list) {
			if (user.uniqueID != null && user.uniqueID.equalsIgnoreCase(userUniqueId))
				return user;
		}

		return null;
	}


	public static void chatBalloon(String txt, Activity act) {
		Context context = act.getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, txt, duration);
		toast.show();
	}


    public static void ShowNotification(PendingIntent intent, String message) {
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(ChatSearchScreenFrag.Service)
    	        .setSmallIcon(com.chat.herechat.R.drawable.msg_icon) //set a small icon
    	        .setContentTitle("New messages available:")
    	        .setAutoCancel(true)
    	        .setContentText(message)
    			.setContentIntent(intent);

    	LocalService.notifications.notify(0xdeadbeef, mBuilder.build());
    }
}
