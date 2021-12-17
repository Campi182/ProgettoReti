import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Utente implements Serializable{	//tutte le cose che vado a scrivere negli outputstream devono implementare serializable

	private String username;
	private String password;
	private List<String> tags;
	private List<Transaction> transazioni;
	private double wincoins;
	
	public Utente(String username, String password, List<String> tags) {
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.wincoins = 0;
		transazioni = new ArrayList<>();
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
	
	public void setWincoins(double val, Timestamp timestamp) {
		Transaction action = new Transaction(val, timestamp);
		transazioni.add(action);
		this.wincoins += val;
	}
	
	public double getWincoins() {
		return this.wincoins;
	}
	
	public List<Transaction> getAllTransactions(){
		return transazioni;
	}
}
