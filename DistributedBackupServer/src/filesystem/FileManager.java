package filesystem;

import java.util.concurrent.ConcurrentHashMap;

public class FileManager {

	// Backed up files 
	// Hash for getting stored fileID  file pathname -> fileID
	public static ConcurrentHashMap<String, String> nameToFileID = new ConcurrentHashMap<String, String>();
	// fBacked up files fileID -> file info
	public static ConcurrentHashMap<String, BackedUpFile> backedUpFiles = new ConcurrentHashMap<String, BackedUpFile>();

	// Replication degree for each chunk
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> filesTrackReplication = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
	// Stored files fileID -> Map(Chunks)
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> storedChunks = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>>();

	// Put chunks prepared to be sent when reclaim protocol starts
	public static ConcurrentHashMap<String, Integer> chunksToSend = new ConcurrentHashMap<String, Integer>();

	/* Current max storage of the peer */
	public static float maxStorage = 6000; // KBytes

	/**
	 * Adds a file to the backed up files
	 * 
	 * @param file to be backed up
	 */
	public static void addBackedUpFile(BackedUpFile file){
		nameToFileID.put(file.getFilePath(), file.getFileID());
		backedUpFiles.put(file.getFileID(), file);
	}

	/**
	 * Adds a backed up chunk to the backed up chunks
	 * 
	 * @param fileID of the file parent of the chunk
	 * @param chunkNo of the chunk
	 * @param size of the chunk
	 * @param desiredReplicationDeg of the chunk
	 */
	public static void addBackedUpFileChunk(String fileID, int chunkNo, long size, int desiredReplicationDeg){
		backedUpFiles.get(fileID).addChunk(chunkNo, size, desiredReplicationDeg);
	}

	/**
	 * Verifies if a file is backed up by its file pathname
	 * 
	 * @param filePath of the file
	 * @return true if is backed up, false otherwise
	 */
	public static boolean hasBackedUpFilePathName(String filePath){	
		return nameToFileID.containsKey(filePath);
	}

	/**
	 * Verifies if a file is backed up by its fileID
	 * 
	 * @param fileID of the file
	 * @return is backed up, false otherwise
	 */
	public static boolean hasBackedUpFileID(String fileID){	
		return backedUpFiles.containsKey(fileID);
	}

	/**
	 * Gets the file's ID of a file providing its file pathname
	 * 
	 * @param filePath of the file
	 * @return the file ID of the file
	 */
	public static String getBackedUpFileID(String filePath) {
		return nameToFileID.get(filePath);
	}

	/**
	 * Gets the file's name of a the specified file
	 * 
	 * @param fileID of the file
	 * @return file's name
	 */
	public static String getBackedUpFileName(String fileID) {
		return backedUpFiles.get(fileID).getFileName();
	}

	/**
	 * Gets a ConcurrentHashMap with the backed up chunks of a file
	 * 
	 * @param fileID of the file
	 * @return ConcurrentHashMap  with chunks
	 */
	public static ConcurrentHashMap <Integer, Chunk> getChunksBackedUpFile(String fileID){
		return backedUpFiles.get(fileID).getChunks();
	}

	/**
	 * Deletes a backed up file
	 * 
	 * @param filePath of the file
	 * @param fileID of the file
	 */
	public static void deleteBackedUpFile(String filePath, String fileID){
		nameToFileID.remove(filePath);
		backedUpFiles.remove(fileID);
	}

	/**
	 * Increments the perceived replication degree of a chunk
	 * 
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 */
	public static void addBackedUpChunkReplication(String fileID, int chunkNo){
		backedUpFiles.get(fileID).addReplication(chunkNo);
	}

	/**
	 * Gets the perceived replication degree of a chunk
	 * 
	 * @param fileID of the parent chunk
	 * @param chunkNo of the chunk
	 * @return the perceived replication degree of the chunk
	 */
	public static int getBackedUpChunkPerceivedReplication(String fileID, int chunkNo){
		return backedUpFiles.get(fileID).getChunkPerceivedReplication(chunkNo);
	}

	/**
	 * Verifies if a 
	 * 
	 * @param fileID
	 * @return
	 */
	public static boolean hasStoredFileID(String fileID){
		return storedChunks.containsKey(fileID);
	}

	/**
	 * Verifies if a chunk is stored
	 * 
	 * @param fileID of the parent chunk
	 * @param chunkNo of the chunk
	 * @return true if the chunk is stored, false otherwise
	 */
	public static boolean hasStoredChunkNo(String fileID, int chunkNo){
		if(storedChunks.containsKey(fileID))
			if(storedChunks.get(fileID).containsKey(chunkNo))
				return true;			
		
		return false;
	}

	/**
	 * Verifies if the perceived replication degree of a chunk is lower than its desired replication degree
	 * 
	 * @param fileID of the parent file ID
	 * @param chunkNo of the chunk
	 * @return true if the perceived replication degree is lower than the desired replication degree
	 */
	public static boolean hasPerceveidedLowerDesired(String fileID, int chunkNo){
		return storedChunks.get(fileID).get(chunkNo).hasPerceveidedLowerDesired();
	}

	/**
	 * Gets the desired replication degree of a chunk
	 * 
	 * @param fileID of the parent chunk
	 * @param chunkNo of the chunk
	 * @return desired replication degree
	 */
	public static int getStoredDesiredReplicationDeg(String fileID, int chunkNo){
		return storedChunks.get(fileID).get(chunkNo).getDesiredReplicationDeg();
	}

	/**
	 * Adds a file to the stored files structure
	 * 
	 * @param fileID of the file
	 */
	public static void addStoredFile(String fileID){
		storedChunks.put(fileID, new ConcurrentHashMap<Integer, Chunk>());
	}

	/**
	 * Adds a chunk to the stored chunks structure
	 * 
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 * @param size of the chunk
	 * @param desiredReplicationDeg of the chunk
	 * @param perceivedReplicationDeg of the chunk
	 */
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
	 * Removes a stored chunk and updates the used storage
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

	/**
	 * Gets a structure with the stored chunks
	 * 
	 * @return chunks
	 */
	public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>>  getStoredFilesChunks(){
		return storedChunks;
	}
	
	/**
	 * Verifies if a chunk is being prepared to be sent
	 * 
	 * @param fileID
	 * @param chunkNo
	 * @return true if the chunk is being prepared to be sent, false otherwise
	 */
	public static boolean hasChunkToSend(String fileID, int chunkNo){
		if(chunksToSend.containsKey(fileID))
			if(chunksToSend.get(fileID).equals(chunkNo))
				return true;
		
		return false;
	}

	/**
	 * Adds a chunk to be send
	 * 
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 */
	public static void addChunkToSend(String fileID, int chunkNo){
		chunksToSend.put(fileID, chunkNo);
	}

	/**
	 * Removes a chunk thats was going to be sent
	 * 
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 */
	public static void removeChunkToSend(String fileID, int chunkNo){
		if(chunksToSend.containsKey(fileID))
			if(chunksToSend.get(fileID).equals(chunkNo)){
				//TODO - APAGAR
				System.out.println("removing");
				chunksToSend.remove(fileID, chunkNo);
			}
	}

	/**
	 * Gets the current perceived replication degree of a chunk
	 * 
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 * @return perceived replication degree of the chunk
	 */
	public static int getPerceivedReplicationDeg(String fileID, int chunkNo){	
		if(filesTrackReplication.containsKey(fileID)){
			if(!filesTrackReplication.get(fileID).containsKey(chunkNo)){
				filesTrackReplication.get(fileID).put(chunkNo, 0);	
			} 
		} else {
			filesTrackReplication.put(fileID, new ConcurrentHashMap<Integer, Integer>());
			filesTrackReplication.get(fileID).put(chunkNo, 0);
		}

		return filesTrackReplication.get(fileID).get(chunkNo);
	}

	/**
	 * Updates perceived replication degree of the backed up files
	 * 
	 * @param fileID of the file to be updated
	 * @param chunkNo of the chunk to be updated
	 */
	public static void updateBackedUpReplicationDeg(String fileID, int chunkNo) {

		/* Updates backed up files structure */
		addBackedUpChunkReplication(fileID, chunkNo);

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

	/**
	 * Updates perceived replication degree of the stored chunks
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
	 * @param fileID of the parent file
	 * @param chunkNo of the chunk
	 */
	public static void reduceReplicationDeg(String fileID, int chunkNo){
		if (FileManager.hasBackedUpFileID(fileID))
			backedUpFiles.get(fileID).reduceReplication(chunkNo);	

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
	 * Verifies if a new amount of storage space can be added to the used storage
	 * 
	 * @param space to be added
	 * @return true if the 
	 */
	public static boolean hasEnoughStorage(float space){

		float chunkSpace = (float)space/1000;

		if((getUsedStorage()+chunkSpace) > maxStorage)
			return false;
		else
			return true;
	}

	public static float getUsedStorage(){
		float storage = 0;

		for(String fileID: storedChunks.keySet())
			for(Chunk chunk: storedChunks.get(fileID).values())
				storage += chunk.getSize();

		return storage;
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
			state += "\n\n##### STORED CHUNKS #####";

			for(String fileID: storedChunks.keySet()){
				state += "\n\nFILE ID: "+fileID;

				for(Chunk chunk: storedChunks.get(fileID).values())				
					state += "\nID: "+chunk.getNumber()+
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
		String space = "\n\nMAX STORAGE CAPACITY: "+maxStorage+" KBytes  |  USED STORAGE: "+getUsedStorage()+" KBytes";

		return getBackedUpToString()+getStoredChunksToString()+space;
	}
}
