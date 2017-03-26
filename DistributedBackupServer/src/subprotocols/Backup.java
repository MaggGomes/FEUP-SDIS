package subprotocols;

import utilities.Message;

public class Backup extends Protocol{
	public Backup(){
		
	}
	
	
	
	public Message getPutChunkMsg(String version, String senderId, String fileId, String chunkNo, String replicationDeg, String body){
		return new Message("PUTCHUNK", version, senderId, fileId, chunkNo, replicationDeg, body);
	}
	
	public Message getStoredMsg(String version, String senderId, String fileId, String chunkNo){
		return new Message("STORED", version, senderId, fileId, chunkNo, null, null);
	}
	
	
}
