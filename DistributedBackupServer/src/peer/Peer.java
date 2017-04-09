package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

import subprotocols.Backup;
import subprotocols.BackupEnhancement;
import subprotocols.Delete;
import subprotocols.Protocol;
import subprotocols.Reclaim;
import subprotocols.Restore;
import utilities.Utilities;

import channels.BackupChannel;
import channels.ControlChannel;
import channels.RestoreChannel;
import filesystem.BackedUpFile;
import filesystem.Chunk;
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
	public static final String DATA = "Data/data";

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
		loadMetadata();
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

	/**
	 * Initializes channels
	 */
	public void listen(){		
		this.mc.listen();
		this.mdb.listen();
		this.mdr.listen();
	}

	@Override
	public void backup(String version, String filePath, int replicationDeg) throws RemoteException {		
		if(version.equals("1.0"))
			Backup.backUpFile(filePath, replicationDeg);
		else
			BackupEnhancement.backUpFile(filePath, replicationDeg);	
	}

	@Override
	public void restore(String filePath) throws RemoteException {
		Restore.restoreFile(filePath);
	}

	/**
	 * Attempts to delete a file
	 * 
	 * @param filePath of the file to be deleted
	 */
	@Override
	public void delete(String filePath) throws RemoteException {
		Delete.deleteFile(filePath);		
	}

	//TODO - VERIFICAR SE FUNCIONA
	@Override
	public void reclaim(long space) throws RemoteException {
		Reclaim.reclaimSpace(space);
	}

	/**
	 * Provides the state of the files in the peer in string format
	 */
	@Override
	public String state() throws RemoteException {		
		return FileManager.getState();
	}

	/**
	 * Saves metadata in non-volatile memory
	 */
	public void saveMetadata(){
		ObjectOutputStream os;		

		try {
			String path = Utilities.createDataPath(this.serverID);
			Path osPath = Paths.get(path);			
			Files.createDirectories(osPath.getParent());			
			File osFile = new File(path);

			os = new ObjectOutputStream(new FileOutputStream(osFile));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			os.writeObject(FileManager.nameToFileID);
			os.writeObject(FileManager.backedUpFiles);
			os.writeObject(FileManager.filesTrackReplication);
			os.writeObject(FileManager.storedChunks);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Loads metadata saved in non-volatile memory
	 */
	@SuppressWarnings("unchecked")
	public void loadMetadata(){
		ObjectInputStream is;		

		try {
			String path = Utilities.createDataPath(this.serverID);
			Path osPath = Paths.get(path);	
			// Verifies if there is any saved metadata
			if(!Files.exists(osPath))
				return;
			
			File isFile = new File(path);

			is = new ObjectInputStream(new FileInputStream(isFile));
		} catch (IOException e) {
			System.out.println("No saved metadata found!");
			return;
		}

		try {
			FileManager.nameToFileID = (ConcurrentHashMap<String, String>) is.readObject();
			FileManager.backedUpFiles = (ConcurrentHashMap<String, BackedUpFile>) is.readObject();
			FileManager.filesTrackReplication = (ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>) is.readObject();
			FileManager.storedChunks = (ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>>) is.readObject();
			is.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}		
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
