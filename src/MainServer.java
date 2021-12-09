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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainServer extends RemoteObject implements InterfaceUserRegistration{

	//------------------------strutture dati e variabili----------------- //
	private static int IdUser = 1;
	private static final long serialVersionUID = 1L;
	
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;

	private Map<Integer, InfoRegistration> users;
	
	
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		users = new HashMap<Integer, InfoRegistration>();
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
							System.out.println("LOGIN");
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
		
		
		}catch(IOException e) {}
		
		
	}//MAIN
	
	//Method of RMI interface
	public void register(String username, String password, List<String> tags){
		InfoRegistration user = new InfoRegistration(username, password, tags);
		users.put(IdUser, user);
		IdUser++;
	}

	//Method of RMI interface (only for debugging)
	public List<String> getUsernames(){
		List<String> u = new ArrayList<String>();
		for(Integer key : users.keySet())
			u.add(users.get(key).getUsername());
		return u;
	}

}
