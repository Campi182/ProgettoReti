import java.io.Serializable;
import java.util.List;

public class Utente implements Serializable{

	private String username;
	private String password;
	private List<String> tags;
	
	public Utente(String username, String password, List<String> tags) {
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
