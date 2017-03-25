package utilities;

public class Utilities {
	
	public static boolean isInteger(String str){
		if(str.matches("\\d+"))
			return true;
		
		return false;
	}

}
