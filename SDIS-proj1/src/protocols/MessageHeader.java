package protocols;

public class MessageHeader {
	public char[] messageType; //variable length
	public char[] version; // <number>.<number>
	public char[] senderId; // variable length
	public char[] fileId; //32 bytes -> 64 chars
	public char[] chunkNo; // variable length(max = 6)
	public char[] replicationDeg; // 1 char length
	
}
