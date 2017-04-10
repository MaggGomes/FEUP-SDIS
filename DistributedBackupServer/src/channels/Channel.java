package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import peer.Peer;

import utilities.Message;

public abstract class Channel implements Runnable {
	
	protected Peer peer;
	protected InetAddress address;
	protected int port;
	protected MulticastSocket socket;
	
	/**
	 * Channel constructor
	 * 
	 * @param peer
	 * @param address
	 * @param port
	 * @throws UnknownHostException
	 */
	public Channel(Peer peer, String address, String port) throws UnknownHostException{
		
		this.address = InetAddress.getByName(address);
		this.port = Integer.parseInt(port);
		this.peer = peer;
		
		try {			
			this.socket = new MulticastSocket(this.port);
			this.socket.setTimeToLive(1);
			this.socket.joinGroup(this.address);	
			
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Initiates channel
	 */
	public void listen(){
		new Thread(this).start();
	}
	
	/**
	 * Sends a message
	 * 
	 * @param message
	 */
	public void sendMessage(byte[] message){
		
		DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
		
		try {
			getSocket().send(packet);
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Processes a message
	 * 
	 * @param message
	 */
	public abstract void processMessage(Message message);
	
	/**
	 * Closes channel
	 */
	public void close(){	
		
		try {
			getSocket().leaveGroup(address);
			getSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the channel's socket
	 * 
	 * @return
	 */
	public MulticastSocket getSocket() {
		return socket;
	}

}
