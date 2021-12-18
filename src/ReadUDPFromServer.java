import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ReadUDPFromServer implements Runnable{

	private MulticastSocket ms;
	private InetAddress group;
	
	public ReadUDPFromServer() throws IOException {
		ms = new MulticastSocket(1025);
		group = InetAddress.getByName("226.226.226.226");
	}
	
	public void run() {

		try {
			ms.joinGroup(group);
			ms.setSoTimeout(2000);
			byte[] buffer = new byte[1024];
			while(true) {
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				ms.receive(dp);
				String s = new String(dp.getData());
				System.out.println("FROM SERVER: " + s);
			}
		}catch(Exception e) {
			System.out.println(e);
		}
	}
}
