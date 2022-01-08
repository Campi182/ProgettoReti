import java.io.Serializable;

public class Voto implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int voto;
	private String timestamp;
	
	public Voto(int voto, String timestamp) {
		this.voto = voto;
		this.timestamp = timestamp;
	}
	
	public int getVoto() {
		return this.voto;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
}
