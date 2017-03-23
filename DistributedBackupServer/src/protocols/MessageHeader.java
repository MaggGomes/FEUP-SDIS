package protocols;

public class MessageHeader {
	public char[] messageType; //variable length
	public char[] version; // <number>.<number>
	public char[] senderId; // variable length
	public char[] fileId; //32 bytes -> 64 chars
	public char[] chunkNo; // variable length(max = 6)
	public char[] replicationDeg; // 1 char length

	public MessageHeader(String messageType, String version, String senderId,String fileId,String chunkNo,String replicationDeg){

	}

	public char[] getMessageType() {
		return messageType;
	}
	public void setMessageType(char[] messageType) {
		this.messageType = messageType;
	}
	public char[] getVersion() {
		return version;
	}
	public void setVersion(char[] version) {
		this.version = version;
	}
	public char[] getSenderId() {
		return senderId;
	}
	public void setSenderId(char[] senderId) {
		this.senderId = senderId;
	}
	public char[] getFileId() {
		return fileId;
	}
	public void setFileId(char[] fileId) {
		this.fileId = fileId;
	}
	public char[] getChunkNo() {
		return chunkNo;
	}
	public void setChunkNo(char[] chunkNo) {
		this.chunkNo = chunkNo;
	}
	public char[] getReplicationDeg() {
		return replicationDeg;
	}
	public void setReplicationDeg(char[] replicationDeg) {
		this.replicationDeg = replicationDeg;
	}
}
