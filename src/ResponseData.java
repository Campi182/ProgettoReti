import java.io.Serializable;

public class ResponseData implements Serializable{

	String username;
	
	public ResponseData(String username) {
		this.username = username;
	}
	
	public String getUsername() {
		return this.username;
	}
}
