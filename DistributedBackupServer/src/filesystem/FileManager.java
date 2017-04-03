package filesystem;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
	
	// TODO - CORRIGIR
	// Stored files
	public static ConcurrentHashMap<String, FileInfo> backedUpFiles = new ConcurrentHashMap<String, FileInfo>();
	
	// Backup files
	// TODO - CORRIGIR
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> filesTrackReplication = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
	public static ConcurrentHashMap<String, Set<Integer>> storedFiles = new ConcurrentHashMap<String, Set<Integer>>();
		
	// TODO - USAR
	public static int usedSpace;
	
	public static void addBackedUpFile(FileInfo file){
		backedUpFiles.put(file.getFileID(), file);
	}
	
	public static void addBackedUpFileChunk(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addChunk(chunkNo);
	}
	
	public static boolean hasBackedUpFile(String fileID){	
		return backedUpFiles.containsKey(fileID);
	}
	
	public static ConcurrentHashMap <Integer, Integer> getChunksBackedUpFile(String fileID){
		return backedUpFiles.get(fileID).getChunks();
	}
	
	// TODO - ACRESCNTAR REMOÇÃO DO FICHEIRO DA HASHMAP <FILEID, FILENAME>
	public static void deleteBackedUpFile(String fileID){
		backedUpFiles.remove(fileID);
	}
	
	public static void addBackedUpChunkReplication(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addReplication(chunkNo);
	}
	
	public static int getBackedUpCurrentChunkReplication(String fileID, int chunkNo){
		return backedUpFiles.get(fileID).getChunkReplication(chunkNo);
	}

	// TODO - CRIAR MAIS METODOS??
	
	public static void updateStoredReplicationDeg(String fileID, int chunkNo) {			
		int chunkrep = FileManager.filesTrackReplication.get(fileID).get(chunkNo);
		FileManager.filesTrackReplication.get(fileID).put(chunkNo, chunkrep+1);		
	}
}
