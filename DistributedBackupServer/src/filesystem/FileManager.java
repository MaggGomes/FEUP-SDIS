package filesystem;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
	
	// TODO - CORRIGIR
	// Own files
	public static ConcurrentHashMap<String, FileInfo> files = new ConcurrentHashMap<String, FileInfo>();
	
	// Stored files
	// TODO - CORRIGIR
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> filesTrackReplication = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
	public static ConcurrentHashMap<String, Set<Integer>> storedFiles = new ConcurrentHashMap<String, Set<Integer>>();
	
	public static void addOwnFile(FileInfo file){
		files.put(file.getFileID(), file);
	}
	
	public static void addOwnFileChunk(String fileID, int chunkNo){
		files.get(fileID).addChunk(chunkNo);
	}
	
	public static void addOwnChunkReplication(String fileID, int chunkNo){
		files.get(fileID).addReplication(chunkNo);
	}
	
	public static boolean hasOwnFile(String fileID){	
		return files.containsKey(fileID);
	}
	
	public static int getOwnCurrentChunkReplication(String fileID, int chunkNo){
		return files.get(fileID).getChunkReplication(chunkNo);
	}

	// TODO - CRIAR MAIS METODOS??
	
	public static void updateStoredReplicationDeg(String fileID, int chunkNo) {			
		int chunkrep = FileManager.filesTrackReplication.get(fileID).get(chunkNo);
		FileManager.filesTrackReplication.get(fileID).put(chunkNo, chunkrep+1);		
	}
	
	// TODO - SÓ ESTÁ A FUNCIONAR PARA STORED CHUNKS
	public static boolean hasChunk(String fileID, String chunkNo){
		
		if (storedFiles.containsKey(fileID)){
			if(storedFiles.get(fileID).contains(chunkNo))
				return true;
		}
		
		return false;
	}
}
