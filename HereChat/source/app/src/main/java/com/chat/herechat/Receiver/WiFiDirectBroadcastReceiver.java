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

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements ConnectionInfoListener, PeerListListener {
    private WifiP2pManager manager;
    private Channel channel;
    private LocalService service = null;
    public boolean isPeersDiscovered = false;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, LocalService service) {
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

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected())
                    manager.requestConnectionInfo(channel, this);

                else { // It's a disconnect
                    service.mIsWifiGroupOwner = false;
                    ChatSearchScreenFrag.mIsConnectedToGroup = false;
                }
            }

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                /* Verifies if Wi-Fi is enabled */
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

    /**
     * Returns a list of peers
     *
     * @param peerList -WifiP2pDeviceList
     * @return an array of devices contained in this list
     */
    public WifiP2pDevice[] createWifiP2pDeviceArray(WifiP2pDeviceList peerList) {
        Object[] obArry = peerList.getDeviceList().toArray();
        if (obArry != null) {
            int size = obArry.length;

            WifiP2pDevice wifiDivArry[] = new WifiP2pDevice[size];
            for (int i = 0; i < size; i++) {
                wifiDivArry[i] = (WifiP2pDevice) obArry[i];
            }//for
            return wifiDivArry;
        }
        return null;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // Out with the old, in with the new.
        service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEERS_AVAILABLE, -1);
        //gets WifiP2pDevice array out of the peerList
        service.devices = createWifiP2pDeviceArray(peerList);
        //call the method that continues with the chat room discovery protocol
        service.onPeerDeviceListAvailable();
    }

    /**
     * When info is available, we check if the other peer is the wifiP2p group owner. If so, we can get its IP
     * and open a socket to it.
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        /* Connection established */
        ChatSearchScreenFrag.mIsConnectedToGroup = true;
        service.mRefreshHandler.sendEmptyMessageDelayed(service.Handler_WHAT_valueForActivePeerTO,
                Constants.VALID_COMM_WITH_WIFI_PEER_TO);

        /* Getting Group Owner IP address */
        if (!info.isGroupOwner && info.groupFormed) {
            Peer peer = new Peer(null, info.groupOwnerAddress.getHostAddress(), null);
            service.UpdateDiscoveredUsersList(info.groupOwnerAddress.getHostAddress(), null, null); //update the peer list

            ClientSocketHandler workerThread = new ClientSocketHandler(service, peer, Constants.CONNECTION_CODE_DISCOVER);
            workerThread.setPriority(Thread.MAX_PRIORITY);
            workerThread.start();
        } else {
            service.mIsWifiGroupOwner = true;
        }
    }
}