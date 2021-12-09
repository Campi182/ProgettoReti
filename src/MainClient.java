import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class MainClient {

	public static void main(String[] args) {
		try {
			Registry r = LocateRegistry.getRegistry(5000);
			InterfaceUserRegistration stub = (InterfaceUserRegistration) r.lookup("Server");
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
			
			System.out.println("Benvenuto su WINSOME.\nLogin o Registrati");
			while(true) {
				String command = in.nextLine();
				String[] splitCommand = command.split(" ");
				
				switch(splitCommand[0].toLowerCase()) {
				case "registrati":
					Registrazione(splitCommand, stub);
					break;
				case "login":
					System.out.println("Login");
					break;
				case "listusers":
					List<String> users = stub.getUsernames();
					for(String u : users)	System.out.println(u);
					break;
				default:
					System.out.println("ERROR: invalid command");
					break;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}//main
	
	public static void Registrazione(String[] command, InterfaceUserRegistration stub) throws RemoteException{
		if(command.length != 3)
			System.err.println("ERROR. Usage registrati: registrati [username] [password]");
		else{
			stub.register(command[1],command[2]);
			System.out.println("Registrazione effettuata con successo");
		}
	}

	public static void Login() {
		System.out.println("Login");
	}
}
