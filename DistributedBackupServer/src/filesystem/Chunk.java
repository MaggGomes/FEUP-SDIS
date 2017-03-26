package filesystem;

public class Chunk {
	
	public static final int MAX_SIZE = 64000; // 64 KBytes
	private int chunkNo;
	private int replicationDeg;
	private int perceivedReplicationDeg;
	private byte[] data;
	
	public Chunk(int chunkNo, int fileID, int replicationDeg, byte[] data){
		this.chunkNo = chunkNo;
		this.setReplicationDeg(replicationDeg);
		this.setData(data);		
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(int chunkNo) {
		this.chunkNo = chunkNo;
	}

	public int getReplicationDeg() {
		return replicationDeg;
	}

	public void setReplicationDeg(int replicationDeg) {
		this.replicationDeg = replicationDeg;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	@Override
	public String toString(){
		return "CHUNK:" +
				"id:" + this.chunkNo + 
				" , size(KBytes): " + data.length + 
				" , replication degree: " + perceivedReplicationDeg;
	}

}
