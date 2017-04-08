package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import filesystem.Chunk;
import filesystem.FileManager;
import utilities.Message;
import utilities.Utilities;

public class Reclaim extends Protocol {

	// TODO - VERIFICAR SE FUNCIONA
	public static void reclaimSpace(long space) {
		if(space < FileManager.maxStorage){
			if (space >= FileManager.usedStorage)
				FileManager.maxStorage = space;
			else {
				FileManager.maxStorage = space;
				reclaimChunks(space);
			}			
		} else {
			FileManager.maxStorage = space;
		}
		
		peer.saveMetadata();
	}
	
	//TODO - VERIFICAR SE FUNCIONA
	public static void reclaimChunks(long space){
		
		// Verifies if the peer has any stored files
		if(FileManager.getStoredFilesChunks().size() <= 0)
			return;
		
		PriorityQueue<Chunk> chunks = new PriorityQueue<Chunk>();
		
		// Adds chunks to the priority queue
		for(ConcurrentHashMap<Integer, Chunk> stored: FileManager.storedChunks.values())
			for(Chunk chunk: stored.values())
				chunks.add(chunk);
		
		long currentSpace = 0;
		
		while(!chunks.isEmpty()){
			if((currentSpace+chunks.peek().getSize()) > space)
				break;
			else {
				currentSpace += chunks.peek().getSize();
				chunks.poll();
			}				
		}
		
		// Sends REMOVED message for each chunk to be deleted
		while(!chunks.isEmpty()){
			Message msg = new Message(Message.REMOVED, 
					peer.getProtocolVersion(), 
					peer.getServerID(), 
					chunks.peek().getFileID(), 
					Integer.toString(chunks.peek().getNumber()));
			
			System.out.println("Sending REMOVED message fo chunk "+chunks.peek().getNumber());
			peer.getMc().sendMessage(msg.getMessage());
			
			// Deleting chunk
			try {
				String path = Utilities.createBackupPath(peer.getServerID(), chunks.peek().getFileID(), Integer.toString(chunks.peek().getNumber()));
				Path chunkPath = Paths.get(path);
				Files.deleteIfExists(chunkPath);					
			} catch(IOException | SecurityException e) {
				System.out.println("Failed to delete chunk!");
			}
			
			FileManager.removeStoredChunk(chunks.peek().getFileID(), chunks.peek().getNumber());
			FileManager.reduceReplicationDeg(chunks.peek().getFileID(), chunks.peek().getNumber());	
			
			chunks.poll();
		}
		
		// TODO -  CORRIGIR
		FileManager.maxStorage = space;
		peer.saveMetadata();
	}

	//TODO - verificar se funciona
	public static void updateChunkReplicationDegree(Message message) {
		// Verifies if this peer is the initiator peer
		if(message.getSenderID().equals(peer.getServerID()))
			return;
		
		FileManager.reduceReplicationDeg(message.getFileID(), Integer.parseInt(message.getChunkNo()));
		
		// Verify if has the chunk stored
		if(FileManager.hasStoredChunkNo(message.getFileID(), Integer.parseInt(message.getChunkNo()))){
			// Verify if the perceived replication degree is lower than the desired replication degree
			if(FileManager.hasPerceveidedLowerDesired(message.getFileID(), Integer.parseInt(message.getChunkNo()))){
				
				String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), message.getChunkNo());
				Path chunkPath = Paths.get(path);

				try {
					byte[] chunk = Files.readAllBytes(chunkPath);
					sendBackUp(message.getFileID(), 
							Integer.parseInt(message.getChunkNo()), 
							FileManager.getStoredDesiredReplicationDeg(message.getFileID(), 
							Integer.parseInt(message.getChunkNo())), 
							chunk);
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
		
		peer.saveMetadata();
	}
	
	// TODO -CORRIGIR
	public static void sendBackUp(final String fileID, final int chunkNo, final int replicationDeg, byte[] data){	
		final byte[] chunk = Arrays.copyOf(data, data.length);
		
		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						// Verifies if the chunk was already stored
						
						if(FileManager.filesTrackReplication.get(fileID).get(chunkNo) < replicationDeg){
							// TODO - VERIFICAR SE ENTRETANTO JA NAO FOI RECEBIDO UM PEDIDO PARA O MESMO FILEID
							
							Message msg = new Message(Message.PUTCHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo, Integer.toString(replicationDeg), chunk);		
							System.out.println("Sending PUTCHUNK message for chunk "+chunkNo);
							peer.getMc().sendMessage(msg.getMessage());	
							// TODO - USAR FUNÇAO SEND CHUNK AQUI
						} else {							
							try {
								
							} catch (IOException e) {
								e.printStackTrace();
							}
						}					
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);
	}
}
