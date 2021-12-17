import java.io.Serializable;
import java.sql.Timestamp;

public class Transaction implements Serializable{
	private double value;
	private Timestamp timestamp;
	
	public Transaction(double value, Timestamp timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}
	
	public double getValue() {
		return this.value;
	}
	
	public Timestamp getTimestamp() {
		return this.timestamp;
	}
}
