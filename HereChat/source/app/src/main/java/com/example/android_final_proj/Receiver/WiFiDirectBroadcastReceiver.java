package com.example.android_final_proj.Receiver;

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

import com.example.android_final_proj.chat.ChatSearchScreenFrag;
import com.example.android_final_proj.Constants;
import com.example.android_final_proj.LocalService;
import com.example.android_final_proj.thread.NewConnectionWorkerThread;
import com.example.android_final_proj.User;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements ConnectionInfoListener, PeerListListener {

    private WifiP2pManager manager;
    private Channel channel;
    private LocalService service = null;
    //This flag will make sure that mManager.requestPeers() happens only once per refresh operation
    public boolean isPeersDiscovered = false;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, LocalService srv) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.service = srv;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

    	if (service!=null) {
	        
	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) //Broadcast intent action to indicate whether Wi-Fi p2p is enabled or disabled.
	        {
	            // Check to see if Wi-Fi is enabled and notify appropriate activity
	            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
	            	{
	                // Wifi P2P is enabled
	            	service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_ENABLED,-1);
	            	} 
	            else 
	            	{
	                // Wi-Fi P2P is disabled
	            	service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_P2P_DISABLED,-1);
	            	}  	
	        } //WIFI_P2P_STATE_CHANGED_ACTION
	        
	       if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action) && !isPeersDiscovered) //Broadcast intent action indicating that peer discovery has either started or stopped.
	        {
	        	service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEER_CHANGED,-1);
	            /** Call WifiP2pManager.requestPeers() to get a list of current peers
	            * This is an asynchronous call and the calling activity is notified with a
	            * callback on PeerListListener.onPeersAvailable()
	            */
	            if (manager != null)
	            {
	            	//get the peer list by creating a listener to with the onPeersAvailable() method
	                manager.requestPeers(channel, this); //update the peer list
	                //SINCE WE'VE NOTICED THE SYSTEM ENTERS HERE WAY TOO MANY TIMES ON EVERY REFRESH, WE WANNA MAKE SURE
	                //THAT mManager.requestPeers() HAPPENS ONLY ONCE PER REFRESH OPERATION
	                isPeersDiscovered = true;
	            }
	        }//WIFI_P2P_PEERS_CHANGED_ACTION
	            
	      if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) 
	       {
             //Respond to new connection or disconnections
              if (manager == null)
                  return;

              NetworkInfo networkInfo = (NetworkInfo) intent
                      .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

              if (networkInfo.isConnected()) 
                  // We are connected with the other device, request connection
                  // info to find group owner IP
                  manager.requestConnectionInfo(channel, this);

            else 
            {
               // It's a disconnect
               ChatSearchScreenFrag.mIsConnectedToGroup=false;  //reset the connection state
               service.mIsWifiGroupOwner=false;
            }
	       }  
    	}
    }
    
    /**
     * this function gets a  list of peers "peerList"
     * @param peerList -WifiP2pDeviceList
     * @return an array of devices contained in this list
     */
    public WifiP2pDevice[] createWifiP2pDeviceArray(WifiP2pDeviceList peerList)
    {
    	Object[] obArry = peerList.getDeviceList().toArray();
    	if(obArry!=null){
        	int size = obArry.length;
        	
        	WifiP2pDevice wifiDivArry[]= new  WifiP2pDevice[size];
        	for (int i=0;i<size;i++){
        		wifiDivArry[i]= (WifiP2pDevice)obArry[i];
        	}//for
        	return wifiDivArry;
    	}//if(obArry!=null)
		return null;
    }//end of createWifiP2pDeviceArray()

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peerList)
	{
		  // Out with the old, in with the new.      
    	service.CreateAndBroadcastWifiP2pEvent(Constants.SERVICE_BROADCAST_WIFI_EVENT_PEERS_AVAILABLE,-1);
    	//gets WifiP2pDevice array out of the peerList
        service.mDevices =createWifiP2pDeviceArray(peerList);
    	//call the method that continues with the chat room discovery protocol
    	service.onPeerDeviceListAvailable();
	}
	
	/**When info is available, we check if the other peer is the wifiP2p group owner. If so, we can get its IP
	 * and open a socket to it.
	*/
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info)
	{
		//if we've reached this point it means a connection was established:
		ChatSearchScreenFrag.mIsConnectedToGroup=true;
		//We're connected. Start a timer, which in the end of we'll check if this group is valid and runs our app.
		service.mRefreshHandler.sendEmptyMessageDelayed(service.Handler_WHAT_valueForActivePeerTO,
				Constants.VALID_COMM_WITH_WIFI_PEER_TO);
		
		if (!info.isGroupOwner && info.groupFormed) //if the other peers is the owner, we can get it's IP address:
		{
			User peer = new User(null, info.groupOwnerAddress.getHostAddress(), null); //create a new peer
			service.UpdateDiscoveredUsersList(info.groupOwnerAddress.getHostAddress(),null,null); //update the peer list
	
			NewConnectionWorkerThread workerThread = new NewConnectionWorkerThread(service, peer, Constants.CONNECTION_CODE_DISCOVER);
			 workerThread.setPriority(Thread.MAX_PRIORITY);
			 workerThread.start();
		} else {
			service.mIsWifiGroupOwner=true;
		}
	}
}


