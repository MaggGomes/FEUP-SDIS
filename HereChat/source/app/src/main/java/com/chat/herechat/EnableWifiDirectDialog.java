package com.chat.herechat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;

public class EnableWifiDirectDialog extends DialogFragment {
    Context context;
	 
    public EnableWifiDirectDialog() {
        context = getActivity();
    }
	    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("WiFi Direct disabled." +
        		"\r\nWould you like to turn it on now?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   @Override
				public void onClick(DialogInterface dialog, int id) {
                   startActivity(new Intent(Settings.ACTION_SETTINGS));
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   @Override
				public void onClick(DialogInterface dialog, int id) {

                   }
               });
        

        return builder.create();
    }
}