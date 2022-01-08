import java.io.Serializable;

public class Comment implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
