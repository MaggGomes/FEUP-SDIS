package cli;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import peer.IPeerInterface;

public class TestApp {	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("dasdnjaksndja");
		
		// TODO - APAGAR - CODIGO PARA TESTAR
		
		try {			
			Registry registry = LocateRegistry.getRegistry();
			IPeerInterface stub =  (IPeerInterface) registry.lookup(args[0]);
			System.out.println(stub.state());
			
		} catch(Exception e) {
			e.printStackTrace();			
		}

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
