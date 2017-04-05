package filesystem;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
	
	// Backed up files 
	// Hash for getting stored fileID  file pathname -> fileID
	public static ConcurrentHashMap<String, String> nameToFileID = new ConcurrentHashMap<String, String>();
	// fileID -> file info
	public static ConcurrentHashMap<String, BackedUpFile> backedUpFiles = new ConcurrentHashMap<String, BackedUpFile>();
	
	// Stored chunks
	// TODO - CORRIGIR - APAGAR
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> filesTrackReplication = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
	public static ConcurrentHashMap<String, Set<Integer>> storedFiles = new ConcurrentHashMap<String, Set<Integer>>();
	
	// TODO - UTILIZAR
	public static ConcurrentHashMap<String, Set<Chunk>> storedChunks = new ConcurrentHashMap<String, Set<Chunk>>();
	
	
		
	// TODO - USAR
	public static int usedSpace;
	
	public static void addBackedUpFile(BackedUpFile file){
		nameToFileID.put(file.getFilePath(), file.getFileID());
		backedUpFiles.put(file.getFileID(), file);
	}
	
	public static void addBackedUpFileChunk(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addChunk(chunkNo);
	}
	
	public static boolean hasBackedUpFilePathName(String filePath){	
		return nameToFileID.containsKey(filePath);
	}
	
	public static boolean hasBackedUpFileID(String fileID){	
		return backedUpFiles.containsKey(fileID);
	}
	
	public static String getBackedUpFileID(String filePath) {
		return nameToFileID.get(filePath);
	}
	
	public static String getBackedUpFileName(String fileID) {
		return backedUpFiles.get(fileID).getFileName();
	}
	
	public static ConcurrentHashMap <Integer, Integer> getChunksBackedUpFile(String fileID){
		return backedUpFiles.get(fileID).getChunks();
	}
	
	public static void deleteBackedUpFile(String filePath, String fileID){
		nameToFileID.remove(filePath);
		backedUpFiles.remove(fileID);
	}
	
	public static void addBackedUpChunkReplication(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addReplication(chunkNo);
	}
	
	public static int getBackedUpCurrentChunkReplication(String fileID, int chunkNo){
		return backedUpFiles.get(fileID).getChunkReplication(chunkNo);
	}
	
	public static void updateStoredReplicationDeg(String fileID, int chunkNo) {
		if(FileManager.filesTrackReplication.containsKey(fileID)){
			if(!FileManager.filesTrackReplication.get(fileID).containsKey(chunkNo)){
				FileManager.filesTrackReplication.get(fileID).put(chunkNo, 0);	
			} 
		} else {
			FileManager.filesTrackReplication.put(fileID, new ConcurrentHashMap<Integer, Integer>());
			FileManager.filesTrackReplication.get(fileID).put(chunkNo, 0);
		}
		
		int chunkrep = FileManager.filesTrackReplication.get(fileID).get(chunkNo);
		FileManager.filesTrackReplication.get(fileID).put(chunkNo, chunkrep+1);	
	}	
}
