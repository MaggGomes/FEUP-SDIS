package com.chat.herechat.ChatManager;

import com.chat.herechat.LocalService;
import com.chat.herechat.Peer.Peer;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by joliveira on 28/05/2017.
 */

public class ChatHandOver {

    public ChatRoomDetails chat = null;
    public LocalService service = null;
    public Vector<ChatRoomDetails> users = null;

    public ChatHandOver(ChatRoomDetails chat, LocalService service, ArrayList<ChatMessage> mListContent) {
        this.chat = chat;
        this.service = service;
        this.users = service.getChatRooms();

        boolean isPassword;
        String password = null;

        if (this.chat.Password == null)
            isPassword = false;
        else {
            isPassword = true;
            password = this.chat.Password;
        }

        ChatSearchScreenFrag.mService.CreateNewHostedPublicChatRoom(this.chat.Name, password, mListContent, muita gente de guimarãthis.chat);

    }
}
