package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
			FileInfo fileChunks = FileManager.getBackedUpChunksInfo(file.getFileID());

			//TODO - FALTA CRIAR PARA CADA CHUNKNO UMA CHAMDA PARA A FUNÇAO QUE PEDE O CHUNK


			// TODO CORRIGIR
			Thread.sleep(5000);
			joinChunks();

		} else {
			System.out.println("File not backed up!");
		}
	}

	//TODO - VERIFICAR SE ESTÁ A FUNCIONAR CORRETAMENTE
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

	// TODO - VERIFICA SE ESTÁ TUDO FUNCIONAL
	public static void getChunk(Message message){
		// Verifies if the sender is the same as the receiver 
		if(message.getSenderID().equals(peer.getServerID()))
			return;

		// Verifies if the peer has any chunk of the requested file
		if(FileManager.storedFiles.containsKey(message.getFileID())){
			// Verifies if the peer has the requested chunk
			if (FileManager.FileManager.storedFiles.get(message.getFileID()).contains(Integer.parseInt(message.getChunkNo()))){
				// Verifies if the chunk was already sent
				if(!chunksSent.containsInteger.parseInt(message.getChunkNo())){
					
					/*String path = "abc/ficheiro.jpg";
		Path newpath = Paths.get(path);
		
		
		
		try {
			byte[] chunck = Files.readAllBytes(newpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/



				}
			}
		}
	}

	// TODO - VERIFICAR SE ESTÁ A FUNCIONAR CORRETAMENTE
	public static void sendChunk(final String fileID, final int chunkNo, byte[] data){
		final byte[] chunk = Arrays.copyOf(data, data.length);

		Executors.newSingleThreadScheduledExecutor().schedule(
				new Runnable(){
					@Override
					public void run(){
						// Verifies if the chunk was already
						if(!chunksSent.contains(chunkNo)){
							Message message = new Message(Message.STORED, peer.getProtocolVersion(), peer.getServerID(), fileID, Integer.parseInt(Integer.parseInt(chunkNo)), chunk);		
							System.out.println("Sending chunk "+chunkNo+".");
							peer.getMdr().sendMessage(message.getMessage());
							chunksSent.add(Integer.parseInt(chunkNo));
						}						
					}
				},
				Utilities.randomNumber(0, 400), TimeUnit.MILLISECONDS);	
	}

	// TODO -  VERIFICAT SE ESTÁ A FUNCIONAR
	public static void recoverChunk(Message message){
		if(!chunksSent.contains(Integer.parseInt(message.getChunkNo())))
			chunksSent.add(Integer.parseInt(message.getChunkNo()));
		
		// Verifies if this is the initiator peer which requested the file
		if(FileManager.hasBackedUpFile(message.getFileID()))
			chunksRecovered.put(Integer.parseInt(message.getChunkNo()), message.getBody());
	}

//TODO - CORRIGIR
	public static void joinChunks(){


		/*

FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(getFile(RESTORED_DIR + filename));
        } catch (IOException e) {
            System.err.println("Error opening file for recovery.");
            return false;
        }

        for (int chunkNo = 0; chunkNo < receivedChunks.size(); chunkNo++) {
            try {
                fileOutputStream.write(receivedChunks.get(chunkNo), 0, receivedChunks.get(chunkNo).length);
            } catch (IOException e) {
                System.err.println("Error writing chunk " + chunkNo + " to file.");
                return false;
            }
        }

		*/


	}

}
