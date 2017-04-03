package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import peer.Peer;

import subprotocols.Protocol;
import subprotocols.Restore;
import utilities.Message;

public class RestoreChannel extends Channel {

	public RestoreChannel(Peer peer, String address, String port) throws UnknownHostException {
		super(peer, address, port);
	}

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

	@Override
	public void processMessage(Message message) {
		
		switch(message.getMessageType()){
		case Message.CHUNK:
			Restore.recoverChunk(message);				
			break;
		default:
			System.out.println("MDR: Packet discarded!");
			break;
		}		
	}
}
