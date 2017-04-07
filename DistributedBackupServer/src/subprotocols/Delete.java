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

public class Delete extends Protocol {
	
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
			Message message = new Message(Message.DELETE, peer.getProtocolVersion(), peer.getServerID(), fileID);
			
			//Sends message to the other peers to delete the chunks from this file
			peer.getMc().sendMessage(message.getMessage());
			peer.saveMetadata();
		}
	}
	
	/**
	 * Deletes the stored content of the file if exists
	 * 
	 * @param message
	 */
	public static void deleteStoredFile(Message message){
		// Verifies if this peer is the initiator peer
		if (message.getSenderID().equals(peer.getServerID()))
			return;
		
		if(FileManager.hasStoredFileID(message.getFileID())){
			ConcurrentHashMap<Integer, Chunk> chunks = FileManager.getStoredChunks(message.getFileID());
					
			for (Integer chunkNo : chunks.keySet()){				
				// Deleting each chunk
				try {
					String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), Integer.toString(chunkNo));
					Path chunkPath = Paths.get(path);
					Files.deleteIfExists(chunkPath);					
				} catch(IOException | SecurityException e) {
					System.out.println("Failed to delete chunk!");
				}
			}			
			
			// Deleting directory
			try {
				String dir = Utilities.createBackupDir(peer.getServerID(), message.getFileID());
				Path chunkDir = Paths.get(dir);
				Files.deleteIfExists(chunkDir);
			} catch (IOException e) {
				System.out.println("Failed to delete file directory!");
			}
			
			FileManager.removeStoredFile(message.getFileID());
			FileManager.filesTrackReplication.remove(message.getFileID());	
			
			System.out.println("File chuncks deleted with success!");
			peer.saveMetadata();
		}				
	}
}
