package utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Message {
	private String messageType; // variable length
	private String version; // <number>.<number>
	private String senderId; // variable length
	private String fileId; // 32 bytes -> 64 chars
	private String chunkNo; // variable length(max = 6)
	private String replicationDeg; // 1 char length
	private String body; // 64kBytes length

	public Message(String messageType, String version, String senderId, String fileId, String chunkNo,
			String replicationDeg, String body) {
		this.messageType = messageType;
		this.version = version;
		this.senderId = senderId;
		this.fileId = this.createHash(fileId);
		this.chunkNo = chunkNo;
		this.replicationDeg = replicationDeg;

		this.body = body;
	}

	private String createHash(String fileId) {
		MessageDigest dig = null;
		try {
			dig = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		byte[] ret = dig.digest(fileId.getBytes(StandardCharsets.UTF_8));

		return ret.toString();
	}

	@Override
	public String toString() {
		String ret = "";
		
		//message header
		
		ret += this.messageType;
		ret += ((this.messageType == null) ? "" : " ");

		ret += this.version;
		ret += ((this.version == null) ? "" : " ");

		ret += this.senderId;
		ret += ((this.senderId == null) ? "" : " ");

		ret += this.fileId;
		ret += ((this.fileId == null) ? "" : " ");

		ret += this.chunkNo;
		ret += ((this.chunkNo == null) ? "" : " ");

		ret += this.replicationDeg;
		ret += ((this.replicationDeg == null) ? "" : " ");
		
		ret += "\r\n" + "\r\n";

		//message body
		
		ret += this.body;

		return ret;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = this.createHash(fileId);
	}

	public String getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(String chunkNo) {
		this.chunkNo = chunkNo;
	}

	public String getReplicationDeg() {
		return replicationDeg;
	}

	public void setReplicationDeg(String replicationDeg) {
		this.replicationDeg = replicationDeg;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	

}
