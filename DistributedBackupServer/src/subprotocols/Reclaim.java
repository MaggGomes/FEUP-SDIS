package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import filesystem.FileManager;
import utilities.Message;
import utilities.Utilities;

public class Reclaim extends Protocol {

	// TODO - CORRIGIR
	public static void reclaimSpace(long space) {
		if(space < FileManager.maxStorage){
			if (space >= FileManager.usedStorage)
				FileManager.maxStorage = space;
			else {
				
			}			
		} else {
			FileManager.maxStorage = space;
		}
		
		peer.saveMetadata();
			
		
		
		
		// TODO Auto-generated method stub
		
		
		//Message message = new Message(Message.REMOVED, peer.getProtocolVersion(), peer.getServerID(), fileID);
		
		//Sends message to the other peers to delete the chunks from this file
		//peer.getMc().sendMessage(message.getMessage());
	}

	//TODO - CORRIGIR
	public static void updateChunkReplicationDegree(Message message) {
		
		if(message.getSenderID().equals(peer.getServerID()))
			return;
		
		FileManager.reduceReplicationDeg(message.getFileID(), Integer.parseInt(message.getChunkNo()));
		
		if(FileManager.hasStoredChunkNo(message.getFileID(), Integer.parseInt(message.getChunkNo()))){
			
		}

		// SO MANDAR PEDIDO SE A REPLICATION BAIXAR ABAXIO DO VALOR MINIMO
		
		
	}
	
	// TODO -CORRIGIR
	public static void sendStored(final String fileID, final int chunkNo, final int replicationDeg, byte[] data){	
		/*final byte[] chunk = Arrays.copyOf(data, data.length);
		
		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						// Verifies if the chunk was already stored
						
						if(FileManager.filesTrackReplication.get(fileID).get(Integer.parseInt(chunkNo)) < replicationDeg){
							// TODO - VERIFICAR SE ENTRETANTO JA NAO FOI RECEBIDO UM PEDIDO PARA O MESMO FILEID
							
							Message msg = new Message(Message.PUTCHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo, Integer.toString(replicationDeg), chunk);		
							peer.getMc().sendMessage(msg.getMessage());							
						} else {							
							try {
								System.out.println("Removing duplicated chunk...");
								String path = Utilities.createBackupPath(peer.getServerID(), fileID, chunkNo);
								Path chunkPath = Paths.get(path);
								Files.deleteIfExists(chunkPath);
								FileManager.removeStoredChunk(fileID, Integer.parseInt(chunkNo));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}					
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);	*/	
	}
}
