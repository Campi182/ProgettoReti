import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ReadUDPFromServer implements Runnable{

	private MulticastSocket ms;
	private InetAddress group;
	
	public ReadUDPFromServer(int UDPPORT, String MULTICAST) throws IOException {
		group = InetAddress.getByName(MULTICAST);
		ms = new MulticastSocket(UDPPORT);
	}
	
	@SuppressWarnings("deprecation")
	public void run() {
		try{
			ms.joinGroup(group);
			byte[] buffer = new byte[1024];
			while(!Thread.currentThread().isInterrupted()) {
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				ms.receive(dp);
				if(Thread.currentThread().isInterrupted())
					return;
				String s = new String(dp.getData());
				System.out.println("FROM SERVER: " + s);
			}
		}catch(Exception e) {
			System.out.println(e);
		} finally {
			if(ms != null) {
				try {
					ms.leaveGroup(group);
					ms.close();
				}catch(IOException e) {}
			}
		}
	}
}
