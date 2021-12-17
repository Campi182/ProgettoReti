
public class CallbackInfo {

	private InterfaceNotifyEvent client;
	private String username;
	
	public CallbackInfo(InterfaceNotifyEvent client, String username) {
		this.client = client;
		this.username = username;
	}
	
	public InterfaceNotifyEvent getClient() {
		return this.client;
	}
	
	public String getUsername() {
		return this.username;
	}
}
