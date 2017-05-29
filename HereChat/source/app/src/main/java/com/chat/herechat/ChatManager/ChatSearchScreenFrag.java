package com.chat.herechat.ChatManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.Receiver.EnableWifiDirectDialog;
import com.chat.herechat.LocalService;
import com.chat.herechat.LocalService.LocalBinder;
import com.chat.herechat.MainScreenActivity;
import com.chat.herechat.R;


public class ChatSearchScreenFrag extends ListFragment 
{
    private  ArrayList<HashMap<String, String>> mListContent = null;

    public static LocalService Service =null;
	MainScreenActivity Activity;
	boolean stateOfBind =false;

	private  SimpleAdapter Adapter =null;

    public static boolean wifiDirect =false;
    public static boolean groupConnect =false;
    private boolean mIsWasWifiDirectDialogShown=false;

    private EnableWifiDirectDialog WifiEnable = null;
	
	ServiceMsgReceiver ServiceBroadcast = null;
	public static WifiP2pManager Manage =null;
	public static Channel cChannel =null;

	

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
	    Activity = (MainScreenActivity)getActivity(); //set a reference to the activity
	    
	    
		wifiDirect =false;  //reset the wifi flag
	    
		if (ServiceBroadcast ==null) //if the b-cast receiver that works with the service wasn't initialized
		{
			ServiceBroadcast = new ServiceMsgReceiver();
			Activity.registerReceiver(ServiceBroadcast, new IntentFilter(Constants.SERVICE_BROADCAST)); //register
		}
		Activity.SearchFragment = this;
	}//end of onAttach()

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (Manage ==null && cChannel ==null)
		{
			Manage = (WifiP2pManager) Activity.getSystemService(Context.WIFI_P2P_SERVICE); 	//get a wifip2pmanager
		    cChannel = Manage.initialize(Activity, Activity.getMainLooper(), null); 		//get a channel
		}
	}//end of onCreate()
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.activity_chat_search_screen_frag, container, false); //inflate the view
	    //init the adapter and perform a peer search. Changes will be made only if it's the 1st time this fragment is run
	    InitAdapter();
	    	    
		return rootView;
	}//end of onCreateView()
	
	@Override
	public void onStart() 
	{
		super.onStart();
		
        // Bind to LocalService
        Intent intent = new Intent(Activity, LocalService.class);
        Activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE); //async
        
        if (Service !=null) //done for cases of fragment refresh done by the system
        	UpdateListView();
        
		registerForContextMenu(getListView()); //enable context menu
	}//end of onStart()
	
	@Override
	public void onResume() 
	{
	    super.onResume();
		
		if (Service !=null)
			UpdateListView();
		
	}//end of onResume()
	
	@Override
	public void onStop() 
	{
		super.onStop();
		// Unbind from the service
	    if (stateOfBind)
	    	{
	        Activity.unbindService(mConnection);
	        stateOfBind = false;
	    	}
	}//end of onStop()

	

	@Override
		public boolean onContextItemSelected(MenuItem item)
		{
			AdapterContextMenuInfo selectedRow = (AdapterContextMenuInfo) item.getMenuInfo(); //get the current selected item
		
			switch(item.getItemId()) //switch by the selected operation:
			{
				//Context menu choices from the history frag are received here, so we just call the frag to handle this event
				case R.id.action_delete_history_file:
				{
					Activity.HistoryFragment.OnContextMenuItemSelected(item);
					return true;
				}
			}
			return true;
		}//end of onContextItemSelected()


    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
    	super.onListItemClick(l, v, position, id);
    	String uniqueID;
		synchronized (mListContent)
		{
			HashMap<String, String> RoomInfo =  mListContent.get((int)id);
			uniqueID = RoomInfo.get(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE);  //get the selected chat item's unique id
		}
	    Intent intent = new Intent(Activity, ChatActivity.class); //crate a new intent
	    intent.putExtra(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE, uniqueID);   //set the room's unique id as extra.  
	    
	    startActivity(intent);  //send
    }//end of onListItemClick()
    

	private class ServiceMsgReceiver extends BroadcastReceiver 
	{
	@Override
	   public void onReceive(Context context, Intent intent) 
	   {    
	       String action = intent.getAction();
	       if(action.equalsIgnoreCase(Constants.SERVICE_BROADCAST))
	       {    
	    	   Bundle extras = intent.getExtras(); //get extras
	          int opcode =extras.getInt(Constants.SERVICE_BROADCAST_OPCODE_KEY); //get the opcode
			  switch (opcode) 
				{
					case Constants.SERVICE_BROADCAST_OPCODE_ACTION_WIFI_EVENT_VALUE:
						HandleWifiP2pEvent(extras.getInt(Constants.SERVICE_BROADCAST_WIFI_EVENT_KEY),
											extras.getInt(Constants.SERVICE_BROADCAST_WIFI_EVENT_FAIL_REASON_KEY));
						break;
					case Constants.SERVICE_BROADCAST_OPCODE_ACTION_DO_TOAST:
						{
						//show a toast with the received message
				//		LocalService.chatBalloon(extras.getString(Constants.SERVICE_BROADCAST_TOAST_STRING_KEY), activity);
						break;
						}
						
//					case Constants.SERVICE_BROADCAST_OPCODE_ACTION_DO_SHOW_MSG:
//					{
//					//show received message on editText
//						String msg = extras.getString(Constants.SERVICE_BROADCAST_SHOW_MSG_KEY);
//						showMsgOnScreen(msg);
//					
//					break;
//					}
					
					case Constants.SERVICE_BROADCAST_OPCODE_ACTION_CHAT_ROOM_LIST_CHANGED:
					{
					//Update the content of the list view
						UpdateListView();
					break;
					}
					case Constants.SERVICE_BROADCAST_WELCOME_SOCKET_CREATE_FAIL:
					{
						ShowWelcomeSockErrorDialog();
					break;
					}
				
				}//switch
	       }//if
	   }//end of onReceive()
	}//end of ServiceMsgReceiver()
	
	

	private void ShowWelcomeSockErrorDialog()
	{
		new AlertDialog.Builder(Activity)
	    .setTitle("Critical error")
	    .setMessage("Unable to open a welcome socket! Shutting down")
	    .setIcon(R.drawable.alert_icon)
	    //yes button setter
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	    		
			Activity.kill();  //close the entire app
	        
	        }//onClick-Yes
	     })//setPositive
	     .setOnCancelListener(new OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				Activity.kill();  //close the entire app
			}
		})
	     .show();
	}//end of ShowCloseHostedRoomDialog()
	

	private void HandleWifiP2pEvent(int eventCode, int failReasonCode)
	{
	  switch (eventCode) //figure out which wifi p2p event has occurred
		{
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_ENABLED:
			{
                wifiDirect =true;
				break;
			}
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_DISABLED:
			{
				//if the wifi-direct was shutdown by the user in the middle of runtime
				if (wifiDirect ==true)
				{
					kill();
					//we want to relaunch the application:
					Intent intent = new Intent(Activity,MainScreenActivity.class);
					intent.putExtra(Constants.WIFI_BCAST_RCVR_WIFI_OFF_EVENT_INTENT_EXTRA_KEY, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					return;
				}
				wifiDirect =false;
				ShowWifiDirectDialogWhenStarted();
				break;
			}
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_CHANGED:
			{
		//		LocalService.chatBalloon("peers have changed",activity);
				break;
			}
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_PEERS_AVAILABLE:
			{
       //         LocalService.chatBalloon("peers Available",activity);
				break;
			}
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_SUCCESS:
			{
	  //		LocalService.chatBalloon("Peer discovery was successful!",activity);
				break;
			}
			case Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_DISCOVER_FAILED:
			{
				switch(failReasonCode)
				{
				case 0:
					{
			//	    Constants.chatBalloon("Peer discovery failed! Unknown error!", activity);
					break;
					}
				case 1:
					{
					Constants.chatBalloon("Peer discovery failed! Wifi Direct isn't supported by this device!", Activity);
					break;
					}
				case 2:
					{
			//		Constants.chatBalloon("Peer discovery failed! Channel is busy, please wait.", activity);
					break;
					}
				}
			}
		}//switch
	}//end of HandleWifiP2pEvent()
	


	public void onRefreshButtonClicked (View v)
		{
		if (wifiDirect ==false) //wifi direct is disabled
			{
			if (WifiEnable ==null)
				WifiEnable = new EnableWifiDirectDialog();
			
			if (!WifiEnable.isVisible())
				WifiEnable.show(getFragmentManager(),"MyDialog");
			}
		
	    if(stateOfBind && Service !=null)  //wifi direct is enabled
			{
			Service.OnRefreshButtonclicked();
			UpdateListView();
			}
	    
		}//end of onRefreshButtonClick()
	


	public void kill()
	{
		if (Service !=null)
			Service.kill();
		
		if (ServiceBroadcast !=null){
			if(Activity !=null){
				Activity.unregisterReceiver(ServiceBroadcast);  //unregister the receiver only when the app is closed
			}//if
			ServiceBroadcast =null;
		}
		
		Manage.removeGroup(cChannel, null);
	}//end of kill()


	private void InitAdapter()
	{
	   if (mListContent==null){mListContent = new ArrayList< HashMap<String,String>>();} //create a new array list that'll hold all the data
	   if (Adapter ==null)
	   		{
		    Adapter = new SimpleAdapter(Activity,
				    mListContent,
					R.layout.chat_search_list_item,
					new String[]{ Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_NAME,
				   				  Constants.HASH_MAP_KEY_SEARCH_FRAG_PARTICIPANTS,
				   				  Constants.HASH_MAP_KEY_SEARCH_HOSTED_PUBLIC_ROOM_ICON,
				   				  Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON,
				   				  Constants.HASH_MAP_KEY_SEARCH_NEW_MSG_ICON,
				   				  Constants.HASH_MAP_KEY_SEARCH_FRAG_ICON },
					new int[]{R.id.search_list_item_TV1,R.id.search_list_item_TV2, R.id.search_list_item_hosted_icon,
		    		R.id.search_list_item_lock_icon,R.id.search_list_item_new_msg_icon,R.id.search_list_item_icon});  		    
			setListAdapter(Adapter);
		   	}
	}//end of initAdapter()
	

	private void UpdateListView()
	{
		if (Service ==null) //if we don't have a reference to the service yet
			return;
		
		synchronized (mListContent)
		{
			mListContent.clear(); //clear the current list content
			
			if (!Service.hashChatroomActive.isEmpty()) //we want to look up all hosted chat rooms on this device
			{
				Collection<ActiveChatRoom> chatRooms = Service.hashChatroomActive.values();  //get all available hosted chat rooms
				for (ActiveChatRoom room : chatRooms) //for each chat room
				{
					if (room.isPublicHosted) //if this is a hosted group chat
					{
						HashMap<String, String> singleChatEntryView = new HashMap<String, String>(); //create a new hash map
						//set the 1st field to be the user's name
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_NAME, room.roomInfo.name);
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_PARTICIPANTS, Constants.UserListToString(room.roomInfo.users));
						//messing around with the layout's icons:
						if (room.roomInfo.password !=null)
						{
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_HOSTED_PUBLIC_ROOM_ICON, Integer.toString(R.drawable.hosted_icon));
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON, Integer.toString(R.drawable.lock_icon_orange));
						}
						else
						{
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON, Integer.toString(R.drawable.hosted_icon));
						}
						if (room.roomInfo.hasNewMsg)
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_NEW_MSG_ICON, Integer.toString(R.drawable.msg_icon));
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_ICON, Integer.toString(R.drawable.public_chat_icon));
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE, room.roomInfo.roomID);
						
						mListContent.add(singleChatEntryView); //add the hash map to the content list
					}//if hosted public chat
				}//for
			}//if 
			
			if (!Service.hashChatroom.isEmpty()) //if there's a valid discovered chat room list available
			{
				Collection<ChatRoomDetails> chatRooms = Service.hashChatroom.values();  //get all discovered chat rooms
				for (ChatRoomDetails room : chatRooms) //for each chat room
				{
					HashMap<String, String> singleChatEntryView = new HashMap<String, String>(); //create a new hash map
					if (room.isPrivateChatRoom) //if this is a private chat
					{
						//set the 1st field to be the user's name
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_NAME, room.users.get(0).name);
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_PARTICIPANTS, "Private chat");
						//now we'de like to check if this user is ignored
						if (room.hasNewMsg)
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_NEW_MSG_ICON, Integer.toString(R.drawable.msg_icon));
						else
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_ICON, Integer.toString(R.drawable.private_chat_icon));
						
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE, room.roomID);
					}//if
					else //this is a public chat room, NOT hosted by us
					{
						//set the 1st field to be the user's name
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_NAME, room.name);
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_PARTICIPANTS,room.userNamesString);
						if (room.password !=null) //if this room requires a pw
						{
							//if we've connected already to this public room
							if (Service.hashChatroomActive.containsKey(room.roomID))
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON, 
									Integer.toString(R.drawable.lock_icon_green));
							else //we haven't connected yet
								singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON, 
										Integer.toString(R.drawable.lock_icon_red));
						}
						//if we're connected to a public chat which is not hosted by us
						if (Service.hashChatroomActive.containsKey(room.roomID))
								singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_LOCKED_PUBLIC_ROOM_ICON, 
										Integer.toString(R.drawable.plug_icon));					
						if (room.hasNewMsg)
							singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_NEW_MSG_ICON, Integer.toString(R.drawable.msg_icon));
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_ICON, Integer.toString(R.drawable.public_chat_icon));
						singleChatEntryView.put(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE, room.roomID);
					}//else
					mListContent.add(singleChatEntryView); //add the hash map to the content list
				}//for
			}//if 
			
			Adapter.notifyDataSetChanged(); //notify the adapter that that the content has changed
		}//synchronized (listContent)
	}//end of UpdateListView()
	
	

	public void ShowWifiDirectDialogWhenStarted ()
	{
		if (mIsWasWifiDirectDialogShown==false)
		{
			if (WifiEnable ==null)
			WifiEnable = new EnableWifiDirectDialog();
			

			if (!WifiEnable.isVisible())
				WifiEnable.show(getFragmentManager(),"MyDialog");
			
			mIsWasWifiDirectDialogShown=true;
		}
	}
	


    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            Service = binder.getService();
            stateOfBind = true;
            UpdateListView(); //refresh the discovered room view
            ChatSearchScreenFrag.this.Activity.invalidateOptionsMenu();  //force the menu to be rebuilt the next time it's opened
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	stateOfBind = false;
        }
    };
	        
}//end of class
