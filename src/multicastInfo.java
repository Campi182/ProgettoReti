import java.io.Serializable;

public class multicastInfo implements Serializable{

	private String ipAddress;
	private int port;
	
	public multicastInfo(String ipAddress, int port) {
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	public String getIpAddress() {
		return this.ipAddress;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
}
