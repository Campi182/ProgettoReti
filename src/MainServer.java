import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainServer extends RemoteObject implements InterfaceUserRegistration{

	//------------------------strutture dati e variabili----------------- //
	private static final long serialVersionUID = 1L;
	
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;

	//STRUTTURA DATI CHE MEMORIZZA GLI UTENTI REGISTRATI
	//private Map<Integer, InfoRegistration> users;
	private static List<Utente> registeredUsers;
	
	
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = new ArrayList<Utente>();
	}
	
	
	
	public static void main(String[] args) {
		
		//RMI setup per registrazione
		MainServer server = new MainServer();
		try {
			InterfaceUserRegistration stub = (InterfaceUserRegistration) UnicastRemoteObject.exportObject(server, 0);
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
			ByteBuffer buffer = ByteBuffer.allocate(1024);
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
					}
					else if(key.isReadable()) { //read request
						SocketChannel client = (SocketChannel) key.channel();
						client.read(buffer);
						
						String str_received = new String(buffer.array(), StandardCharsets.UTF_8).trim();
						String[] split_str = str_received.split(" ");
						
						System.out.println("Command requested: " + str_received);
						
						switch(split_str[0]) {
						case "login":
							System.out.println("LOGIN REQUESTED");
							//boolean tmp = login(split_str[1], split_str[2]);
							break;
						case "logout":
							System.out.println("LOGOUT");
							break;
						default:
							System.out.println("ERROR: Incorrect command");
							break;
							
						}
						key.interestOps(SelectionKey.OP_WRITE);
					}
					
					//DA CAMBIARE
					else if(key.isWritable()) { //write requests
						//SEND RESPONSE
						SocketChannel client = (SocketChannel) key.channel();
						client.write(buffer);
						buffer.clear();
						key.interestOps(SelectionKey.OP_READ);
					}
				}catch(ClosedChannelException e) {
					e.printStackTrace();
				}
			}
			}
		
		}catch(IOException e) {}
		
		
	}//MAIN
	
	//Method of RMI interface
	public void register(String username, String password, List<String> tags) throws RemoteException{
		//Manca controllo unicita username, crittografare psw
		//username a unico, non ho bisogno di IdUser
		
		System.out.println("Requested register: " + username + " " + password);
		Utente user = new Utente(username, password, tags);
		registeredUsers.add(user);
		System.out.println("Registration success");
	}

	/*
	//Method of RMI interface (only for debugging)
	public List<String> getUsernames(){
		List<String> u = new ArrayList<String>();
		for(Integer key : registeredUsers.keySet())
			u.add(users.get(key).getUsername());
		return u;
	}
	 */

	public static boolean login(String username, String password) {
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(username)) {
				if(u.getPassword().equals(password)) {		//cambiare controllo della password (deve esserer crittografato)
					return true;
				}
			}
		}
		return false;
	}
	
}
