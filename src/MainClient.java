import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainClient {

	private static int PORT = 6789;
	private static List<String> tags = new ArrayList<String>();
	
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
					login(command, socketChannel);
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
			for(int i = 3; i<command.length; i++)
				tags.add(new String(command[i]));
			//for(String u : tags)	System.out.println(u);
			stub.register(command[1],command[2], tags);	//RMI of mainserver
			System.out.println("Registrazione effettuata con successo");
		}
	}

	public static boolean login(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		//ResponseMessage<ResponseData> response;
		
		//Send
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		@SuppressWarnings("unchecked")
		ResponseMessage<ResponseData> response = (ResponseMessage<ResponseData>) ois.readObject();
		if(response.getCode().equals("OK")) {
			System.out.println("User logged in");
			return true;
		}
		else {
			System.out.println(response.getCode());
			return false;
		}
	}
}
