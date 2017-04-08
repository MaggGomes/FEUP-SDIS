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
	public static float maxStorage = 20000; // KBytes
	public static float usedStorage = 5000;	// KBytes
	
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
	
	public static boolean hasPerceveidedLowerDesired(String fileID, int chunkNo){
		return storedChunks.get(fileID).get(chunkNo).hasPerceveidedLowerDesired();
	}
	
	public static int getStoredDesiredReplicationDeg(String fileID, int chunkNo){
		return storedChunks.get(fileID).get(chunkNo).getDesiredReplicationDeg();
	}
	
	public static void addStoredFile(String fileID){
		storedChunks.put(fileID, new ConcurrentHashMap<Integer, Chunk>());
	}
	
	public static void addStoredChunk(String fileID, int chunkNo, long size, int desiredReplicationDeg, int perceivedReplicationDeg){
		Chunk chunk = new Chunk(fileID, chunkNo, size, desiredReplicationDeg);
		chunk.setPerceivedReplicationDeg(perceivedReplicationDeg);
		storedChunks.get(fileID).put(chunkNo, chunk);
	}
	
	
	/**
	 * Removes a stored file
	 * 
	 * @param fileID of the file
	 */
	public static void removeStoredFile(String fileID){
		storedChunks.remove(fileID);
	}
	
	/**
	 * Removes a stored chunk
	 * 
	 * @param fileID of the file
	 * @param chunkNo of the chunk
	 */
	public static void removeStoredChunk(String fileID, int chunkNo){
		storedChunks.get(fileID).remove(chunkNo);
	}
	
	/**
	 * Gets the chunks of a file
	 * 
	 * @param fileID of the file
	 * @return chunks
	 */
	public static ConcurrentHashMap<Integer, Chunk> getStoredChunks(String fileID){
		return storedChunks.get(fileID);
	}
	
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>>  getStoredFilesChunks(){
		return storedChunks;
	}
	
	/**
	 * Updates perceived replication degree of the chunks
	 * 
	 * @param fileID of the file to be updated
	 * @param chunkNo of the chunk to be updated
	 */
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
	
	/**
	 * Reduces the perceived replication degree of the specified chunk
	 * 
	 * @param fileID
	 * @param chunkNo
	 */
	public static void reduceReplicationDeg(String fileID, int chunkNo){
		if(FileManager.filesTrackReplication.containsKey(fileID)){
			if(!FileManager.filesTrackReplication.get(fileID).containsKey(chunkNo)){
				return;	
			} 
		} else {
			return;
		}
		
		int chunkrep = FileManager.filesTrackReplication.get(fileID).get(chunkNo);
		FileManager.filesTrackReplication.get(fileID).put(chunkNo, chunkrep-1);
		
		if(FileManager.hasStoredFileID(fileID))
			if(FileManager.hasStoredChunkNo(fileID, chunkNo))
				FileManager.storedChunks.get(fileID).get(chunkNo).reducePerceivedReplicationDeg();		
	}
	
	/**
	 * Gets the max storage capacity of the peer
	 * 
	 * @return max storage capacity
	 */
	public static float getMaxStorage(){
		return maxStorage;
	}
	
	/**
	 * Gets the used storage of the peer
	 * 
	 * @return used storage
	 */
	public static float getUsedStorage(){
		return usedStorage;
	}
	
	/**
	 * Converts backed up files information to a string
	 * 
	 * @return backed up files information in string format
	 */
	public static String getBackedUpToString(){
		String state="";
		
		if(backedUpFiles.size() > 0){
			state += "##### BACKED UP FILES #####";
			
			for (BackedUpFile file: backedUpFiles.values())
				state += "\n\n"+file;
		}	
		
		return state;
	}
	
	/**
	 * Converts stored chunks information to a string
	 * 
	 * @return stored chunks information in string format
	 */
	public static String getStoredChunksToString(){
		String state="";
		
		if(storedChunks.size() > 0){
			state+="\n\n##### STORED CHUNKS #####";
			
			for(String fileID: storedChunks.keySet()){
				state+="\n\nFILE ID: "+fileID;
				
				for(Chunk chunk: storedChunks.get(fileID).values())				
				state+="\nID: "+chunk.getNumber()+
				"  |  SIZE: "+chunk.getSize()+
				" KBytes  |  PERCEIVED REPLICATION DEGREE: "+chunk.getPerceivedReplicationDeg();
			}			
		}
			
		return state;
	}
	
	/**
	 * Converts files information to a string
	 * 
	 * @return files information in string format
	 */
	public static String getState(){
		String space = "\n\nMAX STORAGE CAPACITY: "+maxStorage+"  |  USED STORAGE:"+usedStorage;
		
		return getBackedUpToString()+getStoredChunksToString()+space;
	}
}
