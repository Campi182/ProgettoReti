import java.io.Serializable;
import java.util.List;

public class ResponseWallet<T> extends ResponseMessage<T> implements Serializable {
	private double guadagno;
	
	public ResponseWallet(String code, List<T> list, double guadagno) {
		super(code, list);
		this.guadagno = guadagno;
	}
	
	public double getGuadagno() {
		return this.guadagno;
	}
	
}
