package channels;

import java.net.UnknownHostException;

public class BackupChannel extends Channel {

	public BackupChannel(String address, int port) throws UnknownHostException {
		super(address, port);
	}

	@Override
	public void run() {
		// TODO falta implementar
		
	}
}
