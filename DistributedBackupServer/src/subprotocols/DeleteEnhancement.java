package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import peer.Peer;

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
			FileManager.deleteBackedUpFile(filePath, fileID);
			//sendDelete(fileID);
			Message message = new Message(Message.DELETE, peer.getProtocolVersion(), peer.getServerID(), fileID);
			
			/* Sends message to the other peers to delete the chunks from this file */
			peer.getMc().sendMessage(message.getMessage());
			peer.saveMetadata();
		}
	}
	
	public static void sendDelete(final String fileID, final int replicationDeg){

		new Thread((
				new Runnable(){
					@Override
					public void run(){
						/*Message message = new Message(Message.DELETE, peer.getProtocolVersion(), peer.getServerID(), fileID);
						int trys = 0;
						int waitStored = WAIT;

						while(FileManager.getPerceivedReplicationDeg(fileID, chunkNo) < replicationDeg && trys < MAX_TRYS){
							peer.getMdb().sendMessage(msg.getMessage());
							System.out.println("Sending chunk "+chunkNo);
							try{
								Thread.sleep(waitStored);
							} catch(InterruptedException e){
								e.printStackTrace();
							}

							waitStored = waitStored*2;
							trys++;
						}

						if (trys >= MAX_TRYS)
							System.out.println("Failed to save chunk "+chunkNo+".");
						else 				
							System.out.println("Chunk "+chunkNo+" saved with success!");	*/					
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
}
