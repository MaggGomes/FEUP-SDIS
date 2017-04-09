package subprotocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utilities.Message;
import utilities.Utilities;

import filesystem.BackedUpFile;
import filesystem.FileManager;

public class BackupEnhancement extends Protocol{

	/**
	 * Attempts to save a file in other peers
	 * 
	 * @param filePath of the file to back up
	 * @param replicationDeg desired
	 */
	public static void backUpFile(String filePath, int replicationDeg){
		BackedUpFile fileInfo = new BackedUpFile(filePath, replicationDeg);

		/* Verifies if the file was already backed up */
		if(FileManager.hasBackedUpFileID(fileInfo.getFileID())){
			System.out.println("File already backed up!");
			return;
		}

		FileManager.addBackedUpFile(fileInfo); /* Adds new file */
		File file = new File(filePath);
		String fileID = fileInfo.getFileID();		
		FileInputStream fis;
		int readInput = 0;
		byte[] data = new byte[CHUNK_MAXSIZE];

		threadWorkers = Executors.newFixedThreadPool(MAX_WORKERS);
		int chunkNo = 0;

		try {
			fis = new FileInputStream(file);			

			while((readInput = fis.read(data)) != -1){				
				byte[] chunk = data;

				if(readInput != CHUNK_MAXSIZE){
					chunk = Arrays.copyOf(data, readInput);
				}	

				FileManager.addBackedUpFileChunk(fileID, chunkNo, readInput, replicationDeg);
				sendChunk(fileID, chunkNo, replicationDeg, chunk);

				chunkNo++;
			}

			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Shutting down workers */
		try {
			threadWorkers.shutdown();
			threadWorkers.awaitTermination((1+chunkNo)*5000, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			System.err.println("Workers interrupted.");
		}
		finally {
			if (!threadWorkers.isTerminated()) {
				System.err.println("Canceling non-finished tasks");
			}
			threadWorkers.shutdownNow();
			System.out.println("Shutdown finished!");
		}
	}

	/**
	 * Attempts to send a chunk of the file to back up
	 * 
	 * @param fileID
	 * @param chunkNo
	 * @param replicationDeg
	 * @param data
	 */
	public static void sendChunk(final String fileID, final int chunkNo, final int replicationDeg, byte[] data){
		final byte[] chunk = Arrays.copyOf(data, data.length);

		threadWorkers.submit(new Thread() {
			public void run() {
				// TODO - CORRIGIR PROTOCOL VERSION
				Message msg = new Message(Message.PUTCHUNK, "2.0", peer.getServerID(), fileID, chunkNo, Integer.toString(replicationDeg), chunk);
				int trys = 0;
				int waitStored = WAIT;

				while(FileManager.getBackedUpChunkPerceivedReplication(fileID, chunkNo) < replicationDeg && trys < MAX_TRYS){
					peer.getMdb().sendMessage(msg.getMessage());
					System.out.println("Sending chunk "+chunkNo);
					try{
						Thread.sleep(waitStored);
					} catch(InterruptedException e){
						e.printStackTrace();
					}

					waitStored = waitStored*2;
					trys++;
				}

				if (trys >= MAX_TRYS)
					System.out.println("Failed to save chunk "+chunkNo+".");
				else 				
					System.out.println("Chunk "+chunkNo+" saved with success!");
			}
		});		
	}

	/**
	 * Attempts to send a reclaimed chunk to achieve its desired replication degree
	 * 
	 * @param fileID of the parent chunk
	 * @param chunkNo of the chunk
	 * @param replicationDeg desired for the chunk
	 * @param data
	 */
	public static void sendReclaimedChunk(final String fileID, final int chunkNo, final int replicationDeg, byte[] data){
		final byte[] chunk = Arrays.copyOf(data, data.length);

		new Thread((
				new Runnable(){
					@Override
					public void run(){
						Message msg = new Message(Message.PUTCHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo, Integer.toString(replicationDeg), chunk);
						int trys = 0;
						int waitStored = WAIT;

						while(FileManager.getPerceivedReplicationDeg(fileID, chunkNo) < replicationDeg && trys < MAX_TRYS){
							peer.getMdb().sendMessage(msg.getMessage());
							System.out.println("Sending chunk "+chunkNo);
							try{
								Thread.sleep(waitStored);
							} catch(InterruptedException e){
								e.printStackTrace();
							}

							waitStored = waitStored*2;
							trys++;
						}

						if (trys >= MAX_TRYS)
							System.out.println("Failed to save chunk "+chunkNo+".");
						else 				
							System.out.println("Chunk "+chunkNo+" saved with success!");						
					}
				})).start();	
	}

	/**
	 * Saves a chunk in the peer
	 * 
	 * @param message
	 */
	public static void storeChunk(Message message){
		/* Removes the chunk received from the chunks to be sent */
		FileManager.removeChunkToSend(message.getFileID(), Integer.parseInt(message.getChunkNo()));

		// Verifies if the replication degree of a chunk has been achieved
		if(FileManager.filesTrackReplication.containsKey(message.getFileID())){
			if(FileManager.filesTrackReplication.get(message.getFileID()).containsKey(Integer.parseInt(message.getChunkNo()))){
				if(FileManager.filesTrackReplication.get(message.getFileID()).get(Integer.parseInt(message.getChunkNo())) >= Integer.parseInt(message.getReplicationDeg())){
					return;
				}			
			} else {
				FileManager.filesTrackReplication.get(message.getFileID()).put(Integer.parseInt(message.getChunkNo()), 0);
			}

		} else {
			FileManager.filesTrackReplication.put(message.getFileID(), new ConcurrentHashMap<Integer, Integer>());
			FileManager.filesTrackReplication.get(message.getFileID()).put(Integer.parseInt(message.getChunkNo()), 0);
		}

		/* Verifies if a file has already the chunk to store in this peer */
		if(FileManager.hasStoredFileID(message.getFileID())){
			if(FileManager.hasStoredChunkNo(message.getFileID(), Integer.parseInt(message.getChunkNo()))){
				return;
			}				
		} else {
			FileManager.addStoredFile(message.getFileID());
		}		

		/* Verifies if the peer has enough free storage to store the chunk */
		if(!FileManager.hasEnoughStorage(message.getBody().length)){
			System.out.println("Chunk discarded! Not enough free storage to store the chunk received!");
			return;
		}

		/* Storing the chunk */
		try {
			String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), message.getChunkNo());
			Path chunkPath = Paths.get(path);			
			Files.createDirectories(chunkPath.getParent());
			Files.write(chunkPath, message.getBody());

			/* Saves in stored files the chunk saved in the server */
			System.out.println("Saving chunk: File:"+message.getFileID()+" chunk n: "+message.getChunkNo());

			FileManager.addStoredChunk(message.getFileID(), 
					Integer.parseInt(message.getChunkNo()), 
					message.getBody().length, 
					Integer.parseInt(message.getReplicationDeg()), 
					FileManager.filesTrackReplication.get(message.getFileID()).get(Integer.parseInt(message.getChunkNo())));
		} catch (IOException e) {
			e.printStackTrace();
		}

		BackupEnhancement.sendStored(Integer.parseInt(message.getReplicationDeg()), message.getFileID(), message.getChunkNo());			
	}

	/**
	 * Sends a message alerting that the peer has backed up a chunk of the file
	 * 
	 * @param fileID
	 * @param chunkNo
	 */
	public static void sendStored(final int replicationDeg, final String fileID, final String chunkNo){	
		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						/*  Verifies if the chunk was already stored */
						// TODO - CORRIGIR PROTOCOL VERSION
						if(FileManager.getPerceivedReplicationDeg(fileID, Integer.parseInt(chunkNo)) < replicationDeg){
							Message msg = new Message(Message.STORED, "2.0", peer.getServerID(), fileID, chunkNo);		
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
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);		
	}

	/**
	 * Updates replication degree of the file in the initiator peer
	 * 
	 * @param message
	 */
	public static void store(Message message){	
		// Verifies if the peer is the initiator peer
		if (FileManager.hasBackedUpFileID(message.getFileID()))
			FileManager.updateBackedUpReplicationDeg(message.getFileID(), Integer.parseInt(message.getChunkNo()));
		else
			FileManager.updateStoredReplicationDeg(message.getFileID(), Integer.parseInt(message.getChunkNo()));
	}
}
