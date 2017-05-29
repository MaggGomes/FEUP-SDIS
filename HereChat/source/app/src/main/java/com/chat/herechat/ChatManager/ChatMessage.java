package com.chat.herechat.ChatManager;


public class ChatMessage {

	public String UserUnique;
	public String Message;
	public String UserName;
	public String Time;
	public boolean self;
	public boolean isStatusMessage;


	public ChatMessage(String message, boolean self) {
		super();
		this.Message = message;
		this.self = self;
		this.isStatusMessage = false;
	}

	public ChatMessage(boolean status, String message) {
		super();
		this.Message = message;
		this.self = false;
		this.isStatusMessage = status;
	}

	public ChatMessage(String userUnique,String msg,String userName,String time,boolean isMine){
		this(msg,isMine);
		 UserUnique =userUnique;
		 Time =time;
		 UserName =userName;

	}
	public String getMessage() {return Message;}
	public void setMessage(String message) {this.Message = message;}
	public boolean isSelf() {return self;}
	public String getUserName(){return UserName;}
	public String getTime(){return Time;}

}
