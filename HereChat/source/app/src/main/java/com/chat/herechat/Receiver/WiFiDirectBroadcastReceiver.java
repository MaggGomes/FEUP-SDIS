package com.chat.herechat.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

import com.chat.herechat.ChatManager.ChatSearchScreenFrag;
import com.chat.herechat.Utilities.Constants;
import com.chat.herechat.LocalService;
import com.chat.herechat.ServiceHandlers.ClientSocketHandler;
import com.chat.herechat.Peer.Peer;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements ConnectionInfoListener, PeerListListener {
    private WifiP2pManager manager;
    private Channel channel;
    private LocalService service = null;
    public boolean isPeersDiscovered = false;

    public WiFiDirectBroadcastReceiver(Channel channel, LocalService service, WifiP2pManager manager) {
        super();

        this.manager = manager;
        this.channel = channel;
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (service != null) {
            if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (manager == null)
                    return;

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected())
                    manager.requestConnectionInfo(channel, this);

                else {
                    service.groupOwner = false;
                    ChatSearchScreenFrag.groupConnect = false;
                }
            }

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_ENABLED, -1);
                } else {
                    service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_DISABLED, -1);
                }
            }

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action) && !isPeersDiscovered) {
                service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_CHANGED, -1);

                if (manager != null) {
                    manager.requestPeers(channel, this);
                    isPeersDiscovered = true;
                }
            }
        }
    }


    public WifiP2pDevice[] createWifiP2pDeviceArray(WifiP2pDeviceList peerList) {
        Object[] deviceArray = peerList.getDeviceList().toArray();
        if (deviceArray != null) {
            int size = deviceArray.length;

            WifiP2pDevice arr[] = new WifiP2pDevice[size];
            for (int i = 0; i < size; i++) {
                arr[i] = (WifiP2pDevice) deviceArray[i];
            }
            return arr;
        }
        return null;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEERS_AVAILABLE, -1);
        service.devices = createWifiP2pDeviceArray(peerList);
        service.onPeerDeviceListAvailable();
    }


    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        ChatSearchScreenFrag.groupConnect = true;
        service.refreshHandler.sendEmptyMessageDelayed(service.timeoutPeer,
                Constants.VALID_COMM_WITH_WIFI_PEER_TO);


        if (!info.isGroupOwner && info.groupFormed) {
            Peer peer = new Peer(null, null, info.groupOwnerAddress.getHostAddress());
            service.UpdateDiscoveredUsersList(info.groupOwnerAddress.getHostAddress(), null, null); //update the peer list

            ClientSocketHandler workerThread = new ClientSocketHandler(service, peer, Constants.CONNECTION_CODE_DISCOVER);
            workerThread.setPriority(Thread.MAX_PRIORITY);
            workerThread.start();
        } else {
            service.groupOwner = true;
        }
    }
}