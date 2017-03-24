package peer;

import java.rmi.RemoteException;

public class Peer implements IPeerInterface{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void backup(String filename, int replicationDeg) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restore(String filename) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(String filename) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reclaim(int space) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String state() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
