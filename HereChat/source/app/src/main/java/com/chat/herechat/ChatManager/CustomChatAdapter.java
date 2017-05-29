package com.chat.herechat.ChatManager;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.chat.herechat.R;


public class CustomChatAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<ChatMessage> mMessages;
    private HashMap<String, Integer> mColorsForUsers;
    private int[] mColors = null;
    private final int NUM_OF_COLORS = 16;


    public CustomChatAdapter(Context context, ArrayList<ChatMessage> messages) {
        super();
        this.mContext = context;
        this.mMessages = messages;
        this.mColorsForUsers = new HashMap<String, Integer>();

    }//constructor

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return mMessages.get(position);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = (ChatMessage) this.getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_activity_list_item, parent, false); //get the xml layout
            holder.mMessage = (TextView) convertView.findViewById(R.id.message_text);
            holder.mTimeAndUserName = (TextView) convertView.findViewById(R.id.message_time_and_userName);

            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.mMessage.setText(message.getMessage());
        holder.mTimeAndUserName.setText("   " + message.getTime() + "  " + message.getUserName() + "   ");

        holder.mTimeAndUserName.setTextColor(mContext.getResources().getColor(R.color.White));
        holder.mTimeAndUserName.setTextSize(14);

        LayoutParams lp = (LayoutParams) holder.mMessage.getLayoutParams();
        LayoutParams lp2 = (LayoutParams) holder.mTimeAndUserName.getLayoutParams();

        //Check whether message is mine to show green background and align to right
        if (message.isSelf()) {
            holder.mMessage.setBackgroundResource(R.drawable.speech_bubble_green);
            lp.gravity = Gravity.RIGHT;
            lp2.gravity = Gravity.RIGHT;
            holder.mMessage.setTextColor(mContext.getResources().getColor(R.color.Black));

        }

        //If not mine then it is from sender to show orange background and align to left
        else {
            holder.mMessage.setBackgroundResource(R.drawable.speech_bubble_orange);

            //sets color to the MSG
            holder.mMessage.setTextColor(mContext.getResources().getColor(R.color.primaryColor));

            lp.gravity = Gravity.LEFT;
            lp2.gravity = Gravity.LEFT;

        }
        holder.mMessage.setLayoutParams(lp);


        holder.mTimeAndUserName.setLayoutParams(lp2);

        return convertView;
    }

    private static class ViewHolder {
        TextView mMessage;
        TextView mTimeAndUserName;

    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

}//Class