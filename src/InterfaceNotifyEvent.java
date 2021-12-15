import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceNotifyEvent  extends Remote{

	void notifyEventListFollowers(String username) throws RemoteException;
	
}
