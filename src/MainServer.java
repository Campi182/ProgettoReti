import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.Buffer;
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

import com.sun.net.httpserver.Authenticator.Result;

public class MainServer extends RemoteObject implements InterfaceUserRegistration{

	//------------------------strutture dati e variabili----------------- //
	private static final long serialVersionUID = 1L;
	
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;

	//STRUTTURA DATI CHE MEMORIZZA GLI UTENTI REGISTRATI
	//Utente <username, password, tags>
	private static List<Utente> registeredUsers;
	
	
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = new ArrayList<Utente>();
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
						//buffer.clear();
						
						
						switch(split_str[0]) {
						case "login":
							ResponseMessage<ResponseData> res_Login = login(split_str[1], split_str[2]);
							if(res_Login.getCode().equals("OK"))
								key.attach(split_str[1]);
							
							
							//Send response
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(res_Login);
							res = baos.toByteArray();
							break;
							
						case "logout":
							resString = logout((String)key.attachment());
							if(resString.equals("OK"))	key.attach(null);
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							
							break;
						case "list":	//listUsers && list following
							if(split_str[1].equals("users")) {	//list users
								ResponseMessage<Utente> res_listUsers = listUsers((String)key.attachment());
								baos = new ByteArrayOutputStream();
								oos = new ObjectOutputStream(baos);
								oos.writeObject(res_listUsers);
								res = baos.toByteArray();
							}
							break;
						default:	//da rifare
							resString = "ERROR: Command not found";
							baos = new ByteArrayOutputStream();
							System.out.println(baos.size());
							oos = new ObjectOutputStream(baos);
							System.out.println(baos.size());
							oos.writeObject("Ciao mondo");	
							System.out.println("CIAOE");
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
				}catch(IOException e) {
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
	public void register(String username, String password, List<String> tags) throws RemoteException{
		//Manca controllo unicita username, crittografare psw
		//username a unico, non ho bisogno di IdUser
		
		System.out.println("Requested register: " + username + " " + password);
		Utente user = new Utente(username, password, tags);
		registeredUsers.add(user);
		System.out.println("Registration success");
	}

	public static ResponseMessage<ResponseData> login(String username, String password) {
		ResponseMessage<ResponseData> response;
		String code = null;
		boolean tmp = false;
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(username)) {
				if(u.getPassword().equals(password)) {		//cambiare controllo della password (deve esserer crittografato)
					tmp = true;
					
				} else code = "ERROR: wrong password";
			}
		}
		if(!tmp && code == null) 
			code = "ERROR: User not found, register first";
			
		if(!tmp) response = new ResponseMessage<>(code, null);
		else response = new ResponseMessage<>("OK", null);
		
		return response;
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
						System.out.println(u.getUsername());
					}
				}
			}
		}
		
		if(UsersCommonTags.isEmpty())
			return new ResponseMessage<>("Non ci sono utenti con tag in comune", null);
		else return new ResponseMessage<>("OK", UsersCommonTags);
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

	
	
}
