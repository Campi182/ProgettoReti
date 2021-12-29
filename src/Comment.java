import java.io.Serializable;
import java.sql.Timestamp;

public class Comment implements Serializable{

	private String autore;
	private String commento;
	private String timestamp;
	
	public Comment(String autore, String commento, String timestamp) {
		this.autore = autore;
		this.commento = commento;
		this.timestamp = timestamp;
	}
	
	public String getAutore() {
		return this.autore;
	}
	
	public String getCommento() {
		return this.commento;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
}
