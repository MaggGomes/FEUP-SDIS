package filesystem;

import java.io.Serializable;

public class Chunk implements Comparable<Chunk>, Serializable{

	private static final long serialVersionUID = 2L;
	private String fileID;
	private int number;
	private float size; /* KBytes */
	private int desiredReplicationDeg;
	private int perceivedReplicationDeg;
	
	/**
	 * Chunk's constructor
	 * 
	 * @param fileID of the parent file
	 * @param number of the chunk
	 * @param size of the chunk
	 * @param desiredReplicationDeg of the chunk
	 */
	public Chunk(String fileID, int number, long size, int desiredReplicationDeg){
		this.fileID = fileID;
		this.number = number;
		this.size = (float)size/1000;
		this.desiredReplicationDeg = desiredReplicationDeg;
		this.perceivedReplicationDeg = 0;
	}

	/**
	 * Gets chunk's number
	 * 
	 * @return number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Gets chunk's size
	 * 
	 * @return size
	 */
	public float getSize() {
		return size;
	}

	/**
	 * Gets chunks's desired replication degree
	 * 
	 * @return desired replication degree
	 */
	public int getDesiredReplicationDeg() {
		return desiredReplicationDeg;
	}
	
	/**
	 * Sets the chunk's perceived replication degree
	 * 
	 * @param perceivedReplicationDeg
	 */
	public void setPerceivedReplicationDeg(int perceivedReplicationDeg){
		this.perceivedReplicationDeg = perceivedReplicationDeg;
	}
	
	/**
	 * Increments chunk's perceived replication degree
	 */
	public void addPerceivedReplicationDeg(){
		perceivedReplicationDeg++;
	}
	
	/**
	 * Reduces chunk's perceived replication degree
	 */
	public void reducePerceivedReplicationDeg(){
		perceivedReplicationDeg--;
	}
	
	/**
	 * Verifies if the chunk has perceived replication degree lower than the desired replication degree
	 * 
	 * @return
	 */
	public boolean hasPerceveidedLowerDesired(){
		return perceivedReplicationDeg < desiredReplicationDeg;
	}
		
	/**
	 * Gets the the perceived and desired replication degree difference
	 * 
	 * @return difference result
	 */
	public int getDiffPerceivedDesiredDeg(){
		return desiredReplicationDeg-perceivedReplicationDeg;
	}

	/**
	 * Gets the chunk's perceived replication degree
	 * 
	 * @return perceived replication degree
	 */
	public int getPerceivedReplicationDeg() {
		return perceivedReplicationDeg;
	}
	
	/**
	 * Gets fileID of the chunk's parent file 
	 * 
	 * @return
	 */
	public String getFileID() {
		return fileID;
	}
	
	/**
	 * Converts chunk to string format
	 * 
	 * @return chunk in string format
	 */
	public String toString(){
		return "\nID: "+this.number+"  |  PERCEIVED REPLICATION DEGREE: "+this.perceivedReplicationDeg;
	}	

	/**
	 * Comparator of a Chunk element that has in account the difference between the the perceived and desired replication degree, and also its size
	 * 
	 * @param chunk to be compared
	 */
	@Override
	public int compareTo(Chunk chunk) {
		if (this.getDiffPerceivedDesiredDeg() == chunk.getDiffPerceivedDesiredDeg()){
			if(this.getPerceivedReplicationDeg() < chunk.getPerceivedReplicationDeg())
				return 0;
			else
				return 1;
		} 
		
		else if (this.getDiffPerceivedDesiredDeg() > chunk.getDiffPerceivedDesiredDeg())
			return -1;
		else
			return 2;
	}
}
