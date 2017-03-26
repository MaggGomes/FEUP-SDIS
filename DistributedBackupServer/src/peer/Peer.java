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
	
	private double protocolVersion;
	private String serverID;
	private int acessPoint;
	
	private ControlChannel mc;
	private BackupChannel mdb;
	private RestoreChannel mdr;
	
	private Thread control;
	private Thread backup;
	private Thread restore;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Verificar se os argumentos estão corretos e se com o numero suficiente de argumentos		
		
		try {
			Peer peer = new Peer(args);	
			IPeerInterface stub =  (IPeerInterface) UnicastRemoteObject.exportObject(peer, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(peer.serverID, stub);
			peer.init();
			
		} catch(Exception e) {
			e.printStackTrace();			
		}
		
		
		
		
	}
	
	//TODO - VALIDAR INPUTS
	public Peer(String[] args){
		this.protocolVersion = Double.parseDouble(args[0]);
		this.serverID = args[1];
		this.acessPoint = Integer.parseInt(args[2]);
		
		System.out.println("Peer "+this.serverID+" initiated!");
		
		initChannels(args);		
	}
	
	// TODO booleano para verificar se tudo funcionou bem?
	public void initChannels(String args[]) {
		
		try {
			this.mc = new ControlChannel(args[3], args[4]);
			this.mdb = new BackupChannel(args[5], args[6]);
			this.mdr = new RestoreChannel(args[7], args[8]);	
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
	}
	
	public void init(){

		
		
		//TODO CODIGO PARA TESTAR
		
		
		
		
		control = new Thread(this.mc);
		control.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
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
		mc.sendMessage("iujimknui");
		
		return "state";
	}

	public String getServerID() {
		return serverID;
	}	

	public int getAcessPoint() {
		return acessPoint;
	}
	
	public ControlChannel getMc() {
		return mc;
	}

	public BackupChannel getMdb() {
		return mdb;
	}

	public RestoreChannel getMdr() {
		return mdr;
	}
}
