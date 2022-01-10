import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Utente implements Serializable{	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String username;
	private String password;
	private transient boolean isOnline;
	private List<String> tags;
	private List<Transaction> transazioni;
	private double wincoins;
	
	public Utente(String username, String password, List<String> tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.wincoins = 0;
		this.isOnline = false;
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
	
	public boolean isOnline() {
		return this.isOnline;
	}
	
	public void setWincoins(double val, String timestamp) {
		Transaction action = new Transaction(val, timestamp);
		transazioni.add(action);
		this.wincoins += val;
	}
	
	public void setTransazioni(List<Transaction> transazioni) {
		this.transazioni = transazioni;
	}
	
	public void setGuadagno(double wincoins) {
		this.wincoins = wincoins;
	}
	
	public double getWincoins() {
		return this.wincoins;
	}
	
	public void setStatus(boolean value) {
		this.isOnline = value;
	}
	
	public List<Transaction> getAllTransactions(){
		return transazioni;
	}
}
