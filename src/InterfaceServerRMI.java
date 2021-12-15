import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfaceServerRMI extends Remote{

	public String register(String username, String password, List<String> tags) throws RemoteException;
	
	public void registerForCallback(InterfaceNotifyEvent clientInterface, String username) throws RemoteException;
	
	public void unregisterForCallback(InterfaceNotifyEvent clientInterface) throws RemoteException; 
}
