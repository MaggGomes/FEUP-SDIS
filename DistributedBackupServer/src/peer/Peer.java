package peer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import subprotocols.Backup;
import subprotocols.Delete;
import subprotocols.Protocol;

import channels.BackupChannel;
import channels.ControlChannel;
import channels.RestoreChannel;

public class Peer implements IPeerInterface{
	
	private String protocolVersion;	
	private String accessPoint;
	
	private ControlChannel mc;
	private BackupChannel mdb;
	private RestoreChannel mdr;
	
	private Thread control;
	private Thread backup;
	private Thread restore;
	
	private String serverID;
	public static final String BACKUP = "Backup/";
	public static final String RESTORED = "Restored/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Verificar se os argumentos estão corretos e se com o numero suficiente de argumentos		
		
		try {
			Peer peer = new Peer(args);	
			IPeerInterface stub =  (IPeerInterface) UnicastRemoteObject.exportObject(peer, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(peer.getAccessPoint(), stub);
			peer.listen();
			
		} catch(Exception e) {
			e.printStackTrace();			
		}		
	}
	
	//TODO - VALIDAR INPUTS
	public Peer(String[] args){
		this.protocolVersion = args[0];
		this.serverID = args[1];
		this.accessPoint = args[2];
		
		System.out.println("Peer "+this.serverID+" initiated!");
		
		init(args);		
	}
	
	/**
	 * Connects the peer to the channels
	 * 
	 * @param args
	 */
	public void init(String args[]) {
		
		try {
			this.mc = new ControlChannel(this, args[3], args[4]);
			this.mdb = new BackupChannel(this, args[5], args[6]);
			this.mdr = new RestoreChannel(this, args[7], args[8]);	
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		Protocol.start(this);		
	}
	
	public void listen(){		
		this.mc.listen();
		this.mdb.listen();
		this.mdr.listen();
	}

	@Override
	public void backup(String filePath, int replicationDeg) throws RemoteException {		
		Backup.saveFile(filePath, replicationDeg);		
	}

	@Override
	public void restore(String filePath) throws RemoteException {
		Restore.restoreFile(filePath);
	}

	@Override
	public void delete(String filePath) throws RemoteException {
		Delete.deleteFile(filePath);		
	}

	@Override
	public void reclaim(int space) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String state() throws RemoteException {
		// TODO Auto-generated method stub
		//mc.sendMessage("iujimknui");
		
		return "state";
	}

	public String getServerID() {
		return serverID;
	}	
	
	public String getProtocolVersion(){
		return protocolVersion;
	}

	public String getAccessPoint() {
		return accessPoint;
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

	public Thread getControl() {
		return control;
	}
	public Thread getBackup() {
		return backup;
	}

	public Thread getRestore() {
		return restore;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}
}
