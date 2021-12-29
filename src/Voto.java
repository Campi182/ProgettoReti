import java.io.Serializable;
import java.sql.Timestamp;

public class Voto implements Serializable{

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
