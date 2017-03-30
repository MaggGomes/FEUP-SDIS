package subprotocols;

import peer.Peer;

public abstract class Protocol {
	protected static Peer peer;
	
	public static void start(Peer peer){
		Protocol.peer = peer;
	}
}
