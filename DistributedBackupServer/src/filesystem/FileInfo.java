package filesystem;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import utilities.Message;

public class FileInfo {
	
	private String filePath;
	private String fileID;
	private String fileName;	
	private String extension;
	private boolean canExecute;
	private boolean canRead;
	private boolean canWrite;
	private long lastModified;
	private int replicationDeg = 0;		
	private ConcurrentHashMap <Integer, Integer> chunks;
	
	public FileInfo(String filePath, int replicationDeg){
		this.filePath = filePath;
		
		File file = new File(filePath);		
		this.fileName = file.getName();
		this.replicationDeg = replicationDeg;
		this.extension = fileName.substring(fileName.lastIndexOf(".")+1);
		this.canExecute = file.canExecute();
		this.canRead = file.canRead();
		this.canWrite = file.canWrite();
		// TODO - ACRESCENTAR OWNER
		this.lastModified = file.lastModified();
		this.fileID = createFileID();
		
		chunks = new ConcurrentHashMap<>();
	}
	
	public FileInfo(String filePath){
		File file = new File(filePath);
		
		this.fileName = file.getName();
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
	
	public void addChunk(int chunkNo){
		chunks.put(chunkNo, 0);
	}
	
	public void addReplication(int chunkNo){		
		int chunkrep = chunks.get(chunkNo);		
		chunks.put(chunkNo, chunkrep+1);
	}
	
	public int getChunkReplication(int chunkNo){
		return chunks.get(chunkNo);
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

	public int getReplicationDeg() {
		return replicationDeg;
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

	public boolean isCanRead() {
		return canRead;
	}
	
	public boolean equals(String fileID){
		return this.fileID.equals(fileID);
	}

	public ConcurrentHashMap <Integer, Integer> getChunks() {
		return chunks;
	}
}
