import java.io.Serializable;
import java.util.List;

public class ResponseMessage<T> implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String code;
	private List<T> list;
	
	public ResponseMessage(String code, List<T> list) {
		this.code = code;
		this.list = list;
	}
	
	public List<T> getList(){
		return this.list;
	}
	
	public String getCode() {
		return this.code;
	}
}
