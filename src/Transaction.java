import java.io.Serializable;
import java.sql.Timestamp;

public class Transaction implements Serializable{
	private double value;
	private String timestamp;
	
	public Transaction(double value, String timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}
	
	public double getValue() {
		return this.value;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
}
