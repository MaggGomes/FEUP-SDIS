package filesystem;

import java.io.Serializable;

public class Chunk implements Comparable<Chunk>, Serializable{

	private static final long serialVersionUID = 2L;
	private String fileID;
	private int number;
	private float size; // KBytes
	private int desiredReplicationDeg;
	private int perceivedReplicationDeg;
	
	public Chunk(String fileID, int number, long size, int desiredReplicationDeg){
		this.fileID = fileID;
		this.number = number;
		this.size = (float)size/1000;
		this.desiredReplicationDeg = desiredReplicationDeg;
		this.perceivedReplicationDeg = 0;
	}

	public int getNumber() {
		return number;
	}

	public float getSize() {
		return size;
	}

	public int getDesiredReplicationDeg() {
		return desiredReplicationDeg;
	}
	
	public void setPerceivedReplicationDeg(int perceivedReplicationDeg){
		this.perceivedReplicationDeg = perceivedReplicationDeg;
	}
	
	public void addPerceivedReplicationDeg(){
		perceivedReplicationDeg++;
	}
	
	public void reducePerceivedReplicationDeg(){
		perceivedReplicationDeg--;
	}
	
	public boolean hasPerceveidedLowerDesired(){
		return perceivedReplicationDeg < desiredReplicationDeg;
	}
	
	public int getDiffPerceivedDesiredDeg(){
		return desiredReplicationDeg-perceivedReplicationDeg;
	}

	public int getPerceivedReplicationDeg() {
		return perceivedReplicationDeg;
	}
	
	public String toString(){
		return "\nID: "+this.number+"  |  PERCEIVED REPLICATION DEGREE: "+this.perceivedReplicationDeg;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	@Override
	public int compareTo(Chunk chunk) {
		if (this.getDiffPerceivedDesiredDeg() == chunk.getDiffPerceivedDesiredDeg())
			return 0;
		else if(this.getDiffPerceivedDesiredDeg() > chunk.getDiffPerceivedDesiredDeg())
			return 1;
		else
			return -1;
	}
}
