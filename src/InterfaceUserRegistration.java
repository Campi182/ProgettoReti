import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceUserRegistration extends Remote{

	public void register(String username, String password) throws RemoteException;
	
}
