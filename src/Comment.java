import java.io.Serializable;

public class Comment implements Serializable{

	private String autore;
	private String commento;
	
	public Comment(String autore, String commento) {
		this.autore = autore;
		this.commento = commento;
	}
	
	public String getAutore() {
		return this.autore;
	}
	
	public String getCommento() {
		return this.commento;
	}
}
