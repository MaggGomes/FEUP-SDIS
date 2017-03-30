package subprotocols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Restore extends Protocol{
	
	//TODO - CORRIGIR
	public void getChunck(){
		
		String path = "abc/ficheiro.jpg";
		Path newpath = Paths.get(path);
		
		
		
		try {
			byte[] chunck = Files.readAllBytes(newpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ao se mandar a mensagem de retorno, a mesma deve demorar entre 0 e 400ms
		// ver codigo em baixo
		
		
		/*ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		
		executorService.schedule(()-> {
			
			System.out.println("dlskandjasn");
			
		}, randomBetween(0, 400), TimeUnit.MILLISECONDS);	*/	
		
	}

}
