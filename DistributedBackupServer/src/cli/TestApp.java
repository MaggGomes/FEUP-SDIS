package cli;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import peer.IPeerInterface;
import utilities.Utilities;

public class TestApp {
	
	private static String peerAccessPoint;
	private static String operation;
	private static String fileName;
	private static int spaceSize;
	private static int replicationDeg;
	private static IPeerInterface initiatorPeer;
	
	// TODO TERMINAR
	public static void main(String[] args) {
		
		if(!validateInput(args)){
			System.out.println("USAGE: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
			System.out.println("<peer_ap>: Peer's access point");
			System.out.println("<sub_protocol>: BACKUP | RESTORE | DELETE | RECLAIM | STATE");
			System.out.println("<opnd_1>: Path name of file | Amount of space to reclaim (in KByte)");
			System.out.println("<opnd_2>: Integer that specified replication degree (BACKUP only)");
			System.out.println("\nExiting...");
			
			return;
		}
	
		init();
	}
	
	//TODO CORRIGIR - VERIFICAR SITUAÇÃO DOS INPUTS
	public static boolean validateInput(String[] args){
		
		System.out.println(args[1]);
		
		if (args.length < 2 || args.length > 4){
			return false;
		}
		
		// Verify if the specified command is available
		switch(args[1]){
		case "BACKUP":
			if (args.length == 4){
				peerAccessPoint = args[0];
				operation = "BACKUP";
				fileName = args[2];
				replicationDeg = Integer.parseInt(args[3]);
			} else
				return false;			
			break;
		case "RESTORE":
			if (args.length == 4){
				peerAccessPoint = args[0];
				operation = "RESTORE";
				fileName = args[2];
			}
			else
				return false;
			break;
		case "DELETE":
			if (args.length == 3){
				peerAccessPoint = args[0];
				operation = "DELETE";
				fileName = args[2];
			} else
				return false;
			break;
		case "RECLAIM":
			if (args.length == 3){
				peerAccessPoint = args[0];
				operation = "RECLAIM";
				if(Utilities.isInteger(args[2])){
					spaceSize = Integer.parseInt(args[2]);
				} else {
					System.out.println("ERROR: Invalid input The value specified for the RECLAIM operation is not valid.\n");
					
					return false;
				}
			}
			else
				return false;
			break;
		case "STATE":
			if (args.length == 2){				
				peerAccessPoint = args[0];
				operation= "STATE";
			}				
			else
				return false;
			break;
		default:
			System.out.println("Invalid option. Available options: BACKUP, RESTORE, DELETE, RECLAIM, STATE");
			return false;	
		}		
		
		return true;
	}
	
	public static void init(){
		
		try {			
			Registry registry = LocateRegistry.getRegistry("localhost");
			initiatorPeer =  (IPeerInterface) registry.lookup(peerAccessPoint);
			
			switch(operation){
			case "BACKUP":
				initiatorPeer.backup(fileName, replicationDeg);
				break;
			case "RESTORE":
				initiatorPeer.restore(fileName);
				break;
			case "DELETE":
				initiatorPeer.delete(fileName);
				break;
			case "RECLAIM":
				initiatorPeer.reclaim(spaceSize);
				break;
			case "STATE":
				initiatorPeer.state();
				break;			
			}
			
		} catch(Exception e) {
			e.printStackTrace();			
		}
	}

}