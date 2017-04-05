package filesystem;

public class Chunk {

	private long size; // KBytes
	private int desiredReplicationDeg;
	private int perceivedReplicationDeg;
	
	public Chunk(long size, int desiredReplicationDeg){
		this.size = size;
		perceivedReplicationDeg = 0;
	}
}
