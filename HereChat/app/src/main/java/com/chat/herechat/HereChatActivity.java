
package com.chat.herechat;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chat.herechat.Chat.ChatFragment;
import com.chat.herechat.Chat.ChatManager;
import com.chat.herechat.Receiver.WiFiDirectBroadcastReceiver;
import com.chat.herechat.SocketHandlers.ClientSocketHandler;
import com.chat.herechat.SocketHandlers.ServerSocketHandler;
import com.chat.herechat.WiFiDirectServicesList.WiFiDevicesAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HereChatActivity extends Activity implements Handler.Callback, ConnectionInfoListener {

    public static final String TAG = "herechat";

    public static final String AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_herechat";
    public static final String SERVICE_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    public static final int TIMEOUT =  5000;
    public static final int SERVER_PORT = 4545;

    private IntentFilter intentFilter;
    private Channel channel;
    private BroadcastReceiver receiver;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    private Handler handler = new Handler(this);
    private ChatFragment chatFragment;
    private WiFiDirectServicesList servicesList;

    private TextView statusTxtView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        statusTxtView = (TextView) findViewById(R.id.status_text);

        init();
    }

    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }
            });
        }

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void init(){
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();

        servicesList = new WiFiDirectServicesList();
        getFragmentManager().beginTransaction().add(R.id.container_root, servicesList, "services").commit();
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<>();
        record.put(AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_TYPE, record);

        manager.addLocalService(channel, service, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();
    }

    private void discoverService() {
        manager.setDnsSdResponseListeners(channel, new DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                        /* Verifies if the service discovered is the same app */
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");

                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                            }
                        }
                    }
                }, new DnsSdTxtRecordListener() {
                    @Override
                    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> record, WifiP2pDevice device) {
                        Log.d(TAG, device.deviceName + " is " + record.get(AVAILABLE));
                    }
                });

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG, "Failed adding service discovery request");
                    }
                });

        /* Started looking for other peer devices supporting the same service */
        manager.discoverServices(channel,new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery failed");
            }
        });
    }

    public void connectP2p(WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                (chatFragment).pushMessage("Buddy: " + readMessage);
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                (chatFragment).setChatManager((ChatManager) obj);
        }

        return true;
    }

    /* Notifies when the state of the connection changes */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Thread socketHandler = null;

        /* Group owner */
        if (info.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                socketHandler = new ServerSocketHandler(this.getHandler());
                socketHandler.start();
            } catch (IOException e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
                return;
            }

            /* Peer */
        } else {
            Log.d(TAG, "Connected as a peer");
            socketHandler = new ClientSocketHandler(this.getHandler(), info.groupOwnerAddress);
            socketHandler.start();
        }

        chatFragment = new ChatFragment();
        getFragmentManager().beginTransaction().replace(R.id.container_root, chatFragment).commit();
        statusTxtView.setVisibility(View.GONE);
    }

    public Handler getHandler() {
        return handler;
    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }
}
