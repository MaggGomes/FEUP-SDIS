package subprotocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utilities.Message;
import utilities.Utilities;

import filesystem.FileInfo;
import filesystem.FileManager;

public class Backup extends Protocol{
	public static final int MAX_TRYS = 5;
	public static final int WAIT = 1000;
	public static final int MAX_WORKERS = 10;
	public static final int CHUNK_MAXSIZE = 64000;
	public static ExecutorService threadWorkers = Executors.newFixedThreadPool(MAX_WORKERS);

	// TODO - VERIFICAR O QUE FALTA (ADICIONAR FICHEIRO)
	public static void saveFile(String filePath, int replicationDeg){
		FileInfo fileInfo = new FileInfo(filePath, replicationDeg);
		FileManager.addOwnFile(fileInfo); // Adds new file
		File file = new File(filePath);
		String fileID = fileInfo.getFileID();		
		FileInputStream fis;
		int readInput = 0;
		byte[] data = new byte[CHUNK_MAXSIZE];		

		try {
			fis = new FileInputStream(file);
			int chunkNo = 0;

			while((readInput = fis.read(data)) != -1){				
				byte[] arr = data;

				if(readInput != CHUNK_MAXSIZE){
					arr = Arrays.copyOf(data, readInput);
				}	

				FileManager.addOwnFileChunk(fileID, chunkNo);
				sendChunk(fileID, chunkNo, replicationDeg, arr);

				chunkNo++;
			}

			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Shutting down workers
		// COMO ESTÁ ASSIM NAO CRASHA  - .SHUTDOWN() - IMPEDE QUE MAIS WORKS LEVAM SUBMIT DE TAREFAS
		//threadWorkers.shutdown();


		// TODO - NOT WORKING???

		/*try {
			System.out.println("Attempting to shutdown workers.");
			threadWorkers.shutdown();
			threadWorkers.awaitTermination(2, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			System.err.println("Workers interrupted.");
		}
		finally {
			if (!threadWorkers.isTerminated()) {
				System.err.println("cancel non-finished tasks");
			}
			threadWorkers.shutdownNow();
			System.out.println("Shutdown finished!");
		}*/
	}

	// TODO - VERIFICAR SE ESTÁ TUDO FUNCIONAL
	public static void sendChunk(final String fileID, final int chunkNo, final int replicationDeg, final byte[] arr){		
		threadWorkers.submit(new Thread() {
			public void run() {

				Message msg = new Message(Message.PUTCHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo, Integer.toString(replicationDeg), arr);
				int trys = 0;
				int waitStored = WAIT;

				// TODO - VERIFICAR SE ESTÁ A FUNCIONAR CORRETAMENTE
				while(FileManager.getOwnCurrentChunkReplication(fileID, chunkNo) < replicationDeg && trys < MAX_TRYS){
					peer.getMdb().sendMessage(msg.getMessage());
					System.out.println("sending chunk "+chunkNo);
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
				
				System.out.println("Chunk "+chunkNo+" replication: "+FileManager.getOwnCurrentChunkReplication(fileID, chunkNo));
			}
		});		
	}

	public static void saveChunk(Message message){

		// TODO - CORRIGIR IF'S
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

		// Verifies if a file has already chunks in this peer
		if(FileManager.storedFiles.containsKey(message.getFileID())){
			if(FileManager.storedFiles.get(message.getFileID()).contains(Integer.parseInt(message.getChunkNo()))){
				return;
			}				
		} else {
			FileManager.storedFiles.put(message.getFileID(), new HashSet<Integer>());
		}		

		// TODO - CRIAR FUNÇÃO À PARTE PARA ISTO??
		try {
			String path = Utilities.createPath(peer.getServerID(), message.getFileID(), message.getChunkNo());
			Path newpath = Paths.get(path);			
			Files.createDirectories(newpath.getParent());
			Files.write(newpath, message.getBody());

			// Saves in stored files the chunk saved in the server
			System.out.println("Saving chunk: File:"+message.getFileID()+" chunk n: "+message.getChunkNo());
			FileManager.storedFiles.get(message.getFileID()).add(Integer.parseInt(message.getChunkNo()));
			FileManager.updateStoredReplicationDeg(message.getFileID(), Integer.parseInt(message.getChunkNo()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Backup.sendStored(message.getFileID(), message.getChunkNo());			
	}

	public static void sendStored(final String fileID, final String chunkNo){	
		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						Message msg = new Message(Message.STORED, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo);		
						peer.getMc().sendMessage(msg.getMessage());
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);		
	}

	public static void store(Message message){		
		if (FileManager.hasOwnFile(message.getFileID()))
			FileManager.addOwnChunkReplication(message.getFileID(), Integer.parseInt(message.getChunkNo()));	
	}
}
