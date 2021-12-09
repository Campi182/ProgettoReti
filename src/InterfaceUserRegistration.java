import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfaceUserRegistration extends Remote{

	public void register(String username, String password) throws RemoteException;
	
	public List<String> getUsernames() throws RemoteException;
}
