import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceNotifyEvent  extends Remote{

	void notifyEventAddFollower(String username) throws RemoteException;
	
}
