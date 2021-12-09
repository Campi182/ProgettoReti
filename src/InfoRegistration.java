import java.util.List;

public class InfoRegistration {

	private String username;
	private String password;
	private List<String> tags;
	
	public InfoRegistration(String username, String password, List<String> tags) {
		this.username = username;
		this.password = password;
		this.tags = tags;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public List<String> getTags(){
		return this.tags;
	}
}
