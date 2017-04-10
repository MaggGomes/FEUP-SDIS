package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import utilities.Message;
import utilities.Utilities;
import filesystem.Chunk;
import filesystem.FileManager;

public class DeleteEnhancement extends Protocol {
	
	/**
	 * Attempts to delete the specified file
	 * 
	 * @param filePath of the file to delete
	 */
	public static void deleteFile(String filePath){

		if (FileManager.hasBackedUpFilePathName(filePath)){
			String fileID = FileManager.getBackedUpFileID(filePath);
			System.out.println("File found. Attempting to delete now...");
			ConcurrentHashMap<Integer, Chunk> fileChunks = FileManager.getChunksBackedUpFile(fileID);
			int numChunks = fileChunks.size();
			FileManager.deleteBackedUpFile(filePath, fileID);
			sendDelete(fileID);			
			
			try {
				Thread.sleep(numChunks*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
			
			peer.saveMetadata();
		}
	}
	
	public static void sendDelete(final String fileID){
		new Thread((
				new Runnable(){
					@Override
					public void run(){												
						Message message = new Message(Message.DELETE, peer.getProtocolVersion(), peer.getServerID(), fileID);
						int waitStored = WAIT;

						while(FileManager.filesTrackReplication.get(fileID).size() > 0){
							/* Sends message to the other peers to delete the chunks from this file */
							peer.getMc().sendMessage(message.getMessage());
							try{
								Thread.sleep(waitStored);
							} catch(InterruptedException e){
								e.printStackTrace();
							}

							waitStored = waitStored*2;
							if (waitStored > 60000)
								waitStored = 60000;
						}			
					}
				})).start();	
	}
	
	/**
	 * Deletes the stored content of the file if exists
	 * 
	 * @param message
	 */
	public static void deleteStoredFile(Message message){
		/* Verifies if this peer is the initiator peer */
		if (message.getSenderID().equals(peer.getServerID()))
			return;
		
		if(FileManager.hasStoredFileID(message.getFileID())){
			ConcurrentHashMap<Integer, Chunk> chunks = FileManager.getStoredChunks(message.getFileID());
					
			for (Integer chunkNo : chunks.keySet()){				
				/* Deleting each chunk */
				try {
					String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), Integer.toString(chunkNo));
					Path chunkPath = Paths.get(path);
					Files.deleteIfExists(chunkPath);
					
					/* Removes the stored chunk from the structure and updates used storage */
					FileManager.removeStoredChunk(message.getFileID(), Integer.parseInt(message.getChunkNo()));
					
					Message msg = new Message(Message.SENDDELETE, peer.getProtocolVersion(), peer.getServerID(), message.getFileID(), Integer.toString(chunkNo));
					peer.getMc().sendMessage(msg.getMessage());
				} catch(IOException | SecurityException e) {
					System.out.println("Failed to delete chunk!");
				}
			}			
			
			/* Deleting directory */
			try {
				String dir = Utilities.createBackupDir(peer.getServerID(), message.getFileID());
				Path chunkDir = Paths.get(dir);
				Files.deleteIfExists(chunkDir);
			} catch (IOException e) {
				System.out.println("Failed to delete file directory!");
			}
			
			FileManager.removeStoredFile(message.getFileID());
			
			System.out.println("File chuncks deleted with success!");
			peer.saveMetadata();
		}				
	}
	
	/**
	 * Updates the delete chunk count
	 * 
	 * @param message
	 */
	public static void updateDelete(Message message){
		
		if (FileManager.filesTrackReplication.containsKey(message.getFileID())){
			
			int chunkrep = FileManager.filesTrackReplication.get(message.getFileID()).get(Integer.parseInt(message.getChunkNo()));
			FileManager.filesTrackReplication.get(message.getFileID()).put(Integer.parseInt(message.getChunkNo()), chunkrep-1);
			
			if((chunkrep-1)<= 0)
				FileManager.filesTrackReplication.get(message.getFileID()).remove(Integer.parseInt(message.getChunkNo()));			
		}		
	}
}
