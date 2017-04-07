package filesystem;

import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
	
	// Backed up files 
	// Hash for getting stored fileID  file pathname -> fileID
	public static ConcurrentHashMap<String, String> nameToFileID = new ConcurrentHashMap<String, String>();
	// fBacked up files fileID -> file info
	public static ConcurrentHashMap<String, BackedUpFile> backedUpFiles = new ConcurrentHashMap<String, BackedUpFile>();
	
	// Stored chunks
	// Replication degree for each chunk
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> filesTrackReplication = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
	// Stored files fileID -> Map(Chunks)
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> storedChunks = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>>();
				
	// TODO - USAR
	public static long usedStorage;
	public static long maxStorage;
	
	public static void addBackedUpFile(BackedUpFile file){
		nameToFileID.put(file.getFilePath(), file.getFileID());
		backedUpFiles.put(file.getFileID(), file);
	}
	
	public static void addBackedUpFileChunk(String fileID, int chunkNo, long size, int desiredReplicationDeg){
		backedUpFiles.get(fileID).addChunk(chunkNo, size, desiredReplicationDeg);
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
	
	public static ConcurrentHashMap <Integer, Chunk> getChunksBackedUpFile(String fileID){
		return backedUpFiles.get(fileID).getChunks();
	}
	
	public static void deleteBackedUpFile(String filePath, String fileID){
		nameToFileID.remove(filePath);
		backedUpFiles.remove(fileID);
	}
	
	public static void addBackedUpChunkReplication(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addReplication(chunkNo);
	}
	
	public static int getBackedUpChunkPerceivedReplication(String fileID, int chunkNo){
		return backedUpFiles.get(fileID).getChunkPerceivedReplication(chunkNo);
	}
	
	public static boolean hasStoredFileID(String fileID){
		return storedChunks.containsKey(fileID);
	}
	
	public static boolean hasStoredChunkNo(String fileID, int chunkNo){
		return storedChunks.get(fileID).containsKey(chunkNo);
	}
	
	public static void addStoredFile(String fileID){
		storedChunks.put(fileID, new ConcurrentHashMap<Integer, Chunk>());
	}
	
	public static void addStoredChunk(String fileID, int chunkNo, long size, int desiredReplicationDeg, int perceivedReplicationDeg){
		Chunk chunk = new Chunk(chunkNo, size, desiredReplicationDeg);
		chunk.setPerceivedReplicationDeg(perceivedReplicationDeg);
		storedChunks.get(fileID).put(chunkNo, chunk);
	}
	
	public static void removeStoredFile(String fileID){
		storedChunks.remove(fileID);
	}
	
	public static void removeStoredChunk(String fileID, int chunkNo){
		storedChunks.get(fileID).remove(chunkNo);
	}
	
	public static ConcurrentHashMap<Integer, Chunk> getStoredChunks(String fileID){
		return storedChunks.get(fileID);
	}
	
	// TODO - MODIFICAR APRA ATUALIZAR STOREDCHUNKS
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
		
		if(FileManager.hasStoredFileID(fileID))
			if(FileManager.hasStoredChunkNo(fileID, chunkNo))
				FileManager.storedChunks.get(fileID).get(chunkNo).addPerceivedReplicationDeg();
	}	
}
