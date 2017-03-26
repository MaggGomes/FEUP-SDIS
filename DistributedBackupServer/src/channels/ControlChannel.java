package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import filesystem.Chunk;

public class ControlChannel extends Channel {

	public ControlChannel(String address, String port) throws UnknownHostException {
		super(address, port);
	}

	@Override
	public void run() {
		// TODO falta implementar
		
		// TODO  CODIGO PARA TESTAR
		
		byte[] buf = new byte[Chunk.MAX_SIZE];
		
		try {
			while(true) {
				DatagramPacket msg = new DatagramPacket(buf, buf.length);
				this.socket.receive(msg);
				
				String newmsg = new String(buf, 0, buf.length);
				System.out.println("Packet: "+newmsg);
			}
			
		} catch(IOException e){
			e.printStackTrace();
		}
		
		
		
	}
}
