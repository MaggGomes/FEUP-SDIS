package utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import peer.Peer;

public class Utilities {
	
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
	
	public static boolean fileExists(String path){		
		
		Path filePath = Paths.get(path);
		
		if(Files.exists(filePath))
			return true;
		else{
			System.out.println("Invalid input: File does not exist!");
			return false;
		}
				
	}
	
	public static String createDir(String serverID, String fileID){
		return serverID+"/"+Peer.BACKUP+fileID+"/";
	}
	
	public static String createPath(String serverID, String fileID, String chunkNo){
		return createDir(serverID, fileID)+chunkNo;
	}
}
