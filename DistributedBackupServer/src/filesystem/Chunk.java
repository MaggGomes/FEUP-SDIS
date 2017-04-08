package filesystem;

import java.io.Serializable;

public class Chunk implements Serializable{

	private static final long serialVersionUID = 2L;
	private int number;
	private float size; // KBytes
	private int desiredReplicationDeg;
	private int perceivedReplicationDeg;
	
	public Chunk(int number, long size, int desiredReplicationDeg){
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

	public int getPerceivedReplicationDeg() {
		return perceivedReplicationDeg;
	}
	
	public String toString(){
		return "\nID: "+this.number+"  |  PERCEIVED REPLICATION DEGREE: "+this.perceivedReplicationDeg;
	}
}
