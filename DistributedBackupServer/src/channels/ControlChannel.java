package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

public class ControlChannel extends Channel {

	public ControlChannel(String address, String port) throws UnknownHostException {
		super(address, port);
	}

	@Override
	public void run() {
		// TODO falta implementar
		
		byte[] buf = new byte[64];
		
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
