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

	public String createFileID(){		
		String id = fileName+
				Boolean.toString(canExecute)+
				Boolean.toString(canRead)+
				Boolean.toString(canWrite)+
				Long.toString(lastModified);		

		return Message.createHash(id);
	}

	public void addChunk(int chunkNo, long size, int desiredReplicationDeg){
		chunks.put(chunkNo, new Chunk(fileID, chunkNo, size, desiredReplicationDeg));
	}

	public void addReplication(int chunkNo){		
		chunks.get(chunkNo).addPerceivedReplicationDeg();
	}
	
	public void reduceReplication(int chunkNo){		
		chunks.get(chunkNo).reducePerceivedReplicationDeg();
	}

	public int getChunkPerceivedReplication(int chunkNo){
		return chunks.get(chunkNo).getPerceivedReplicationDeg();
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileID() {
		return fileID;
	}

	public String getFileName() {
		return fileName;
	}

	public String getExtension() {
		return extension;
	}

	public int getDesiredReplicationDeg() {
		return desiredReplicationDeg;
	}

	public boolean isCanExecute() {
		return canExecute;
	}

	public boolean isCanWrite() {
		return canWrite;
	}

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

	public ConcurrentHashMap <Integer, Chunk> getChunks() {
		return chunks;
	}

	/**
	 * Returns the backed up file info in string format
	 */
	public String toString(){
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
