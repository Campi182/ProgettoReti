import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class MainServer extends RemoteObject implements InterfaceServerRMI{

	//------------------------strutture dati e variabili----------------- //
	private static final long serialVersionUID = 1L;
	
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;
	private static List<InterfaceNotifyEvent> clients; 

	//STRUTTURA DATI CHE MEMORIZZA GLI UTENTI REGISTRATI
	//Utente <username, password, tags>
	private static List<Utente> registeredUsers;
	private static Map<String, ArrayList<String>> followers;
	private static Map<String, ArrayList<String>> following;
	
	
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = new ArrayList<Utente>();
		clients = new ArrayList<InterfaceNotifyEvent>();
		followers = new HashMap<>();
		following = new HashMap<>();
	}
	
	
	
	public static void main(String[] args) {
		
		//RMI setup per registrazione
		MainServer server = new MainServer();
		//strutture per spedire le risposte/oggetti al client
		ByteArrayOutputStream baos;
		ObjectOutputStream oos;
		byte[] res = new byte[512];
		
		String resString;
		
		try {
			InterfaceServerRMI stub = (InterfaceServerRMI) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.createRegistry(5000);
			Registry r = LocateRegistry.getRegistry(5000);
			r.rebind("Server", stub);
			System.out.println("Server pronto");
		}catch(RemoteException e) {
			System.err.println("Errore: " + e.getMessage());
		}
		
		
		try {
			//Listening per nuove connessioni
			ServerSocketChannel serverSocket = ServerSocketChannel.open();
			serverSocket.socket().bind(new InetSocketAddress(PORT));
			serverSocket.configureBlocking(false);
			
			selector = Selector.open();
			SelectionKey clientKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			
			
			System.out.println("SERVER IS ON");
			
			while(true) {
				try {
					selector.select();
				}catch(IOException e) {
					e.printStackTrace();
					break;
				}
			
			
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
			while(iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				
				try {
					if(key.isAcceptable()) { //connection request
						ServerSocketChannel srv = (ServerSocketChannel) key.channel();
						SocketChannel client = srv.accept();
						client.configureBlocking(false);
						System.out.println("Accepted connection from " + client);
						client.register(selector, SelectionKey.OP_READ);
						key.attach(null);
					}
					else if(key.isReadable()) { //read request
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						client.read(buffer);
						
						String str_received = new String(buffer.array()).trim();
						String[] split_str = str_received.split(" ");
						System.out.println("Command requested: " + str_received);
						
						switch(split_str[0]) {
						case "login":
							ResponseMessage<String> res_Login;
							if(split_str.length != 3)
								res_Login = new ResponseMessage<>("ERROR: Usage: login <username> <password>", null);
							else {
								res_Login = login(split_str[1], split_str[2]);
								if(res_Login.getCode().equals("OK"))
									key.attach(split_str[1]);	
							}
							//Send response
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(res_Login);
							res = baos.toByteArray();
							break;
							
						case "logout":
							if(split_str.length != 1)
								resString = "ERROR: Usage: logout";
							else {
								resString = logout((String)key.attachment());
								if(resString.equals("OK"))	
									key.attach(null);
							}
							
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "list":	//listUsers && list following
							ResponseMessage<Utente> res_listUsers = null;
							ResponseMessage<String> res_listFollowing = null;
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
				
							if(split_str[1].equals("users")) {	//list users
								if(split_str.length != 2)
									res_listUsers = new ResponseMessage<>("ERROR: Usage: list users", null);
								res_listUsers = listUsers((String)key.attachment());
								oos.writeObject(res_listUsers);
							}
							
							if(split_str[1].equals("following")) {
								if(split_str.length != 2)
									res_listFollowing = new ResponseMessage<>("ERROR: Usage: list followers", null);
								res_listFollowing = listFollowing((String)key.attachment());
								oos.writeObject(res_listFollowing);
							}
							res = baos.toByteArray();
							break;
							
						case "follow":
							if(split_str.length != 2)
								resString = "ERROR: Usage: follow <username>";
							else {
								resString = follow((String) key.attachment(), split_str[1]);
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
						case "unfollow":
							if(split_str.length != 2)
								resString = "ERROR: Usage. unfollow <username>";
							else {
								resString = unfollow((String) key.attachment(), split_str[1]);
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "quit":
							if(key.attachment() != null)
								logout((String)key.attachment());
							client.close();
							key.cancel();
							break;
						default:	//da rifare
							resString = "ERROR: Command not found";
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);	
							res = baos.toByteArray();
							break;
							
						}
						key.interestOps(SelectionKey.OP_WRITE);
					}
					
					//DA CAMBIARE
					else if(key.isWritable()) { //write requests
						//SEND RESPONSE
						SocketChannel client = (SocketChannel) key.channel();
						client.write(ByteBuffer.wrap(res));
						key.interestOps(SelectionKey.OP_READ);
					}
				}catch(IOException | CancelledKeyException e) {	//cancelledkey per quando faccio il quit
					key.cancel();
					try {
						key.channel().close();
					}catch(IOException ig) {}
				}
			}
			}
		
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		
	}//MAIN
	
	//Method of RMI interface
	public String register(String username, String password, List<String> tags) throws RemoteException{
		if(username.isEmpty() || password.isEmpty())
			return "ERROR: Username and password cannot be empty";
		
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(username))
				return "ERROR: this username already exists";
		
		System.out.println("Requested register");
		Utente user = new Utente(username, password, tags);
		registeredUsers.add(user);
		followers.put(username, new ArrayList<>());
		following.put(username, new ArrayList<>());
		return "Registration success";
	}

	public static ResponseMessage<String> login(String username, String password) {

		String code = null;
		boolean tmp = false;
		List<String> followList;
		
		if(username.isEmpty() || password.isEmpty())
			code = "ERROR: Username and password cannot be empty";
		else {
			for(Utente u : registeredUsers) {
				if(u.getUsername().equals(username)) {
					if(u.getPassword().equals(password)) {		//cambiare controllo della password (deve esserer crittografato)
						tmp = true;
						code = "OK";
					} else code = "ERROR: wrong password";
				}
			}
		}
		
		if(tmp)
			followList = new ArrayList<>(followers.get(username));
		else followList = null;
			
		
		if(!tmp && code == null) 
			code = "ERROR: User not found, register first";
			
		return new ResponseMessage<>(code, followList);
	}
	
	public static ResponseMessage<Utente> listUsers(String username) {
		System.out.println(username+" ha richiesto la lista");
		
		if(registeredUsers.isEmpty())
			return new ResponseMessage<>("No Users Registered", null);
		
		List<Utente> UsersCommonTags = new ArrayList<>();
		List<String> userTags = new ArrayList<>();
		
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(username)) {
				userTags = u.getTags();
			}
		
		for(Utente u : registeredUsers) {
			if(!(u.getUsername().equals(username))) {
				for(String t : userTags) {
					if(u.getTags().contains(t)) {
						UsersCommonTags.add(u);
					}
				}
			}
		}
		
		if(UsersCommonTags.isEmpty())
			return new ResponseMessage<>("Non ci sono utenti con tag in comune", null);
		else return new ResponseMessage<>("OK", UsersCommonTags);
	}

	public static ResponseMessage<String> listFollowing(String username){
		if(following.get(username).isEmpty())
			return new ResponseMessage<>("Non segui nessuno", null);
		
		return new ResponseMessage<>("OK", following.get(username));
	}
	
	public static String logout(String username) {
		if(registeredUsers.isEmpty())	return "ERROR: No registered users";
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(username)) {
				return "OK";
			}
		}
		return "ERROR: User doesn't exists";
	}
	
	public static String follow(String currUser, String userToFollow) {
		if(userToFollow.isEmpty())
			return "ERROR: username cannot be empty";
		boolean exists = false;
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(userToFollow))
				exists = true;
		
		if(exists) {
			if(!following.get(currUser).contains(userToFollow)) {
				followers.get(userToFollow).add(currUser);
				following.get(currUser).add(userToFollow);
				try {
					update(currUser);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				return "OK";
			} else return "ERROR: Segui gia' quest'utente";
		} else return "ERROR: user does not exists";
		
	}
	
	public static String unfollow(String currUser, String userToUnfollow) {
		if(userToUnfollow.isEmpty())
			return "ERROR: username cannot be empty";
		
		boolean exists = false;
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(userToUnfollow))
				exists = true;
		
		if(exists) {
			if(following.get(currUser).contains(userToUnfollow)) {
				followers.get(userToUnfollow).remove(currUser);
				following.get(currUser).remove(userToUnfollow);
				try {
					update(currUser);
				} catch (RemoteException e) {
					e.printStackTrace();
				}	
			}else return "ERROR: Non segui quest'utente";
			return "OK";
		} else return "ERROR: user does not exists";
	}
	
	public synchronized void registerForCallback(InterfaceNotifyEvent clientInterface, String username) throws RemoteException{	
		if(!clients.contains(clientInterface)) {
			clients.add(clientInterface);
			System.out.println("New client registered");
		}
		
	}
	
	public synchronized void unregisterForCallback(InterfaceNotifyEvent client) throws RemoteException{
		if(clients.remove(client))
			System.out.println("CALLBACK SYSTEM: Client unregistered");
		else System.out.println("Unable to unregister client");
	}
	
	public static void update(String username) throws RemoteException{
		CallbackFollowers(username);
	}
	
	private static synchronized void CallbackFollowers(String username) throws RemoteException{
		System.out.println("CALLBACK SYSTEM: starting callbacks");
		for(InterfaceNotifyEvent info : clients) {
			info.notifyEventListFollowers(username);
		}
		System.out.println("CALLBACK SYSTEM: Callback complete");
	}
}
