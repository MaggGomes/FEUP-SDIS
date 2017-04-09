package filesystem;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import utilities.Message;

public class BackedUpFile implements Serializable{

	private static final long serialVersionUID = 1L;
	private String filePath;
	private String fileID;
	private String fileName;	
	private String extension;
	private boolean canExecute;
	private boolean canRead;
	private boolean canWrite;
	private long lastModified;
	private int desiredReplicationDeg;	
	private ConcurrentHashMap <Integer, Chunk> chunks;

	/**
	 * BackedUpFile constructor
	 * 
	 * @param filePath of the file
	 * @param replicationDeg of the file
	 */
	public BackedUpFile(String filePath, int replicationDeg){
		this.filePath = filePath;

		File file = new File(filePath);		
		this.fileName = file.getName();
		this.desiredReplicationDeg = replicationDeg;
		this.extension = fileName.substring(fileName.lastIndexOf(".")+1);
		this.canExecute = file.canExecute();
		this.canRead = file.canRead();
		this.canWrite = file.canWrite();
		this.lastModified = file.lastModified();
		this.fileID = createFileID();

		chunks = new ConcurrentHashMap<>();
	}

	/**
	 * Creates a fileID for the file
	 * 
	 * @return fileID
	 */
	public String createFileID(){		
		String id = fileName+
				Boolean.toString(canExecute)+
				Boolean.toString(canRead)+
				Boolean.toString(canWrite)+
				Long.toString(lastModified);		

		return Message.createHash(id);
	}

	/**
	 * Adds a new chunk
	 * 
	 * @param chunkNo of the chunk
	 * @param size of the chunk
	 * @param desiredReplicationDeg of the chunk
	 */
	public void addChunk(int chunkNo, long size, int desiredReplicationDeg){
		chunks.put(chunkNo, new Chunk(fileID, chunkNo, size, desiredReplicationDeg));
	}

	/**
	 * Increments the chunk's perceived replication degree
	 * 
	 * @param chunkNo of the chunk
	 */
	public void addReplication(int chunkNo){		
		chunks.get(chunkNo).addPerceivedReplicationDeg();
	}
	
	/**
	 * Reduces chunk's perceived replication degree
	 * 
	 * @param chunkNo of the chunk
	 */
	public void reduceReplication(int chunkNo){		
		chunks.get(chunkNo).reducePerceivedReplicationDeg();
	}

	/**
	 * Gets the chunk's perceived replication degree
	 * 
	 * @param chunkNo of the chunk
	 * @return chunk's perceived replication degree
	 */
	public int getChunkPerceivedReplication(int chunkNo){
		return chunks.get(chunkNo).getPerceivedReplicationDeg();
	}

	/**
	 * Gets the file pathname
	 * 
	 * @return pathname
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Gets the file's ID
	 * 
	 * @return ID
	 */
	public String getFileID() {
		return fileID;
	}

	/**
	 * Gets file's name
	 * 
	 * @return file's name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Gets the file's extension
	 * 
	 * @return extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Gets the file's desired replication degree
	 * 
	 * @return deisred replication degree
	 */
	public int getDesiredReplicationDeg() {
		return desiredReplicationDeg;
	}

	/**
	 * Verifies if the has execute permissions
	 * 
	 * @return true if has execute permissions, false otherwise
	 */
	public boolean isCanExecute() {
		return canExecute;
	}

	/**
	 * Verifies if the file has write permissions
	 * 
	 * @return true if the file has write permissions, false otherwise
	 */
	public boolean isCanWrite() {
		return canWrite;
	}

	/**
	 * Gets file last modified
	 * 
	 * @return last modified
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Verify if a file has "can read" property
	 * 
	 * @return true if has property "can read", false otherwise
	 */
	public boolean isCanRead() {
		return canRead;
	}

	/**
	 * Verifies if 2 files have same fileID
	 * 
	 * @param fileID to be compared
	 * @return true if they are equals, false otherwise
	 */
	public boolean equals(String fileID){
		return this.fileID.equals(fileID);
	}

	/**
	 * Gets the file chunk
	 * 
	 * @return chunks
	 */
	public ConcurrentHashMap <Integer, Chunk> getChunks() {
		return chunks;
	}

	/**
	 * Returns the backed up file info in string format
	 */
	public String toString(){
		if(chunks.size() == 0)
			return "";
		
		String state = "PATHNAME: "+
				this.filePath+"\nFILE ID: "+
				this.fileID+"\nDESIRED REPLICATION DEGREE: "+
				this.desiredReplicationDeg+
				"\n\n### BACKED UP CHUNKS ###";
		
		for(Chunk chunk: chunks.values())
			state+=chunk;

		return state;
	}
}
