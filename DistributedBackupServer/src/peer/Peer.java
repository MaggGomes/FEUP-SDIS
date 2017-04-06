package peer;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import subprotocols.Backup;
import subprotocols.BackupEnhancement;
import subprotocols.Delete;
import subprotocols.Protocol;
import subprotocols.Restore;

import channels.BackupChannel;
import channels.ControlChannel;
import channels.RestoreChannel;
import filesystem.BackedUpFile;
import filesystem.FileManager;

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

	//TODO - VERFICAR SE É PARA ESPECIFICAR ATRAVES DO PROCOLO VERSION A VERSAO A USAR DE UM PROTOCOLO
	@Override
	public void backup(String protocolV, String filePath, int replicationDeg) throws RemoteException {
		if(protocolV.equals("1.0"))
			Backup.saveFile(filePath, replicationDeg);
		else
			BackupEnhancement.saveFile(filePath, replicationDeg);	
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

	// TODO - VERIFICAR SE FUNCIONA - AINDA SO FAZ PARA BACKED UP FILES
	@Override
	public String state() throws RemoteException {
		String state = "";
		
		for (BackedUpFile file: FileManager.backedUpFiles.values())
			state += file;
		
		return state;
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
