package subprotocols;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import peer.Peer;

public abstract class Protocol {
	protected static ServerSocket serverSocket;
	protected static Socket privateSocket;
	protected static Socket clientSocket;
	protected static Peer peer;
	protected static ExecutorService threadWorkers;
	protected static final int MAX_TRYS = 5;
	protected static final int WAIT = 1000;
	protected static final int MAX_WORKERS = 10;
	public static final int CHUNK_MAXSIZE = 64000;	
	
	/**
	 * Initiates a peer
	 * 
	 * @param peer to be initiated
	 */
	public static void start(Peer peer){
		Protocol.peer = peer;
	}
}
