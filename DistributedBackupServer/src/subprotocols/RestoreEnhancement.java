package subprotocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import peer.Peer;

import utilities.Message;
import utilities.Utilities;

import filesystem.Chunk;
import filesystem.FileManager;

public class RestoreEnhancement extends Protocol{
	/* Recovered chunks by the initiator peer */
	public static ConcurrentHashMap<Integer, byte[]> chunksRecovered;
	
	public static ConcurrentHashMap<Integer, String> chunksToBeRecovered;
	
	public static ConcurrentHashMap<String, HashSet<Integer>> chunksSent = new ConcurrentHashMap<String, HashSet<Integer>>();

	/**
	 * Attempts to restore a backed up file
	 * 
	 * @param filePath
	 */
	public static void restoreFile(String filePath){
		threadWorkers = Executors.newFixedThreadPool(MAX_WORKERS);

		if(FileManager.hasBackedUpFilePathName(filePath)){
			String fileID = FileManager.getBackedUpFileID(filePath);
			System.out.println("Attempting to restore file...");
			chunksRecovered = new ConcurrentHashMap<Integer, byte[]>();
			chunksToBeRecovered = new ConcurrentHashMap<Integer, String>();
			chunksSent = new ConcurrentHashMap<String, HashSet<Integer>>();
			ConcurrentHashMap<Integer, Chunk> fileChunks = FileManager.getChunksBackedUpFile(fileID);
			
			/* Requests each chunk */
			for (Integer chunkNo: fileChunks.keySet())
				sendGetChunk(fileID, Integer.toString(chunkNo));			
			
			/* Creating server socket */
			try {
				serverSocket = new ServerSocket(6789+Integer.parseInt(peer.getServerID()));
			} catch(IOException e){
				System.out.println("Failed to create TCP server connection.");
			}
			
			while(chunksRecovered.size() < fileChunks.size()){				
				Message message;
							
				try {
					privateSocket = serverSocket.accept();
					DataInputStream in = new DataInputStream(privateSocket.getInputStream());
					
					int size = in.readInt();					
					byte[] buf;
					
					if(size > 0){
						buf = new byte[size];
						in.readFully(buf, 0, buf.length);
						message = new Message(buf);
						
						/* Verifies if the chunk was already recovered */
						if(!chunksRecovered.containsKey(Integer.parseInt(message.getChunkNo())))
							chunksRecovered.put(Integer.parseInt(message.getChunkNo()), message.getBody());	
					}									
				} catch(IOException e){
					System.out.println("Failed to create TCP socket.");
				}				
			}		
						
			/* Closes the sockets */
			try {
				privateSocket.close();
				serverSocket.close();
			} catch(IOException e){
				System.out.println("Failed to close TCP server.");
			}			
			
			/* Wait */
			try {
				Thread.sleep(fileChunks.size()*1000);
				/* Restoring the file joining all chunks */
				joinChunks(fileID, FileManager.getBackedUpFileName(fileID));
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}	
			
			/* Shutting down workers */			
			try {
				threadWorkers.shutdown();
				threadWorkers.awaitTermination(fileChunks.size()*5000, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e2) {
				System.err.println("Workers interrupted.");
			}
			finally {
				if (!threadWorkers.isTerminated()) {
					System.err.println("Canceling non-finished tasks");
				}
				threadWorkers.shutdownNow();
				System.out.println("Shutdown finished!");
			}			
		} else {
			System.out.println("File not backed up!");
		}
	}

	/**
	 * Send a request for all connected peer asking for the chunks of the file to be restored
	 * 
	 * @param fileID of the file to be restored
	 * @param chunkNo of the chunk
	 */
	public static void sendGetChunk(final String fileID, final String chunkNo){
		threadWorkers.submit(new Thread() {
			public void run() {
				Message msg = new Message(Message.GETCHUNK, Peer.enhancedProtocolVersion, peer.getServerID(), fileID, chunkNo);
				int trys = 0;
				int waitStored = WAIT;

				while(!chunksToBeRecovered.containsKey(Integer.parseInt(chunkNo)) && trys < MAX_TRYS){
				
					peer.getMc().sendMessage(msg.getMessage());
					
					try{
						Thread.sleep(waitStored);
					} catch(InterruptedException e){
						e.printStackTrace();
					}
				
					waitStored = waitStored*2;
					trys++;
				}

				if (trys >= MAX_TRYS)
					System.out.println("Failed to recover chunk "+chunkNo+".");
			}
		});		
	}

	/**
	 * Reads the data of a backed up chunk
	 * 
	 * @param message
	 */
	public static void getChunk(Message message){
		/* Verifies if the sender is the same as the receiver */
		if(message.getSenderID().equals(peer.getServerID()))
			return;

		/* Verifies if the peer has any chunk of the requested file */
		if(FileManager.hasStoredFileID(message.getFileID())){
			/* Verifies if the peer has the requested chunk */
			if (FileManager.hasStoredChunkNo(message.getFileID(), Integer.parseInt(message.getChunkNo()))){
				if(!chunksSent.contains(message.getFileID()))
					chunksSent.put(message.getFileID(), new HashSet<Integer>());				
				
				/* Verifies if the chunk was already sent */
				if(!chunksSent.get(message.getFileID()).contains(Integer.parseInt(message.getChunkNo()))){					
					sendHaveChunk(message.getFileID(), Integer.parseInt(message.getChunkNo()));
				}
			}
		}
	}

	/**
	 * Sends a chunk to the requesting peer
	 * 
	 * @param fileID of the file to be restored
	 * @param chunkNo of the chunk
	 * @param data of the chunk
	 */
	public static void sendHaveChunk(final String fileID, final int chunkNo){

		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						// Verifies if the chunk was already sent
						if(!chunksSent.contains(chunkNo)){
							Message message = new Message(Message.HAVECHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, Integer.toString(chunkNo));
							peer.getMdr().sendMessage(message.getMessage());
							chunksSent.get(fileID).add(chunkNo);
						}						
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);	
	}

	/**
	 * Saves a recovered chunk in the hash
	 * 
	 * @param message
	 */
	public static void recoverChunk(Message message){		
		if(chunksSent.contains(message.getFileID())){
			if(!chunksSent.get(message.getFileID()).contains(Integer.parseInt(message.getChunkNo())))
				chunksSent.get(message.getFileID()).add(Integer.parseInt(message.getChunkNo()));
		} else {
			chunksSent.put(message.getFileID(), new HashSet<Integer>());
			chunksSent.get(message.getFileID()).add(Integer.parseInt(message.getChunkNo()));
		}

		/* Verifies if this is the initiator peer which requested the file */
		if(FileManager.hasBackedUpFileID(message.getFileID())){
			if(!chunksToBeRecovered.containsKey(Integer.parseInt(message.getChunkNo()))){
				chunksToBeRecovered.put(Integer.parseInt(message.getChunkNo()), message.getSenderID());
				Message msg= new Message(Message.SENDCHUNK, message.getSenderID(), peer.getServerID(), message.getFileID(), message.getChunkNo());
				peer.getMdr().sendMessage(msg.getMessage());
			}			
		}			
	}
	
	public static void sendChunk(Message message){
		
		System.out.println("Sending chunk "+message.getChunkNo()+" throught TCP.");
			
		try {
			clientSocket = new Socket("127.0.0.1", 6789+Integer.parseInt(message.getSenderID()));
		} catch (IOException e){
			System.out.println("Failed to create client socket");
		}
		
		String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), message.getChunkNo());
		Path chunkPath = Paths.get(path);

		try {
			byte[] chunk = Files.readAllBytes(chunkPath);
			Message msg = new Message(Message.CHUNK, peer.getProtocolVersion(), peer.getServerID(), message.getFileID(), Integer.parseInt(message.getChunkNo()), chunk);			
			DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
			os.writeInt(msg.getMessage().length);
			os.write(msg.getMessage());
			os.close();
			clientSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to send chunk.");
		}
	}

	/**
	 * Joins all recovered chunks in order to restore the file
	 * 
	 * @param fileID of the file to be restored
	 * @param fileName of the file to be restored
	 */
	public static void joinChunks(String fileID, String fileName){

		String path = Utilities.createRestorePath(peer.getServerID(), fileID, fileName);
		Path chunkPath = Paths.get(path);			
		try {
			Files.createDirectories(chunkPath.getParent());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		File file = new File(path);
		FileOutputStream fos;		
		
		try {
            fos = new FileOutputStream(file);
            
            TreeMap<Integer, byte[]> sortedChunks = new TreeMap<Integer, byte[]>(chunksRecovered);
            
            for(Entry<Integer, byte[]> entry: sortedChunks.entrySet()){
            	try {
                    fos.write(entry.getValue(), 0, entry.getValue().length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            fos.close(); 
            System.out.println("File restored successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }		 
	}
}
