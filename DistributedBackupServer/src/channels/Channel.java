package channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public abstract class Channel implements Runnable {
	
	protected InetAddress address;
	protected int port;
	protected MulticastSocket socket;
	
	public Channel(String address, String port) throws UnknownHostException{
		
		this.address = InetAddress.getByName(address);
		this.port = Integer.parseInt(port);
		
		try {			
			socket = new MulticastSocket(this.port);
			socket.joinGroup(this.address);	
			
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
