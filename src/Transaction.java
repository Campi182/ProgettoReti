import java.io.Serializable;

public class Transaction implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
