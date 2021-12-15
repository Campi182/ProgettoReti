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
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainClient extends RemoteObject implements InterfaceNotifyEvent{

	private static int PORT = 6789;
	private static String currentUser;
	private static List<String> tags = new ArrayList<String>();
	
	private static List<String> followers;
	
	public static void main(String[] args) {
		boolean logged = false;
		boolean var = true;	//variabile per il ciclo while. con quit esco
		boolean response;
		SocketChannel socketChannel;
		
		try {
			//Setup RMI for registration
			Registry r = LocateRegistry.getRegistry(5000);
			InterfaceServerRMI stub = (InterfaceServerRMI) r.lookup("Server");
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
			
			socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress("localhost", PORT));
			
			System.out.println("Benvenuto su WINSOME.\nLogin o Registrati");
			
			
			while(var) {
				String command = in.nextLine();
				String[] splitCommand = command.split(" ");
				
				switch(splitCommand[0].toLowerCase()) {
				case "register":
					Registrazione(splitCommand, stub);
					break;
				case "login":
					if(logged) {
						System.err.println("ERROR: User already logged in");
						break;
					}
					response = login(command, socketChannel);
					if(response)
						logged = true;
					break;
				case "logout":
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					response = logout(command, socketChannel);
					if(response)
						logged = false;
					break;
				case "list":		//manca listfollowers e list following
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					if(splitCommand[1].equals("users")) {
						listUsers(command, socketChannel);
					}
					
					if(splitCommand[1].equals("followers")) {
						if(followers.isEmpty())
							System.out.println("Non hai followers");
						else {
							System.out.println("UTENTI CHE TI SEGUONO");
							for(String u : followers)
								System.out.println(u);
						}
					}
					
					if(splitCommand[1].equals("following")) {
						listFollowing(command, socketChannel);
					}
					break;
					
				case "follow":
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					else follow(command, socketChannel);
					break;
				case "unfollow":
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					else unfollow(command, socketChannel);
					break;
					
				case "post":
					if(!logged) {
						System.out.println("ERROR: User not logged in");
						break;
					}
					else createPost(command, socketChannel);
					break;
					
				case "show":
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					else showFeed(command, socketChannel);
					break;
					
				case "blog":
					if(!logged) {
						System.err.println("ERROR: User not logged in");
						break;
					}
					else viewBlog(command, socketChannel);
					break;
					
				case "help":
					help();
					break;
				case "quit":
					socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
					System.out.println("EXIT OK");
					var = false;
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
	
	public static void Registrazione(String[] command, InterfaceServerRMI stub) throws RemoteException{
		String result;
		if(command.length < 3 || command.length > 8) {
			System.err.println("ERROR. Usage registrati: register [username] [password] [tags]");
		} else {
			for(int i = 3; i<command.length; i++)
				tags.add(new String(command[i]));
			result = stub.register(command[1],command[2], tags);	//RMI of mainserver
			System.out.println(result);
		}
	}

	public static boolean login(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		String[] str_split = cmd.split(" ");
		//Send
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		//Receive
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		@SuppressWarnings("unchecked")
		ResponseMessage<String> response = (ResponseMessage<String>) ois.readObject();
		if(response.getCode().equals("OK")) {
			System.out.println(response.getCode());
			currentUser = str_split[1];
			followers = response.getList();	//preso la lista dei followers dal server
			return true;
		}
		else {
			System.out.println(response.getCode());
			return false;
		}
	}

	public static void listUsers(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		//Receive
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		
		@SuppressWarnings("unchecked")
		ResponseMessage<Utente> res = (ResponseMessage<Utente>) ois.readObject();
		
		if(res.getList() == null) {
			System.out.println(res.getCode());
			return;
		}
		
		System.out.println("Users | Tags");
		for(Utente u : res.getList())
			System.out.println(u.getUsername() + " : " + u.getTags());
	}

	public static boolean logout(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		//Receive
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		String result = ((String) ois.readObject()).trim();
		
		if(result.equals("OK")) {
			System.out.println("LOGGED OUT");
			return true;
		} else System.out.println(result);
		return false;
	}

	public static void follow(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
		String[] str_split = cmd.split(" ");
		
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		
		String res = (String) ois.readObject();
		
		if(res.equals("OK"))
			System.out.println("Ora segui " + str_split[1]);
		else System.out.println(res);
	}
	
	public static void unfollow(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException{
		String[] str_split = cmd.split(" ");
		
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		String res = (String) ois.readObject();
		
		if(res.equals("OK"))
			System.out.println("Non segui piu " + str_split[1]);
		else System.out.println(res);
	}
	
	public static void listFollowing(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException{
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		@SuppressWarnings("unchecked")
		ResponseMessage<String> res = (ResponseMessage<String>) ois.readObject();
		
		if(res.getList() == null) {
			System.out.println(res.getCode());
			return;
		}
		
		System.out.println("Utenti che segui");
		for(String u : res.getList())
			System.out.println(u);
	}
	
	public static void createPost(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException{
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		String res = (String) ois.readObject();
		
		if(res.equals("OK"))
			System.out.println("Post pubblicato");
		else System.out.println(res);
		
	}
	
	public static void showFeed(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException{
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		@SuppressWarnings("unchecked")
		ResponseMessage<Post> res = (ResponseMessage<Post>) ois.readObject();
		
		if(res.getList() == null || res.getList().isEmpty()) {
			System.out.println(res.getCode());
			return;
		}
		
		System.out.println("-----------FEED-------------");
		for(Post p : res.getList()) {
			System.out.println(p.getId()+"\t"+p.getAutore()+"\t"+p.getTitolo());
		}
	}
	
	public static void viewBlog(String cmd, SocketChannel socketChannel) throws IOException, ClassNotFoundException{
		socketChannel.write(ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8)));
		
		ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
		@SuppressWarnings("unchecked")
		ResponseMessage<Post> res = (ResponseMessage<Post>) ois.readObject();
		
		if(res.getList() == null || res.getList().isEmpty()) {
			System.out.println(res.getCode());
			return;
		}
		
		System.out.println("-----------BLOG-------------");
		for(Post p : res.getList()) {
			System.out.println(p.getId()+"\t"+p.getAutore()+"\t"+p.getTitolo());
		}
	}
	
	public static void help() {
		System.out.println("------------------------COMANDS GUIDE-----------------");
		System.out.println("register <username> <password> <tags> : Registra un nuovo utente nel sistema");
		System.out.println("login <username> <password> : Login nel sistema");
		System.out.println("logout");
		System.out.println("list users : lista gli utenti con cui hai almeno un tag in comune");
		System.out.println("list followers: lista degli utenti che ti seguono");
		System.out.println("list following: lista degli utenti che segui");
		System.out.println("follow <username>: segui un utente");
		System.out.println("unfollow <username>: smetti di seguire un utente");
		System.out.println("quit: esci");
		//System.out.println("");
	}
	
	public void notifyEventListFollowers(String username) {
		if(!(followers.contains(username)))
			followers.add(username);
		else followers.remove(username);
	}
	
}
