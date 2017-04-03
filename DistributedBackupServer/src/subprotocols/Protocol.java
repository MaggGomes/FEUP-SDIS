package subprotocols;

import peer.Peer;

public abstract class Protocol {
	protected static Peer peer;	
	protected static final int MAX_TRYS = 5;
	protected static final int WAIT = 1000;
	protected static final int MAX_WORKERS = 10;
	protected static final int CHUNK_MAXSIZE = 64000;
	protected static ExecutorService threadWorkers;
	
	public static void start(Peer peer){
		Protocol.peer = peer;
	}
}
