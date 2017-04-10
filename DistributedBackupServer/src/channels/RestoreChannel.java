package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import peer.Peer;

import subprotocols.Protocol;
import subprotocols.Restore;
import subprotocols.RestoreEnhancement;
import utilities.Message;

public class RestoreChannel extends Channel {

	/**
	 * Restore channels constructor
	 * 
	 * @param peer
	 * @param address
	 * @param port
	 * @throws UnknownHostException
	 */
	public RestoreChannel(Peer peer, String address, String port) throws UnknownHostException {
		super(peer, address, port);
	}

	/**
	 * Runs the channel
	 */
	@Override
	public void run() {
		while (true){			
			byte[] buf = new byte[Message.MAX_HEADER_SIZE+Protocol.CHUNK_MAXSIZE];

			try {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				this.getSocket().receive(packet);	
				Message message = new Message(packet);					
				processMessage(message);
			} catch(IOException e){
				e.printStackTrace();
			}
		}	
	}

	/**
	 * Processes message received
	 * 
	 * @param message
	 */
	@Override
	public void processMessage(Message message) {
		
		switch(message.getMessageType()){
		case Message.CHUNK:
			Restore.recoverChunk(message);				
			break;
		case Message.HAVECHUNK:
			RestoreEnhancement.recoverChunk(message);				
			break;
		case Message.SENDCHUNK:
			if(message.getVersion().equals(peer.getServerID()))
				RestoreEnhancement.sendChunk(message);				
			break;
		default:
			System.out.println("MDR: Packet discarded!");
			break;
		}		
	}
}
