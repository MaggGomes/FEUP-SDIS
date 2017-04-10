package cli;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
	
	/**
	 * Verifies if the inputs are valid
	 * 
	 * @param array with the inputs to be verified
	 * @return true if all inputs are valid, false otherwise
	 */
	public static boolean validateInput(String[] args){		
		if (args.length < 2 || args.length > 4){
			return false;
		}
		
		/* Verifies if the specified command is available */
		switch(args[1]){
		case "BACKUP":
			if (args.length == 4 && Utilities.fileExists(args[2])){
				peerAccessPoint = args[0];
				operation = "BACKUP";
				fileName = args[2];
				replicationDeg = Integer.parseInt(args[3]);
			} else
				return false;			
			break;
		case "BACKUPENH":
			if (args.length == 4 && Utilities.fileExists(args[2])){
				peerAccessPoint = args[0];
				operation = "BACKUPENH";
				fileName = args[2];
				replicationDeg = Integer.parseInt(args[3]);
			} else
				return false;			
			break;
		case "RESTORE":
			if (args.length == 3){
				peerAccessPoint = args[0];
				operation = "RESTORE";
				fileName = args[2];
			}
			else
				return false;
			break;
		case "RESTOREENH":
			if (args.length == 3){
				peerAccessPoint = args[0];
				operation = "RESTOREENH";
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
		case "DELETEENH":
			if (args.length == 3){
				peerAccessPoint = args[0];
				operation = "DELETEENH";
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
					System.out.println("ERROR: Invalid input! The value specified for the RECLAIM operation is not valid.\n");
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
	
	/**
	 * Initiates the initiator peer with the requested service
	 */
	public static void init(){
		
		try {			
			Registry registry = LocateRegistry.getRegistry("localhost");
			initiatorPeer =  (IPeerInterface) registry.lookup(peerAccessPoint);
			
			switch(operation){
			case "BACKUP":
				initiatorPeer.backup("1.0", fileName, replicationDeg);
				break;
			case "BACKUPENH":
				initiatorPeer.backup("2.0", fileName, replicationDeg);
				break;
			case "RESTORE":
				initiatorPeer.restore("1.0", fileName);
				break;
			case "RESTOREENH":
				initiatorPeer.restore("2.0", fileName);
				break;
			case "DELETE":
				initiatorPeer.delete("1.0", fileName);
				break;
			case "DELETEENH":
				initiatorPeer.delete("2.0", fileName);
				break;
			case "RECLAIM":
				initiatorPeer.reclaim(spaceSize);
				break;
			case "STATE":
				System.out.println(initiatorPeer.state());
				break;			
			}
			
		} catch(NotBoundException | RemoteException e) {
			e.printStackTrace();			
		} 
	}

}
