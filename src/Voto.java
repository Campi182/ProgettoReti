import java.io.Serializable;
import java.sql.Timestamp;

public class Voto implements Serializable{

	private int voto;
	private Timestamp timestamp;
	
	public Voto(int voto, Timestamp timestamp) {
		this.voto = voto;
		this.timestamp = timestamp;
	}
	
	public int getVoto() {
		return this.voto;
	}
	
	public Timestamp getTimestamp() {
		return this.timestamp;
	}
}
