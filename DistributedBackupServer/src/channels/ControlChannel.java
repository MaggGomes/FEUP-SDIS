package channels;

import java.net.UnknownHostException;

public class ControlChannel extends Channel {

	public ControlChannel(String address, String port) throws UnknownHostException {
		super(address, port);
	}

	@Override
	public void run() {
		// TODO falta implementar
		
	}
}
