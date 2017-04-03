package utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

public class Message {
	private String messageType;
	private String version;
	private String senderID;
	private String fileID;
	private String chunkNo;
	private String replicationDeg; // 1 char length
	private String header;
	private byte[] body;
	private byte[] message;

	// Message types
	public static final String PUTCHUNK = "PUTCHUNK";
	public static final String STORED = "STORED";
	public static final String GETCHUNK = "GETCHUNK";
	public static final String CHUNK = "CHUNK";
	public static final String DELETE = "DELETE";
	public static final String REMOVED = "REMOVED";
	
	public static final int MIN_DELAY = 0;
	public static final int MAX_DELAY = 400;

	public static final int MAX_HEADER_SIZE = 1000;
	public static final String CRLF = " \r\n\r\n";
	public static final String SPACE = " ";

	public Message(String header, byte[] body){

		this.header = header;
		this.body = body;			
		this.createMessage();
	}

	// PUTCHUNK
	public Message(String messageType, String version, String senderId, String fileId, int chunkNo,
			String replicationDeg, byte[] body) {		

		this.messageType = messageType;
		this.version = version;
		this.senderID = senderId;
		this.fileID = fileId;
		this.chunkNo = Integer.toString(chunkNo);
		this.replicationDeg = replicationDeg;
		this.body = body;

		this.createHeader();				
		this.createMessage();
	}

	// CHUNK
	public Message(String messageType, String version, String senderId, String fileId, int chunkNo, byte[] body) {		

		this.messageType = messageType;
		this.version = version;
		this.senderID = senderId;
		this.fileID = fileId;
		this.chunkNo = Integer.toString(chunkNo);
		this.body = body;

		this.createHeader();				
		this.createMessage();
	}

	// STORED, GETCHUNK, REMOVED
	public Message(String messageType, String version, String senderId, String fileId, String chunkNo) {		

		this.messageType = messageType;
		this.version = version;
		this.senderID = senderId;
		this.fileID = fileId;
		this.chunkNo = chunkNo;
		this.body = new byte[0];

		this.createHeader();				
		this.createMessage();
	}
	
	// DELETE
	public Message(String messageType, String version, String senderId, String fileId) {		

		this.messageType = messageType;
		this.version = version;
		this.senderID = senderId;
		this.fileID = fileId;
		this.body = new byte[0];

		this.createHeader();				
		this.createMessage();
	}

	public Message(DatagramPacket packet){

		this.message = Arrays.copyOf(packet.getData(), packet.getLength());

		String msg = new String(message, packet.getOffset(), message.length);
		String[] messageFields = msg.split("( \\r\\n\\r\\n)"); // CRLF

		header = new String(messageFields[0].getBytes(), StandardCharsets.US_ASCII);

		// Parses header
		parseHeader();

		// Reads the body of the message (chunk data)
		if(messageType.equals(Message.PUTCHUNK) || messageType.equals(Message.CHUNK)){
			body = Arrays.copyOfRange(message, header.length()+CRLF.length(), packet.getLength());
		}
	}
	
	/**
	 * Creates the header of the message
	 */
	public void createHeader(){

		header = "";
		
		if (this.messageType != null)
			header += this.messageType+SPACE;
		
		if (this.version!= null)
			header += this.version+SPACE;
		
		if (this.senderID != null)
			header += this.senderID+SPACE;
		
		if (this.fileID != null)
			header += this.fileID+SPACE;
		
		if (this.chunkNo != null)
			header += this.chunkNo+SPACE;
		
		if (this.replicationDeg != null)
			header += this.replicationDeg+SPACE;
	}

	public void createMessage(){
		ByteArrayOutputStream byteToStream = new ByteArrayOutputStream();

		try {
			byteToStream.write(header.getBytes(StandardCharsets.US_ASCII), 0, header.length());
			byteToStream.write(CRLF.getBytes());
			byteToStream.write(body, 0, body.length);
		} catch (IOException e) {
			e.printStackTrace();
		}		

		this.message = byteToStream.toByteArray();
	}

	// Verificar existência de erros??
	public void parseHeader(){

		String[] headerFields = header.split("\\s+");

		messageType = headerFields[0];
		version = headerFields[1];
		senderID = headerFields[2];
		fileID = headerFields[3];

		try {
			switch(messageType){
			case Message.PUTCHUNK:
				chunkNo = headerFields[4];
				replicationDeg = headerFields[5];
				break;
			case Message.STORED:
				chunkNo = headerFields[4];
				break;
			case Message.GETCHUNK:
				chunkNo = headerFields[4];
				break;
			case Message.CHUNK:
				chunkNo = headerFields[4];
				break;
			case Message.DELETE:
				break;
			case Message.REMOVED:
				chunkNo = headerFields[4];
				break;
			default:
				System.out.println("Invalid message type: messageType");
				break;		
			}			
		} catch(ArrayIndexOutOfBoundsException e){
			e.printStackTrace();
		}		
	}

	/**
	 * Hashes input using SHA-256
	 * 
	 * @param fileId
	 * @return hash
	 */
	public static String createHash(String fileId) {
		MessageDigest dig = null;

		try {
			dig = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		byte[] hash = new byte[0];

		hash = dig.digest(fileId.getBytes(StandardCharsets.UTF_8));

		return DatatypeConverter.printHexBinary(hash);
	}	

	public String getHeader(){
		return header;
	}

	public byte[] getBody(){
		return body;
	}

	public byte[] getMessage(){
		return message;
	}

	public String getMessageType() {
		return messageType;
	}

	public String getSenderID(){
		return senderID;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileId(String fileId) {
		this.fileID = Message.createHash(fileId);
	}

	public String getChunkNo() {
		return chunkNo;
	}

	public String getReplicationDeg() {
		return replicationDeg;
	}
}
