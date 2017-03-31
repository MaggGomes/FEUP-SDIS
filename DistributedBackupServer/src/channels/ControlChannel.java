package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import peer.Peer;
import subprotocols.Backup;
import subprotocols.Delete;
import utilities.Message;

public class ControlChannel extends Channel {

	public ControlChannel(Peer peer, String address, String port) throws UnknownHostException {
		super(peer, address, port);
	}

	@Override
	public void run() {
		while (true){			
			byte[] buf = new byte[Message.MAX_HEADER_SIZE+Backup.CHUNK_MAXSIZE];

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

	// TODO - COMPLETAR	
	@Override
	public void processMessage(Message message) {
		
		switch(message.getMessageType()){
		case Message.STORED:
			Backup.store(message);				
			break;
		case Message.GETCHUNK:	
			break;
		case Message.DELETE:
			Delete.deleteStoredFile(message);
			break;
		case Message.REMOVED:
			break;
		default:
			System.out.println("MC: Packet discarded!");
			break;
		}		
	}
}
