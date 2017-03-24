package peer;

import java.rmi.RemoteException;

public class Peer implements IPeerInterface{
	
	private static double protocolVersion;
	private static int serverID;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Verificar se os argumentos estão corretos e se com o numero suficiente de argumentos
		
		protocolVersion = Double.parseDouble(args[0]);
		serverID = Integer.parseInt(args[1]);
		
		System.out.println(protocolVersion);
		System.out.println(serverID);
		
		// args[2] serviceacessepoint

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

	public static double getProtocolVersion() {
		return protocolVersion;
	}

	public static int getServerID() {
		return serverID;
	}
}
