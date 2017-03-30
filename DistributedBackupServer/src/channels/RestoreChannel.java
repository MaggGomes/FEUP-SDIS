package channels;

import java.net.UnknownHostException;

import peer.Peer;

import utilities.Message;

public class RestoreChannel extends Channel {

	public RestoreChannel(Peer peer, String address, String port) throws UnknownHostException {
		super(peer, address, port);
	}

	@Override
	public void run() {
		// TODO falta implementar
		
	}

	@Override
	public void processMessage(Message message) {
		// TODO Auto-generated method stub
		
	}

}
