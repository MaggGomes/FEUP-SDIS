package peer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import channels.BackupChannel;
import channels.ControlChannel;
import channels.RestoreChannel;

public class Peer implements IPeerInterface{
	
	private static double protocolVersion;
	private static int serverID;
	private static int acessPoint;
	
	private static ControlChannel mc;
	private static BackupChannel mdb;
	private static RestoreChannel mdr;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Verificar se os argumentos estão corretos e se com o numero suficiente de argumentos
		
		protocolVersion = Double.parseDouble(args[0]);
		serverID = Integer.parseInt(args[1]);
		acessPoint = Integer.parseInt(args[2]);
		
		initChannels(args[3], args[4], args[5], args[6], args[7], args[8]);
	}
	
	// TODO booleano para verificar se tudo funcionou bem?
	public static void initChannels(String mcAddress, String mcPort, String mdbAddress, String mdbPort, String mdrAddress, String mdrPort) {
		
		try {
			mc = new ControlChannel(mcAddress, mcPort);
			mdb =new BackupChannel(mdbAddress, mdbPort);
			mdr = new RestoreChannel(mdrAddress, mdrPort);	
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		
		try {
			Peer peer = new Peer();
			IPeerInterface stub =  (IPeerInterface) UnicastRemoteObject.exportObject(peer, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(""+serverID, stub);
			
		} catch(Exception e) {
			e.printStackTrace();			
		}
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
		return "state";
	}

	public static double getProtocolVersion() {
		return protocolVersion;
	}

	public static int getServerID() {
		return serverID;
	}

	public static ControlChannel getMc() {
		return mc;
	}

	public static BackupChannel getMdb() {
		return mdb;
	}

	public static RestoreChannel getMdr() {
		return mdr;
	}

	public static int getAcessPoint() {
		return acessPoint;
	}

	public static void setAcessPoint(int acessPoint) {
		Peer.acessPoint = acessPoint;
	}
}
