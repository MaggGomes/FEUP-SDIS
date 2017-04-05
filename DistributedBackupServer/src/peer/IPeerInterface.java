package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *	Remote Method Invocation Interface to be used by peer
 */
public interface IPeerInterface extends Remote {
	
	void backup(String protocoloV, String filename, int replicationDeg) throws RemoteException;
	
	void restore(String filename) throws RemoteException;
	
	void delete(String filename) throws RemoteException;
	
	void reclaim(int space) throws RemoteException;
	
	String state() throws RemoteException;
}
