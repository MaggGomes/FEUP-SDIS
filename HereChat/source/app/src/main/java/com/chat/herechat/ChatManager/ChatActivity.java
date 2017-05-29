package com.chat.herechat.ChatManager;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;

import com.chat.herechat.FileExplorerActivity;
import com.chat.herechat.ServiceHandlers.FileWritter;
import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.LocalService;
import com.chat.herechat.MainScreenActivity;
import com.chat.herechat.PreferencesActivity;
import com.chat.herechat.R;


public class ChatActivity extends ListActivity {
	ChatRoomDetails chatRoomInfo =null;
	private  ArrayList<ChatMessage> listContent = null;
	private ChatAdapter listAdapter =null;
	ServiceMsgReceiver serviceBroadcastReceiver = null;
	LocalService service = ChatSearchScreenFrag.Service;
	Handler handler =null;
	ProgressDialog historyLoadDialog, peerConnectDialog;
	String roomNameAsWrittenInHistory =null;
	public static boolean isActive =false;
	public static ArrayList<ChatMessage> msgsWaitingForSendResult =null;
	private boolean isChatRoomHosted = false;
	AlertDialog dialog = null;
	boolean isTimedOut = false;
	private final int Handler_WHAT_valueForPeerConnect = 10;
    static final int PICK_CONTACT_REQUEST = 1;


	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		service = ChatSearchScreenFrag.Service; //get a reference to the service
		Bundle extras = getIntent().getExtras();
		String ChatRoomID = extras.getString(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE); //get the chat room ID from the intent
		this.listContent = this.service.listContent;

		if(chatRoomInfo ==null)
			chatRoomInfo = service.hashChatroom.get(ChatRoomID);  //get the room's info if it's not a hosted chat room

		if(chatRoomInfo ==null) //if the info is still null, it means that this room is a hosted room
		{
			chatRoomInfo = service.hashChatroomActive.get(ChatRoomID).roomInfo;  //get the room's info if it's a hosted chat room
			isActive =true;
		}

		setupActionBar();

		if(msgsWaitingForSendResult ==null)
			msgsWaitingForSendResult = new ArrayList<ChatMessage>();

		serviceBroadcastReceiver = new ServiceMsgReceiver(this);   //create a new b-cast receiver for handling service events
		registerReceiver(serviceBroadcastReceiver, new IntentFilter(Constants.SERVICE_BROADCAST)); //register

		initAdapter();

		handler = new Handler(){ //define a new message handler for the file thread
			//Here we'll receive the content of the history file that was read by a thread
			@Override
			public void handleMessage(Message msg) {
				//this is a TO on a connection attempt
				if (msg.what==Handler_WHAT_valueForPeerConnect)
				{
					 //if we weren't able to connect yet
					if (!isActive)
					{
						try
						{
						ChatActivity.this.dismissDialog(peerConnectDialog);
						ChatActivity.this.showSingleButtonDialogFinishActivity("Peer unresponsive!",
								"Unable to establish communication with the target peer! closing.");
						}
						catch (Exception e) {

						}
					}
				}
				else
				{
					//parse the data and update the list view
					if (msg.getData().getBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false)) //if a history exists
						parseHistoryFileDataUpdateListView((msg.getData().getString(Constants.FILE_THREAD_DATA_CONTENT_KEY, null)));
					else
						initHistoryFile(null, chatRoomInfo.roomID, chatRoomInfo.name, getApplication(), chatRoomInfo.isPrivateChatRoom);       //write 2 required lines at the beginning of the file
					//dismiss the progress dialog
					if (historyLoadDialog.isShowing())
					historyLoadDialog.dismiss();
				}
			}
		};

	}//end of onCreate()



	@Override
	protected void onResume()
	{
		super.onResume();
		//load the history
		historyLoadDialog = new ProgressDialog(this);
		historyLoadDialog.setTitle("Loading history...");
		historyLoadDialog.setMessage("Please Wait.");
		historyLoadDialog.show();

		new FileWritter(chatRoomInfo.roomID, handler, true,this).start(); //launch the history file reader

		service.activeChat =true;   //mark that the chat activity is active
		service.DisplayedAtChatActivity= chatRoomInfo;  //set the details of the displayed room

		 //set the window's title to be the chat's name:
		if (chatRoomInfo.isPrivateChatRoom)
			setTitle(chatRoomInfo.users.get(0).name);
		else
			setTitle(chatRoomInfo.name);

		//set icon
		if (chatRoomInfo.isPrivateChatRoom)
			getActionBar().setIcon(R.drawable.private_chat_icon);
		else
			getActionBar().setIcon(R.drawable.public_chat_icon);

		isChatRoomHosted = findHostedChatRoom();

		if (!isChatRoomHosted) //if this isn't a hosted room
		{
			peerConnectDialog = new ProgressDialog(this);
			peerConnectDialog.setTitle("Approving with peer...");
			peerConnectDialog.setMessage("Please Wait.");
			peerConnectDialog.show();

			//if this is a public chat room, requires a password and inactive:
			if (!chatRoomInfo.isPrivateChatRoom && chatRoomInfo.password !=null && service.hashChatroomActive.get(chatRoomInfo.roomID)==null)
				showPasswordDialogForPublicChat(true); //show the password request dialog
			else
			//Now try and establish a handshake with the peer:
			service.EstablishChatConnection(chatRoomInfo.roomID, null, chatRoomInfo.isPrivateChatRoom); //try and establish a connection
			handler.sendEmptyMessageDelayed(Handler_WHAT_valueForPeerConnect, 3000); //set a TO in 3 seconds from now
		}
		else //this is a hosted public chat room
		{
			isActive =true;                        //this room is always active
			registerForContextMenu(getListView()); //register this activity for context menu
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu.
		getMenuInflater().inflate(R.menu.chat_prv_room_menu, menu);
		return true;
	}//end of onCreateOptionsMenu()



	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
        menu.clear(); //Clear view of previous menu
        MenuInflater inflater = getMenuInflater();
        //switch between 2 different menus, according to if it's a private chat or not
        if(chatRoomInfo.isPrivateChatRoom)
        {
            inflater.inflate(R.menu.chat_prv_room_menu, menu);
        }
        else //this is a public chat room
            inflater.inflate(R.menu.chat_pub_room_menu, menu);

        return true;

	}//end of onPrepareOptionsMenu()

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		 super.onOptionsItemSelected(item);
		 switch (item.getItemId())
			{
				case R.id.action_settings://setting was clicked
				{
					startActivity(new Intent(this, PreferencesActivity.class));
					break;
				}
				case R.id.action_clear_view: //clear view was clicked
				{
					listContent.clear();
					listAdapter.notifyDataSetChanged();
					break;
				}
				case R.id.action_close_room: //close chat room was clicked
				{
					if (service.hashChatroomActive.get(chatRoomInfo.roomID).isPublicHosted) //trying to close a hosted public chat
						{
						showClosedHostedRoomDialog(); //the dialog will call the service and close this room if necessary
						}
					else   //this chat room isn't hosted. send disconnection control message and close
						{
						service.CloseNotHostedPublicChatRoom(chatRoomInfo);
						finish();
						}
					break;
				}
				case android.R.id.home:
				{
					   NavUtils.navigateUpFromSameTask(this);
					   break;
				}
			}//switch

		 return true;
	}//end of onOptionsItemSelected()

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
	    AdapterContextMenuInfo selectedRow = (AdapterContextMenuInfo) menuInfo; //get the current selected item
	    ChatMessage message = listContent.get(selectedRow.position);
	    if (!message.self)
	    {
			if (isChatRoomHosted) //we need a context menu only for a hosted chat room
			{
		        MenuInflater inflater = getMenuInflater();
		        inflater.inflate(R.menu.chat_activity_hosted_room_context_menu, menu);
			}
	    }
	}//end of onCreateContextMenu



	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
	  super.onMenuItemSelected(featureId, item);
		 switch (item.getItemId())
		{
			case R.id.action_clear_view://clear list view was clicked
			{
				listContent.clear();
				listAdapter.notifyDataSetChanged();
				break;
			}
			case R.id.action_close_room://close room was clicked
			{
				if (isChatRoomHosted)
				{
					showClosedHostedRoomDialog();
				}
				else
				{
					service.CloseNotHostedPublicChatRoom(chatRoomInfo);
				}
				break;
			}
		}

		 return true;
	}//end of onMenuItemSelected()


	private void showClosedHostedRoomDialog()
	{
		new AlertDialog.Builder(this)
	    .setTitle("Closing hosted public chat")
	    .setIcon(R.drawable.alert_icon)
	    .setMessage("You are the host of this chat room. Closing it will pass the host to other peer. Close anyway?")

	    //yes button setter
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {

			service.CloseHostedPublicChatRoom(chatRoomInfo);
			finish();

	        }//onClick-Yes
	     })//setPositive

	     //no button setter
	     .setNegativeButton("No", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        		//do nothing.
	        }//onClick-No
	     })//setNegative
	     .show();

	}




	private void showRoomTimedOutDialog()
	{
		new AlertDialog.Builder(this)
	    .setTitle("Chat has timed out!")
	    .setIcon(R.drawable.alert_icon)
	    .setMessage("Lost connection with peer. This chat is no longer available.")

	    //yes button setter
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        }//onClick-Yes
	     })//setPositive
	     .show();

	}



	private void showSingleButtonDialogFinishActivity(String title, String message)
	{
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setIcon(R.drawable.alert_icon)
				.setMessage(message)

				//yes button setter
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						finish();  //close the activity

					}//onClick-Yes
				})//setPositive
				.setOnCancelListener(new OnCancelListener()
				{

					@Override
					public void onCancel(DialogInterface dialog)
					{
						finish();

					}
				})
				.show();

	}


	private void showSingleButtonDialogAndAssumeHost()
	{
		new AlertDialog.Builder(this)
				.setTitle("Host the chat?")
				.setIcon(R.drawable.alert_icon)
				.setMessage("You'll be the new host!")

				//yes button setter
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//ASSUME O HOST DO CHAT
						new ChatHandOver(chatRoomInfo, service, listContent);
						finish();

					}//onClick-Yes
				})//setPositive
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						finish();  //close the activity

					}//onClick-Yes
				})
				.setOnCancelListener(new OnCancelListener()
				{

					@Override
					public void onCancel(DialogInterface dialog)
					{
						finish();

					}
				})
				.show();

	}


	public static void initHistoryFile(Handler handler, String unique, String name, Context con, boolean isPrivate)
	{
		StringBuilder data = new StringBuilder();
		data.append(name+"\r\n");
		if (isPrivate)
			data.append("private"+"\r\n");
		else
			data.append("public"+"\r\n");

		FileWritter fh = new FileWritter(unique, handler, false, con); //create a new file handler
		fh.UpdateDataToWriteBuffer(data.toString());  //set the data to write
		fh.start(); //run the thread
		fh.Kill();  //exit the thread gracefully

	}//end of InitHistoryFile()


	private void parseHistoryFileDataUpdateListView(String data)
	{
		String[] parsedHistory = data.split("["+Constants.STANDART_FIELD_SEPERATOR+"]"); //parse the string by the separator char
		int length = parsedHistory.length;

		//get the room's name as it's written in the history file:
		roomNameAsWrittenInHistory = parsedHistory[0];

		for (int i=2; i<length ;i++) //for each msg string
		{
			String[] parsedSingleMsg = parsedHistory[i].split("["+Constants.CHAT_MSG_ENTRY_SEPARATOR_CHAR+"]"); //parse by the inner separator
     		ChatMessage msg = new ChatMessage(parsedSingleMsg[1], parsedSingleMsg[2].replace(Constants.ENTER_REPLACEMENT_CHAR, '\n')
     				, parsedSingleMsg[0], parsedSingleMsg[3],
     				parsedSingleMsg[1].equalsIgnoreCase(MainScreenActivity.UniqueID));
     		listContent.add(msg);
		}//for

		listAdapter.notifyDataSetChanged();
		getListView().setSelection(listContent.size()-1);

	}//end of ParseHistoryFileDataAndUpdateListView()


	private class ServiceMsgReceiver extends BroadcastReceiver
	{
		Activity activity = null;

		public ServiceMsgReceiver(Activity act)
		{
			super();
			activity =act;
		}

	@Override
	   public void onReceive(Context context, Intent intent)
	   {
	       String action = intent.getAction();
	       if(action.equalsIgnoreCase(Constants.SERVICE_BROADCAST))
	       {
	    	   Bundle extras = intent.getExtras(); //get extras
	    	   String msgDst = extras.getString(Constants.SINGLE_SEND_THREAD_KEY_UNIQUE_ROOM_ID);
	    	   //if the broadcast isn't targeted for this room:
	    	   if (msgDst==null || !msgDst.equalsIgnoreCase(ChatActivity.this.chatRoomInfo.roomID))
	    		   return;

	          int opcode =extras.getInt(Constants.SERVICE_BROADCAST_OPCODE_KEY); //get the opcode
			  switch (opcode)
				{
			  		//a result for a send attempt is received (simply indicating if the message was sent via socket or the socket has crashed):
					case Constants.SERVICE_BROADCAST_OPCODE_JOIN_SENDING_RESULT:
						{
							//if the send has failed, no connection could be established. notify and block the user
							if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT).equals(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED))
							{
								dismissDialog(peerConnectDialog);
								Constants.chatBalloon("SEND FAILED! SOCK CRASHED!", activity);

								if (isActive) //if a send failed after this chat is active, it means that a message has failed to be sent
								{
									if (!msgsWaitingForSendResult.isEmpty())
										msgsWaitingForSendResult.remove(0);   //remove from the msg stack
								}


								if (chatRoomInfo.password !=null && !isActive) //if we've failed to send a join request to a pw protected chat room
								{
									showSingleButtonDialogFinishActivity("Unable to establish connection!",
											"Failed while trying to contact the host.");
								}
							}
							else //A positive physical send result
							{
								if (msgsWaitingForSendResult.size()!=0 && !isChatRoomHosted) //if we've successfully sent a message
								{
									ChatMessage msg = msgsWaitingForSendResult.get(0);  //get the msg

									addMessage(msg);  //add to list view
									if (!msgsWaitingForSendResult.isEmpty())
										msgsWaitingForSendResult.remove(0);   //remove from the msg stack

									ActiveChatRoom room = service.hashChatroomActive.get(chatRoomInfo.roomID); //get the active chat room
									writeMessageInHistoryFile(room, msg.Message.replace('\n',Constants.ENTER_REPLACEMENT_CHAR)); //write this message to the file
								}//if
							}
						break;
						}
					//a result for a connection request has come, or a denial reply was received
					case Constants.SERVICE_BROADCAST_OPCODE_JOIN_REPLY_RESULT:
						{
							//if the connection was denied:
							if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT).equals(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_FAILED))
							{
								dismissDialog(peerConnectDialog);
								isActive =false;
								//
								if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_REASON).equalsIgnoreCase(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_WRONG_PW))
								{
									isActive = false;
									peerConnectDialog.show();
									showPasswordDialogForPublicChat(false);
								}
								if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_REASON).equalsIgnoreCase(Constants.SERVICE_NEGATIVE_REPLY_FOR_JOIN_REQUEST_REASON_NON_EXITISING_ROOM))
								{
                                    showSingleButtonDialogAndAssumeHost();
									service.RemoveFromDiscoveredChatRooms(msgDst); //msgDst is the room's unique
								}
								if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_REASON).equalsIgnoreCase(Constants.SERVICE_NEGATIVE_REPLY_REASON_ROOM_CLOSED))
								{
									showSingleButtonDialogAndAssumeHost();
									service.RemoveFromDiscoveredChatRooms(msgDst); //msgDst is the room's unique
								}

							}
						    //if the connection was approved:
							if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT).equals(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_SUCCESS))
							{
								dismissDialog(peerConnectDialog);
								isActive =true;
						//		Constants.chatBalloon("JOIN SUCCEEDED! PEER HAS ACCEPTED", activity);
							}
						    //if the connection already exists
							if (extras.getString(Constants.SINGLE_SEND_THREAD_KEY_RESULT).equals(Constants.SINGLE_SEND_THREAD_ACTION_RESULT_ALREADY_CONNECTED))
							{
								dismissDialog(peerConnectDialog);
								isActive =true;
						//		Constants.chatBalloon("CHAT ROOM IS ALREADY CONNECTED!", activity);
							}
						break;
						}

					case Constants.CONNECTION_CODE_NEW_CHAT_MSG: //a new chat message has arrived
						{
							String[] content = extras.getStringArray(Constants.SERVICE_BROADCAST_MSG_CONTENT_KEY);
							//create a ChatMessage from the received socket-string
							ChatMessage msg = new ChatMessage(content[2],
									content[4].replace(Constants.ENTER_REPLACEMENT_CHAR,'\n'),  //convert the 'enter's back
									content[1], Constants.getTimeString(), false);
							addMessage(msg); //add to the list view

							if (ChatActivity.this.chatRoomInfo.isPrivateChatRoom) //if this is a private chat room
							{
								//update the title with the peer's currant name
								ChatActivity.this.setTitle(ChatActivity.this.chatRoomInfo.users.get(0).name);
							}
						break;
						}

					case Constants.SERVICE_BROADCAST_OPCODE_ROOM_TIMED_OUT: //the displayed room has timed out
					{

					if (isTimedOut ==false)
					{
						isTimedOut =true;
						showRoomTimedOutDialog();
						disableButtonEditText();
						isActive =false;
						break;
					}
					}

				}//switch
	       }//if
	   }
	}

	private void disableButtonEditText()
	{
		EditText text = (EditText) findViewById(R.id.MsgText);
		text.setText("");  //clear the user's edit-text
		text.setEnabled(false);  //disable the edit-text

		Button button = (Button) findViewById(R.id.chat_activity_send_button);
		button.setEnabled(false);  //disable the button
	}


	private void writeMessageInHistoryFile(ActiveChatRoom room, String msg)
	{
		String[] temp;

		if (room!=null)
		{
		    temp = new String[4];
			temp[0] = MainScreenActivity.UserName;
			temp[1] = MainScreenActivity.UniqueID;
			temp[2] = msg;
			temp[3] = Constants.getTimeString();

			room.UpdateFileWithNewMessage( Constants.SeparateArray2String(temp, Constants.CHAT_MSG_ENTRY_SEPARATOR_CHAR));
		}//if
	}


	private void dismissDialog(Dialog d)
	{
		if (d!=null && d.isShowing())
			d.dismiss();
	}


	protected void initAdapter()
	{
	   if (listContent ==null){
		   listContent = new ArrayList<ChatMessage>();} //create a new array list that'll hold all the data
	   if (listAdapter ==null)
	   		{
		    listAdapter = new ChatAdapter(this, listContent);  //create a new adapter
			setListAdapter(listAdapter);   						//set the content
		   	}
	}//end of initAdapter()

	@Override
	protected void onPause()
	{
		super.onPause();
		service.DisplayedAtChatActivity=null;

		if (isTimedOut)
			service.RemoveSingleTimedOutRoom(chatRoomInfo, false); //remove this room from the service

		service.activeChat =false; //mark that this activity is no longer active
		chatRoomInfo.hasNewMsg=false;  //lower the messages flag
		//if this is a private chat room and the peer has changed his name:
		if (chatRoomInfo.isPrivateChatRoom &&  !chatRoomInfo.users.get(0).name.equalsIgnoreCase(roomNameAsWrittenInHistory))
		{
			service.UpdatePrivateChatRoomNameInHistoryFile(chatRoomInfo);
		}
	}//end of onPause()

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (serviceBroadcastReceiver !=null) //unregister receiver
		unregisterReceiver(serviceBroadcastReceiver);
	}//end of onDestroy()


	public void onSendButtonClicked(View v)
	{
		EditText text = (EditText) findViewById(R.id.MsgText);
		String newMessage = text.getText().toString().trim();  //get the user's text msg
		if(newMessage.length() > 0) //if this is a valid message
		{
			text.setText("");  //clear the user's edit-text
			ChatMessage m = new ChatMessage(MainScreenActivity.UniqueID,newMessage,MainScreenActivity.UserName,
					Constants.getTimeString(),true);
			if (!isChatRoomHosted) //if this isn't a hosted public chat
			{
				//add the msg to the head of the waiting for result list
				if(msgsWaitingForSendResult.size()!=0)
					msgsWaitingForSendResult.add(0,m);
				else
					msgsWaitingForSendResult.add(m);
			}
			else //this is a hosted public chat. We don't queue up messages. We assume that they're always successfully sent
			{
				addMessage(m);  //add to list view
				ActiveChatRoom room = service.hashChatroomActive.get(chatRoomInfo.roomID); //get the active chat room
				//replace all '\n' chars and write this message to the file
				writeMessageInHistoryFile(room, m.Message.replace('\n', Constants.ENTER_REPLACEMENT_CHAR));
			}
			//replace all 'enter's in the text with our special char, since 'enter's will fuck our logic up.
			newMessage = newMessage.replace('\n',Constants.ENTER_REPLACEMENT_CHAR);

			service.SendMessage(newMessage, chatRoomInfo.roomID); //call the service to send the message to the peer
		}//if
	}//end of onSendButtonClicked()


    public boolean onFileButtonClicked(View v)
    {
        Intent chooseFileIntent = new Intent(this, FileExplorerActivity.class);
        startActivityForResult(chooseFileIntent,PICK_CONTACT_REQUEST);
        return true;
    }

	private boolean findHostedChatRoom()
	{
		ActiveChatRoom room = service.hashChatroomActive.get(chatRoomInfo.roomID);

		return (room!=null && room.isPublicHosted);
	}//end of findHostedChatRoom()


	private void addMessage(ChatMessage m)
	{
		listContent.add(m);      				//add the new message
		listAdapter.notifyDataSetChanged();   //update the list view
		getListView().setSelection(listContent.size()-1); //scroll down so that the new message will be visible
	}//end of addMessage()


	private void showPasswordDialogForPublicChat(boolean isFirstTry)
	{
        // adding custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        dialog = new AlertDialog.Builder(this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(isFirstTry? "This room requires a password" : "Wrong password! try again")
            .setView(textEntryView)
            .setIcon(R.drawable.key_icon)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                EditText ed = (EditText) ChatActivity.this.dialog.findViewById(R.id.password_edit);
                String pw = ed.getText().toString();  //get the pw
                service.EstablishChatConnection(chatRoomInfo.roomID, pw, chatRoomInfo.isPrivateChatRoom); //try and establish a connection
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {


                	ChatActivity.this.finish();  //the user chose not to enter a password. close the activity
                }
            })
            .setOnCancelListener(new OnCancelListener()
    		{

    			@Override
    			public void onCancel(DialogInterface dialog)
    			{
    				ChatActivity.this.finish();  //the user chose not to enter a password. close the activity
    			}
    		})
            .create();
        dialog.show();
    }//end of ShowPasswordRequestDialog()

	 private void setupActionBar()
	 {
	  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	  {
	   // enables the activity icon as a 'home' button. required if "android:targetSdkVersion" > 14
	   getActionBar().setHomeButtonEnabled(true);
	   getActionBar().setDisplayHomeAsUpEnabled(true);
	  }
	 }

}//end of class
