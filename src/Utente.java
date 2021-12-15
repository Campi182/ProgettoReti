import java.io.Serializable;
import java.util.List;

public class Utente implements Serializable{	//tutte le cose che vado a scrivere negli outputstream devono implementare serializable

	private String username;
	private String password;
	private List<String> tags;
	private float wincoins;
	
	public Utente(String username, String password, List<String> tags) {
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.wincoins = 0;
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
	
	public void setWincoins(float val) {
		wincoins += val;
	}
	
	public float getWincoins() {
		return this.wincoins;
	}
}
