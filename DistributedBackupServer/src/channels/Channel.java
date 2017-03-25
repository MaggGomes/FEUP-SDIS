package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import utilities.Message;

public abstract class Channel implements Runnable {
	
	protected InetAddress address;
	protected int port;
	protected MulticastSocket socket;
	
	public Channel(String address, String port) throws UnknownHostException{
		
		this.address = InetAddress.getByName(address);
		this.port = Integer.parseInt(port);
		
		try {			
			socket = new MulticastSocket(this.port);
			socket.setTimeToLive(1);
			socket.joinGroup(this.address);	
			
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void sendMessage(String message){
		
		byte[] sendMessage = message.getBytes(StandardCharsets.US_ASCII);
		DatagramPacket packet = new DatagramPacket(sendMessage, sendMessage.length, address, port);
		
		try {
			socket.send(packet);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public MulticastSocket getSocket(){
		return socket;
	}

}
