package filesystem;

import java.util.HashMap;

public class File {
	
	private String fileID;
	private HashMap <Integer, Chunck> chunks;
	
	public File(String fileID){
		this.fileID = fileID;
		
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	public HashMap <Integer, Chunck> getChunks() {
		return chunks;
	}

	public void setChunks(HashMap <Integer, Chunck> chunks) {
		this.chunks = chunks;
	}

}
