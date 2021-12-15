import java.net.MulticastSocket;

public class multicastConnectInfo extends multicastInfo{

	private MulticastSocket socket;
	
	public multicastConnectInfo(MulticastSocket socket, String address, int port) {
		super(address, port);
		this.socket = socket;
	}
	
	public void setSocket(MulticastSocket socket) {
		this.socket = socket;
	}
	
	public MulticastSocket getSocket() {
		return socket;
	}
}
