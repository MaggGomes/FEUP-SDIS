package com.chat.herechat;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.chat.herechat.Utilities.Constants;

public class PreferencesActivity extends PreferenceActivity {
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource(R.xml.preferences);   
        setupActionBar();
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

    @Override
    protected void onStop() {
    	super.onStop();
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String updatedUserName = sharedPrefs.getString(Constants.SHARED_PREF_USER_NAME, null);
    	if (updatedUserName!=null)
    	MainScreenActivity.UserName = updatedUserName;

    	boolean isToNotify = sharedPrefs.getBoolean(Constants.SHARED_PREF_ENABLE_NOTIFICATION, false); 
    		MainScreenActivity.isToNotifyOnNewMsg = isToNotify;

    	String refreshPeriod = sharedPrefs.getString(Constants.SHARED_PREF_REFRESH_PERIOD, "10000");
    		MainScreenActivity.refreshPeriod = Integer.parseInt(refreshPeriod);

    }
}