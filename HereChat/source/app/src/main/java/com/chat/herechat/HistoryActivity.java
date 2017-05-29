package com.chat.herechat;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.chat.herechat.ChatManager.ChatMessage;
import com.chat.herechat.ChatManager.CustomChatAdapter;
import com.chat.herechat.ServiceHandlers.FileWritter;
import com.chat.herechat.Utilities.Constants;

public class HistoryActivity extends ListActivity {
	private  ArrayList<ChatMessage> mListContent = null;
	private CustomChatAdapter mListAdapter = null;
	Handler mHandler = null;
	ProgressDialog historyLoadDialog;
	
	String mChatRoomID = null;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		
		Bundle extras = getIntent().getExtras();
		mChatRoomID = extras.getString(Constants.HASH_MAP_KEY_SEARCH_FRAG_CHAT_ROOM_UNIQUE); //get the chat room ID from the intent
		
		setupActionBar();
		
		InitAdapter();
		
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if (msg.getData().getBoolean(Constants.FILE_THREAD_WAS_DATA_READ_KEY, false)) //if a history exists
					ParseHistoryFileDataAndUpdateListView((msg.getData().getString(Constants.FILE_THREAD_DATA_CONTENT_KEY, null)));

				if (historyLoadDialog.isShowing())
				historyLoadDialog.dismiss();
			}
		};
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//load the history
		historyLoadDialog = new ProgressDialog(this);
		historyLoadDialog.setTitle("Loading history...");
		historyLoadDialog.setMessage("Please Wait.");
		historyLoadDialog.show();
	
		new FileWritter(mChatRoomID, mHandler, true,this).start();
	}
	
	private void ParseHistoryFileDataAndUpdateListView(String data) {
		String[] parsedHistory = data.split("["+Constants.STANDART_FIELD_SEPERATOR+"]");
		int length = parsedHistory.length;
		
		setTitle(parsedHistory[0]);
		if (parsedHistory[1].equalsIgnoreCase("private"))
			getActionBar().setIcon(R.drawable.private_chat_icon);
		else
			getActionBar().setIcon(R.drawable.public_chat_icon);
		
		
		for (int i = 2; i < length && length > 2 ;i++) {
			String[] parsedSingleMsg = parsedHistory[i].split("["+Constants.CHAT_MSG_ENTRY_SEPARATOR_CHAR+"]");
     		ChatMessage msg = new ChatMessage(parsedSingleMsg[1], parsedSingleMsg[2].replace(Constants.ENTER_REPLACEMENT_CHAR, '\n'),
     				parsedSingleMsg[0], parsedSingleMsg[3], 
     				parsedSingleMsg[1].equalsIgnoreCase(MainScreenActivity.UniqueID));
     		mListContent.add(msg);
		}
		
		mListAdapter.notifyDataSetChanged();
		getListView().setSelection(mListContent.size()-1);
	}

	private void InitAdapter() {
	   if (mListContent == null)
		   mListContent = new ArrayList<ChatMessage>();

	   if (mListAdapter == null) {
		    mListAdapter = new CustomChatAdapter(this,mListContent);
			setListAdapter(mListAdapter);
	   }
	}

	 private void setupActionBar() {
	   getActionBar().setHomeButtonEnabled(true);
	   getActionBar().setDisplayHomeAsUpEnabled(true);
	 }
	 
	 public boolean onOptionsItemSelected(MenuItem item) {
	  switch (item.getItemId()) {
	   case android.R.id.home:
	    NavUtils.navigateUpFromSameTask(this);
	  } 
	  return true;
	 }
}
