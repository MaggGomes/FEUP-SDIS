package subprotocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utilities.Message;
import utilities.Utilities;

import filesystem.FileInfo;
import filesystem.FileManager;

// TODO - FALTA IMPLEMENTAR
public class Restore extends Protocol{
	// TODO - CHUNKS RECUPERADOS PELO SENDER
	public static ConcurrentHashMap<Integer, byte[]> chunksRecovered;
	// TODO - CHUNKS ENVIADOS PELOS RECEIVERS
	public static Set<Integer> chunksSent = new HashSet<Integer>();

	// TODO - FALTA TERMINAR
	public static void restoreFile(String filePath){
		threadWorkers = Executors.newFixedThreadPool(MAX_WORKERS);

		FileInfo file = new FileInfo(filePath);

		if(FileManager.hasBackedUpFile(file.getFileID())){
			System.out.println("Attempting to restore file...");
			//TODO - FALTA TERMINAR
			chunksRecovered = new ConcurrentHashMap<Integer, byte[]>();
			ConcurrentHashMap<Integer, Integer> fileChunks = FileManager.getChunksBackedUpFile(file.getFileID());
			
			// Requests each chunk
			for (Integer chunkNo: fileChunks.keySet())
				sendGetChunk(file.getFileID(), Integer.toString(chunkNo));

			
			// TODO CORRIGIR
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			joinChunks(file.getFileID(), file.getFileName());
		} else {
			System.out.println("File not backed up!");
		}
	}

	public static void sendGetChunk(final String fileID, final String chunkNo){
		threadWorkers.submit(new Thread() {
			public void run() {
				Message msg = new Message(Message.GETCHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo);
				int trys = 0;
				int waitStored = WAIT;

				while(!chunksRecovered.containsKey(Integer.parseInt(chunkNo)) && trys < MAX_TRYS){
					peer.getMc().sendMessage(msg.getMessage());
					System.out.println("Requesting chunk "+chunkNo);
					try{
						Thread.sleep(waitStored);
					} catch(InterruptedException e){
						e.printStackTrace();
					}

					waitStored = waitStored*2;
					trys++;
				}

				if (trys >= MAX_TRYS)
					System.out.println("Failed to recover chunk "+chunkNo+".");
				else 				
					System.out.println("Chunk "+chunkNo+" recovered with success!");
			}
		});		
	}

	public static void getChunk(Message message){
		// Verifies if the sender is the same as the receiver 
		if(message.getSenderID().equals(peer.getServerID()))
			return;

		// Verifies if the peer has any chunk of the requested file
		if(FileManager.storedFiles.containsKey(message.getFileID())){
			// Verifies if the peer has the requested chunk
			if (FileManager.storedFiles.get(message.getFileID()).contains(Integer.parseInt(message.getChunkNo()))){
				// Verifies if the chunk was already sent
				if(!chunksSent.contains(Integer.parseInt(message.getChunkNo()))){					
					String path = Utilities.createBackupPath(peer.getServerID(), message.getFileID(), message.getChunkNo());
					Path chunkPath = Paths.get(path);

					try {
						byte[] chunk = Files.readAllBytes(chunkPath);
						sendChunk(message.getFileID(), Integer.parseInt(message.getChunkNo()), chunk);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void sendChunk(final String fileID, final int chunkNo, byte[] data){
		final byte[] chunk = Arrays.copyOf(data, data.length);

		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						// Verifies if the chunk was already sent
						if(!chunksSent.contains(chunkNo)){
							Message message = new Message(Message.CHUNK, peer.getProtocolVersion(), peer.getServerID(), fileID, chunkNo, chunk);		
							System.out.println("Sending chunk "+chunkNo+".");
							peer.getMdr().sendMessage(message.getMessage());
							chunksSent.add(chunkNo);
						}						
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);	
	}

	// TODO -  VERIFICAR SE ESTÁ A FUNCIONAR
	public static void recoverChunk(Message message){
		if(!chunksSent.contains(Integer.parseInt(message.getChunkNo())))
			chunksSent.add(Integer.parseInt(message.getChunkNo()));

		// Verifies if this is the initiator peer which requested the file
		if(FileManager.hasBackedUpFile(message.getFileID()))
			chunksRecovered.put(Integer.parseInt(message.getChunkNo()), message.getBody());
	}

	//TODO - CORRIGIR
	public static void joinChunks(String fileID, String fileName){

		String path = Utilities.createRestorePath(peer.getServerID(), fileID, fileName);
		Path chunkPath = Paths.get(path);			
		try {
			Files.createDirectories(chunkPath.getParent());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		File file = new File(path);
		FileOutputStream fos;
		
		
		try {
            fos = new FileOutputStream(file);
            
            for (int chunkNo = 0; chunkNo < chunksRecovered.size(); chunkNo++) {
                try {
                    fos.write(chunksRecovered.get(chunkNo), 0, chunksRecovered.get(chunkNo).length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            fos.close();            
        } catch (IOException e) {
            e.printStackTrace();
        }
		 
	}
}
