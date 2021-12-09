import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRegistration implements InterfaceUserRegistration {
	
	private static int IdUser = 1;
	
	private Map<Integer, InfoRegistration> users = new HashMap<Integer, InfoRegistration>();
	
	public void register(String username, String password){
		InfoRegistration user = new InfoRegistration(username, password);
		users.put(IdUser, user);
		IdUser++;
	}

	public List<String> getUsernames(){
		List<String> u = new ArrayList<String>();
		for(Integer key : users.keySet())
			u.add(users.get(key).getUsername());
		return u;
	}
	
}
