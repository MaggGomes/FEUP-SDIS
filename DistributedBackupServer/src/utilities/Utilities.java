package utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import peer.Peer;

public class Utilities {
	
	/**
	 * Verifies if a string is an Integer
	 * 
	 * @param str to be verified
	 * @return true if is a Integer, false otherwise
	 */
	public static boolean isInteger(String str){
		if(str.matches("\\d+"))
			return true;
		
		return false;
	}
	
	/**
	 * Creates a random number between min and max
	 * 
	 * @param min
	 * @param max
	 * @return random number
	 */
	public static int randomNumber(int min, int max){
		return new Random().nextInt(max-min)+min;
	}
	
	/**
	 * Verifies if a file's pathname exists
	 * 
	 * @param path of the file
	 * @return true if the file's pathname exists, false otherwise
	 */
	public static boolean fileExists(String path){		
		
		Path filePath = Paths.get(path);
		
		if(Files.exists(filePath))
			return true;
		else{
			System.out.println("Invalid input: File does not exist!");
			return false;
		}				
	}
	
	/**
	 * Creates a backup directory
	 * 
	 * @param serverID of the peer
	 * @param fileID of the file
	 * @return directory
	 */
	public static String createBackupDir(String serverID, String fileID){
		return serverID+"/"+Peer.BACKUP+fileID+"/";
	}
	
	/**
	 * Creates a backup chunk pathname
	 * 
	 * @param serverID of the peer
	 * @param fileID of the chunk's parent file
	 * @param chunkNo of the chunk
	 * @return chunk's pathname
	 */
	public static String createBackupPath(String serverID, String fileID, String chunkNo){
		return createBackupDir(serverID, fileID)+chunkNo;
	}
	
	/**
	 * Create a file restore pathname
	 * 
	 * @param serverID of the peer
	 * @param fileID of the file
	 * @param fileName of the file
	 * @return file's restore pathname
	 */
	public static String createRestorePath(String serverID, String fileID, String fileName){
		return serverID+"/"+Peer.RESTORED+fileID+"/"+fileName;
	}
	
	/**
	 * Creates data's pathname
	 * 
	 * @param serverID of the peer
	 * @return data's pathname
	 */
	public static String createDataPath(String serverID){
		return serverID+"/"+Peer.DATA;
	}
}
