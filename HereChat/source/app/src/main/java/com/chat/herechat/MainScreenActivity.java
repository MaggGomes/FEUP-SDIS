package com.chat.herechat;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;

import com.chat.herechat.ChatManager.ChatActivity;
import com.chat.herechat.ChatManager.ChatHistoryScreenFrag;
import com.chat.herechat.ChatManager.ChatSearchScreenFrag;
import com.chat.herechat.Utilities.Constants;

public class MainScreenActivity extends FragmentActivity implements ActionBar.TabListener {
    private AlertDialog mDialog = null;
    private OnClickListener AlertCheckBoxClickListener = null;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    public ChatHistoryScreenFrag mHistoryFrag = null;
    public ChatSearchScreenFrag mSearchFrag = null;

    boolean isServiceStarted = false;
    boolean wasWifiDialogShown = false;

    static int mDisplayedFragIndex = 0;
    public static long ChatRoomAccumulatingSerialNumber = 0;
    public static String UniqueID = null;
    public static String UserName = ":>~";
    static boolean isToNotifyOnNewMsg = false;
    static int refreshPeriod = 40000;

    private boolean mIsRunForTheFirstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        InitializeTabHandlerAndAdapter();

        if (!isServiceStarted && ChatSearchScreenFrag.mService == null) {
            startService(new Intent(this, LocalService.class));
            isServiceStarted = true;
        }

        getPrefs();  //get the shared prefs

        //Happens only when the app is run for the very 1st time on a device
        if (MainScreenActivity.UniqueID == null) {
            UserName = new String(Secure.getString(getContentResolver(), Secure.ANDROID_ID)); //get a unique id
            UniqueID = new String(UserName);
        }

        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isToDisplayWifiDialog = getIntent()
                .getBooleanExtra(Constants.WIFI_BCAST_RCVR_WIFI_OFF_EVENT_INTENT_EXTRA_KEY, false);

        if (isToDisplayWifiDialog && !wasWifiDialogShown) {
            new EnableWifiDirectDialog().show(getSupportFragmentManager(), "MyDialog");
            wasWifiDialogShown = true;
        }

        if (mIsRunForTheFirstTime) {
            //launch the preferences activity
            startActivity(new Intent(this, QuickPrefsActivity.class));
            mIsRunForTheFirstTime = false;
        }
    }

    private void InitializeTabHandlerAndAdapter() {
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            //if a tab was changed by a swipe gesture
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position); //update the tab bar to match the selected page
                mDisplayedFragIndex = position;   //update the index of the currently displayed frag
                if (position == 1) {
                    mHistoryFrag.loadHistory();
                }
                invalidateOptionsMenu();
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.getItem(0).setEnabled(ChatSearchScreenFrag.mIsWifiDirectEnabled);

        //if this menu is opened when the chat search is active
        if (mDisplayedFragIndex == 0) {
            //hide the delete history option
            menu.findItem(R.id.action_delete_all_history).setVisible(false);
        } else {
            //show the delete history option
            menu.findItem(R.id.action_delete_all_history).setVisible(true);
        }

        return true;
    }

    @SuppressLint("HandlerLeak")
    Handler FirstTimeMenuUpdater = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MainScreenActivity.this.invalidateOptionsMenu();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_screen_menu, menu);

        FirstTimeMenuUpdater.sendEmptyMessageDelayed(0, 500);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startActivity(new Intent(this, QuickPrefsActivity.class));
                break;
            }

            case R.id.action_create_new_chat_room: {
                mDialog = CreatePublicChatCreationDialog();
                mDialog.show();

                AlertCheckBoxClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        AlertDialog dialog = MainScreenActivity.this.mDialog;
                        EditText ed = (EditText) dialog.findViewById(R.id.choosePassword);
                        boolean b = !ed.isEnabled();
                        ed.setEnabled(b);
                    }
                };

                CheckBox ch = (CheckBox) mDialog.findViewById(R.id.checkBoxSetPassword);
                ch.setOnClickListener(AlertCheckBoxClickListener);
                break;
            }

            case R.id.action_exit: {
                kill();
                break;
            }

            case R.id.action_delete_all_history: {
                mHistoryFrag.DeleteAllHistory();
                break;
            }
        }

        return true;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;

            switch (position) {
                case 0:
                    fragment = new ChatSearchScreenFrag();
                    break;
                case 1:
                    fragment = new ChatHistoryScreenFrag();
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.main_screen_tab1_title).toUpperCase(l);
                case 1:
                    return getString(R.string.main_screen_tab2_title).toUpperCase(l);
            }
            return null;
        }
    }

    public void onRefreshButtonClicked(View v) {
        mSearchFrag.onRefreshButtonClicked(v); //call the frag's method
    }


    protected void getPrefs() {
        SharedPreferences prefs = getPreferences(0);
        ChatRoomAccumulatingSerialNumber = prefs.getLong(Constants.SHARED_PREF_CHAT_ROOM_SERIAL_NUM, 0);
        UserName = prefs.getString(Constants.SHARED_PREF_USER_NAME, null);
        UniqueID = prefs.getString(Constants.SHARED_PREF_UNIQUE_ID, null);
        isToNotifyOnNewMsg = prefs.getBoolean(Constants.SHARED_PREF_ENABLE_NOTIFICATION, false);
        refreshPeriod = prefs.getInt(Constants.SHARED_PREF_REFRESH_PERIOD, 10000);
        mIsRunForTheFirstTime = prefs.getBoolean(Constants.SHARED_PREF_IS_FIRST_RUN, true);
    }


    protected void savePrefs() {
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putLong(Constants.SHARED_PREF_CHAT_ROOM_SERIAL_NUM, ChatRoomAccumulatingSerialNumber); //save to current SN
        editor.putString(Constants.SHARED_PREF_USER_NAME, UserName);
        editor.putString(Constants.SHARED_PREF_UNIQUE_ID, UniqueID);
        editor.putBoolean(Constants.SHARED_PREF_ENABLE_NOTIFICATION, isToNotifyOnNewMsg);
        editor.putInt(Constants.SHARED_PREF_REFRESH_PERIOD, refreshPeriod);
        editor.putBoolean(Constants.SHARED_PREF_IS_FIRST_RUN, false);
        editor.commit();
    }

    public void kill() {
        savePrefs();
        mSearchFrag.kill();

        ChatActivity.mIsActive = false;
        ChatActivity.mMsgsWaitingForSendResult = null;
        ChatSearchScreenFrag.mService = null;
        ChatSearchScreenFrag.mIsWifiDirectEnabled = false;
        ChatSearchScreenFrag.mIsConnectedToGroup = false;
        ChatSearchScreenFrag.mManager = null;
        ChatSearchScreenFrag.mChannel = null;
        LocalService.mNotificationManager = null;

        System.gc();
        finish();
    }

    private AlertDialog CreatePublicChatCreationDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.public_chat_creation_dialog, null);
        return new AlertDialog.Builder(this)

                .setTitle("Create A New Room")
                .setView(textEntryView)
                .setIcon(R.drawable.settings_icon)

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        boolean isPassword = false;
                        String password = "";
                        String roomName = null;

                        EditText ed = (EditText) mDialog.findViewById(R.id.choosePassword);

                        //gets password if exists
                        isPassword = ed.isEnabled();
                        if (isPassword) {
                            password = ed.getText().toString();
                        }

                        //gets rooms name
                        ed = (EditText) mDialog.findViewById(R.id.chooseRoomsName);
                        roomName = ed.getText().toString();

                        //if the room's name is invalid:
                        if (roomName == null || roomName.length() < 1) {
                            // pop alert dialog and reload this dialog
                            new AlertDialog.Builder(MainScreenActivity.this)
                                    .setTitle("Missing name error")
                                    .setMessage("A room must have a name")

                                    //yes button setter
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            mDialog.show();
                                        }
                                    })//setPositive

                                    .setOnCancelListener(new OnCancelListener() {
                                        public void onCancel(DialogInterface dialog) {
                                            mDialog.show();
                                        }
                                    })

                                    .show();
                        }

                        else {
                            if (password.equalsIgnoreCase(""))
                                password = null;

                            ChatSearchScreenFrag.mService.CreateNewHostedPublicChatRoom(roomName, password);

                        }
                    }//onClick dialog listener


                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }

                }).create();
    }
}
