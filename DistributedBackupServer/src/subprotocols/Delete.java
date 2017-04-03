package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import utilities.Message;
import utilities.Utilities;
import filesystem.FileInfo;
import filesystem.FileManager;

public class Delete extends Protocol {
	
	// TODO - RETORNAR BOOLEANO A DIZER
	public static void deleteFile(String filePath){
		
		FileInfo file = new FileInfo(filePath);

		if (FileManager.hasBackedUpFile(file.getFileID())){;
			System.out.println("File found. Attempting to delete now...");
			FileManager.deleteBackedUpFile(file.getFileID());
			Message message = new Message(Message.DELETE, peer.getProtocolVersion(), peer.getServerID(), file.getFileID());
			// TODO - APAGAR FICHEIRO DO HASHMAP TAMBÉM
			peer.getMc().sendMessage(message.getMessage());
		}
	}
	
	// TODO - CORRIGIR
	public static void deleteStoredFile(Message message){

		if (message.getSenderID().equals(peer.getServerID()))
			return;
		
		if(FileManager.storedFiles.containsKey(message.getFileID())){
			// TODO - PASSAR ESTE CODIGO APRA DENTO DO FILEMANAGER
			Set<Integer> chunks= FileManager.storedFiles.get(message.getFileID());
			
			for (Integer chunkNo : chunks){				
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
			FileManager.storedFiles.remove(message.getFileID());
			FileManager.filesTrackReplication.remove(message.getFileID());			
		}
		
		System.out.println("File chuncks deleted with success!");
			
		
	}
}
