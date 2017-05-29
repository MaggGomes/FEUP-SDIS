package com.chat.herechat.ChatManager;

import com.chat.herechat.LocalService;

import java.util.ArrayList;



public class ChatHandOver {

    public ChatRoomDetails chat = null;
    public LocalService service = null;

    public ChatHandOver(ChatRoomDetails chat, LocalService service, ArrayList<ChatMessage> mListContent) {
        this.chat = chat;
        this.service = service;

        boolean isPassword;
        String password = null;

        if (this.chat.Password == null)
            isPassword = false;
        else {
            isPassword = true;
            password = this.chat.Password;
        }

        ChatSearchScreenFrag.mService.CreateNewHostedPublicChatRoom(this.chat.Name, password, mListContent, this.chat);

    }
}
