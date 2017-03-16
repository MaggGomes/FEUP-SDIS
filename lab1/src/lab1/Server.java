package lab1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

public class Server {
	private int BUFFER_SIZE = 256;
	
	private DatagramSocket socket;
	private InetAddress addr;
	
	
	private HashMap<String,String> db; //will contain the database of the application (plate_number, owner:_name)
	
	public static void main(String[] args) throws IOException{
		if(args.length != 1) {
			System.out.println("Wrong number of arguments!");
			return;
		}
		
		try{
			Server srv = new Server(args[0]);
			srv.begin();
		}catch(SocketException e){
			System.out.println("Not able to open the server!");
		}		
		
	}
	
	public Server(String port) throws SocketException{
		this.socket = new DatagramSocket(Integer.parseInt(port));
		this.db = new HashMap<String,String>();
	}
	
	public void begin() throws IOException{
		
		while(true){
			DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE],BUFFER_SIZE);
			
			this.socket.receive(packet);
		}
	}
	
	private String lookup(String plate_number){
		return "";
	}

	private void register(String owner_name, String plate_number){

	}
}
