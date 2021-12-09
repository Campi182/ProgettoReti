import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainServer {

	//------------------------strutture dati e variabili----------------- //
	
	
	
	//---------------------------------------------------------------------/
	
	public static void main(String[] args) {
		try {
			UserRegistration list = new UserRegistration();
			InterfaceUserRegistration stub = (InterfaceUserRegistration) UnicastRemoteObject.exportObject(list, 0);
			LocateRegistry.createRegistry(5000);
			Registry r = LocateRegistry.getRegistry(5000);
			r.rebind("Server", stub);
			System.out.println("Server pronto");
			
		}catch(RemoteException e) {
			System.err.println("Errore: " + e.getMessage());
		}
	}

	
	
}
