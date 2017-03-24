package cli;

public class TestApp {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("dasdnjaksndja");

	}
	
	// TODO - FALTA IMPLEMENTAR
	public static boolean checkSubProtocol(String[] args){
		
		switch(args[2]){
		case "BACKUP":
			break;
		case "RESTORE":
			break;
		case "DELETE":
			break;
		case "RECLAIM":
			break;
		case "STATE":
			break;
		default:
			System.out.println("Available possibilities: BACKUP, RESTORE, DELETE, RECLAIM, STATE");
			return false;	
		}
		
		return true;
	}

}
