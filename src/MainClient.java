import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class MainClient {

	private static int PORT = 6789;
	//private static List<String> tags;
	
	public static void main(String[] args) {

		SocketChannel socketChannel;
		try {
			//Setup RMI for registration
			Registry r = LocateRegistry.getRegistry(5000);
			InterfaceUserRegistration stub = (InterfaceUserRegistration) r.lookup("Server");
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
			
			socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress("localhost", PORT));
			
			System.out.println("Benvenuto su WINSOME.\nLogin o Registrati");
			
			
			while(true) {
				String command = in.nextLine();
				String[] splitCommand = command.split(" ");
				
				switch(splitCommand[0].toLowerCase()) {
				case "register":
					Registrazione(splitCommand, stub);
					break;
				case "login":
					//boolean ret = login(command, socketChannel);
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
		if(command.length < 3 || command.length > 8) {
			System.err.println("ERROR. Usage registrati: register [username] [password] [tags]");
		} else {
			stub.register(command[1],command[2]);	//RMI of mainserver
			System.out.println("Registrazione effettuata con successo");
		}
	}

	public static boolean login(String cmd, SocketChannel socketChannel) throws IOException {
		//Send
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		//Receive
		ByteBuffer buffer = ByteBuffer.allocate(20);
		socketChannel.read(buffer);
		buffer.flip();
		String response = new String(buffer.array()).trim();
		System.out.println(response+" - by server");
		buffer.clear();
		return true;
	}
}
