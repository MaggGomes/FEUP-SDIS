package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import filesystem.FileManager;

import peer.Peer;

import subprotocols.Backup;
import subprotocols.BackupEnhancement;
import utilities.Message;

public class BackupChannel extends Channel {

	public BackupChannel(Peer peer, String address, String port) throws UnknownHostException {
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

	// TODO CORRIGIR PROTOCOL VERSION
	@Override
	public void processMessage(Message message) {
		/* Verifies if the sender and receiver peers are the same */
		if(message.getSenderID().equals(peer.getServerID()))
			return;
		
		/* Verifies if this peer is the one which backed up this file */
		if(FileManager.hasBackedUpFileID(message.getFileID()))
			return;
				
		switch(message.getMessageType()){
		case Message.PUTCHUNK:
			if(message.getVersion().equals("1.0"))
				Backup.storeChunk(message);
			else
				BackupEnhancement.storeChunk(message);
			break;
		default:
			System.out.println("MDB: Packet discarded!");
			break;
		}		
	}
}
