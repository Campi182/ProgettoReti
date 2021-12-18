import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;




public class MainServer extends RemoteObject implements InterfaceServerRMI{

	//------------------------strutture dati e variabili----------------- //
	private static final long serialVersionUID = 1L;
	
	//variabili dal configfile
	private static String indServer;
	private static int TCPPORT;
	private static int UDPPORT;
	private static int MCASTPORT;
	private static String indMulticast;
	private static String regHost;
	private static int regPort;
	private static int timeout;
	private static int ricompensaAutore;
	private static int periodoCalcolo;
	
	
	public static final String registrationInfoFile = "registration.json";
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;
	private static List<CallbackInfo> clients; 

	private static GsonBuilder builder;
	private static Gson gson;
	//STRUTTURA DATI CHE MEMORIZZA GLI UTENTI REGISTRATI
	//Utente <username, password, tags>
	private static List<Utente> registeredUsers;
	private static Map<String, ArrayList<String>> followers;
	private static Map<String, ArrayList<String>> following;
	private static Map<Integer, Post> listPosts;
	private static int IdPost;
	
	
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = new ArrayList<Utente>();
		clients = new ArrayList<>();
		followers = new HashMap<>();
		following = new HashMap<>();
		listPosts = new HashMap<>();
		IdPost = 1;
		builder = new GsonBuilder();
		gson = builder.create();
	}
	
	
	
	public void start() {
		
		//RMI setup per registrazione
		//strutture per spedire le risposte/oggetti al client
		ByteArrayOutputStream baos;
		ObjectOutputStream oos;
		byte[] res = new byte[512];
		
		String resString;		
		
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
								resString = "ERROR: Usage: unfollow <username>";
							else {
								resString = unfollow((String) key.attachment(), split_str[1]);
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
						
						case "post":
							List<String> splitString = new ArrayList<>();
							Pattern pattern = Pattern.compile("'(.*?)'");
							java.util.regex.Matcher matcher = pattern.matcher(str_received);
							while(matcher.find()) {
								splitString.add(matcher.group(1));
							}
							if(splitString.size() != 2)
								resString = "ERROR: Usage: post <titolo> <contenuto>";
							else {
								resString = createPost((String)key.attachment(), splitString.get(0), splitString.get(1));
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "show":
							ResponseMessage<Post> resPosts = null;
							int idd = 0;
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							if(split_str.length == 1 || split_str.length > 3)
								resPosts = new ResponseMessage<>("ERROR: Usage: show feed/post <idpost>PD", null);
							else {
								if(split_str[1].equals("feed")) {
									resPosts = showFeed((String)key.attachment());
								}
								
								if(split_str[1].equals("post")) {
									if(split_str.length != 3)
										resPosts = new ResponseMessage<>("ERROR: Usage: show post <idpost>", null);
									else {
										try {
											idd = Integer.parseInt(split_str[2]);
											resPosts = showPost((String)key.attachment(), idd);
										}catch(NumberFormatException e) {
											resPosts = new ResponseMessage<>("ERROR: idpost must be a number", null);
										}
									}
								}
							}
							oos.writeObject(resPosts);
							res = baos.toByteArray();
							break;
							
						case "blog":
							ResponseMessage<Post> resMyPosts = null;
							if(split_str.length != 1)
								resMyPosts = new ResponseMessage<>("ERROR: Usage: blog", null);
							else resMyPosts = viewBlog((String)key.attachment());
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resMyPosts);
							res = baos.toByteArray();
							break;
							
						case "rate":
							int id = 0, vote;
							if(split_str.length != 3)
								resString = "ERROR: Usage: rate <idpost> <voto>";
							else {
								try {
									id = Integer.parseInt(split_str[1]);
									vote = Integer.parseInt(split_str[2]);
									resString = ratePost((String)key.attachment(), id, vote);
								}catch(NumberFormatException e) {
									resString = "ERROR: id and vote must be a number";
								}
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "comment":
							int i;
							List<String> splitStringg = new ArrayList<>();
							Pattern pat = Pattern.compile("'(.*?)'");
							java.util.regex.Matcher mat = pat.matcher(str_received);
							while(mat.find()) {
								splitStringg.add(mat.group(1));
							}
							if(splitStringg.size() != 1)
								resString = "ERROR: Usage: comment <idPost> <comment>";
							else {
								try {
									i = Integer.parseInt(split_str[1]);
									resString = addComment((String)key.attachment(), i, splitStringg.get(0));
								}catch(NumberFormatException e) {
									resString = "ERROR: idPost must be a number";
								}
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
						case "delete":
							int j;
							if(split_str.length != 2)
								resString = "ERROR: Usage: delete <idPost>";
							else {
								try {
									j = Integer.parseInt(split_str[1]);
									resString = deletePost((String)key.attachment(), j);
								}catch(NumberFormatException e) {
									resString = "ERROR: IdPost must be a number";
								}
								
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "rewin":
							int p;
							if(split_str.length != 2)
								resString = "ERROR: Usage: rewin <idPost>";
							else {
								try {
									p = Integer.parseInt(split_str[1]);
									resString = rewin((String)key.attachment(), p);
								}catch(NumberFormatException e) {
									resString = "ERROR: IdPost must be a number";
								}
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "wallet":
							ResponseWallet<Transaction> resWallet = null;
							if(split_str.length != 1)
								resWallet = new ResponseWallet<>("ERROR: Usage: wallet", null, 0);
							else resWallet = getWallet((String)key.attachment());
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resWallet);
							res = baos.toByteArray();
							break;
							
						case "quit":
							if(key.attachment() != null)
								logout((String)key.attachment());
							client.close();
							key.cancel();
							break;
						default:
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
		
		//Scrivo nel file json i dati
		try(FileOutputStream os = new FileOutputStream(registrationInfoFile);
			FileChannel oc = os.getChannel();
				){
			ByteBuffer buf = ByteBuffer.allocate(8192);
			
			byte[] data = gson.toJson(user).getBytes();
			for(int l = 0; l < data.length; l+=8192) {
				buf.clear();
				buf.put(data, l, Math.min(8192, data.length-1));
				buf.flip();
				while(buf.hasRemaining()) oc.write(buf);
			}
			
			
		}catch(FileNotFoundException e) {
			System.err.println("File non trovato: " + e.getMessage());
			System.exit(1);
		}catch (IOException e) {
			System.err.println("Errore di I/O: " + e.getMessage());
			System.exit(1);
		}
		return "Registration success";
	}
	
	
	/**
     *  Metodo per scrivere un singolo carattere sul canale.
     *  @param channel riferimento al canale su cui scrivere
     *  @param c il carattere da scrivere
     */
    public static void writeChar(FileChannel channel, char c)
    throws IOException {
        CharBuffer charBuffer = CharBuffer.wrap(new char[]{c});
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        // Leggo il contenuto del buffer e lo scrivo sul canale.
        channel.write(byteBuffer);
    }
    
    
	public ResponseMessage<String> login(String username, String password) {

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
	
	public ResponseMessage<Utente> listUsers(String username) {
		System.out.println(username+" ha richiesto la lista");
		
		if(registeredUsers.isEmpty())
			return new ResponseMessage<>("No Users Registered", null);
		
		List<Utente> UsersCommonTags = new ArrayList<>();
		List<String> userTags = new ArrayList<>();
		
		for(Utente u : registeredUsers)		//prendo i tag dell'utente corrente
			if(u.getUsername().equals(username)) {
				userTags = u.getTags();
			}
		
		for(Utente u : registeredUsers) {
			if(!u.getUsername().equals(username)){
				for(String t : userTags) {
					if(u.getTags().contains(t)) {
						UsersCommonTags.add(u);
						break;
					}
				}
			}
		}
		
		if(UsersCommonTags.isEmpty())
			return new ResponseMessage<>("Non ci sono utenti con tag in comune", null);
		else return new ResponseMessage<>("OK", UsersCommonTags);
	}

	public  ResponseMessage<String> listFollowing(String username){
		if(following.get(username).isEmpty())
			return new ResponseMessage<>("Non segui nessuno", null);
		
		return new ResponseMessage<>("OK", following.get(username));
	}
	
	public String logout(String username) {
		if(registeredUsers.isEmpty())	return "ERROR: No registered users";
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(username)) {
				return "OK";
			}
		}
		return "ERROR: User doesn't exists";
	}
	
	public String follow(String currUser, String userToFollow) throws RemoteException {
		if(userToFollow.isEmpty())
			return "ERROR: username cannot be empty";
		if(currUser.equals(userToFollow))
			return "ERROR: Non puoi seguire te stesso";
		
		boolean exists = false;
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(userToFollow))
				exists = true;
		
		if(exists) {
			if(!following.get(currUser).contains(userToFollow)) {
				followers.get(userToFollow).add(currUser);
				following.get(currUser).add(userToFollow);
					update(currUser, userToFollow, 1);
				return "OK";
			} else return "ERROR: Segui gia' quest'utente";
		} else return "ERROR: user does not exists";
		
	}
	
	public String unfollow(String currUser, String userToUnfollow) throws RemoteException {
		if(userToUnfollow.isEmpty())
			return "ERROR: username cannot be empty";
		if(currUser.equals(userToUnfollow))
			return "ERROR: Non puoi unfolloware te stesso";
		
		boolean exists = false;
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(userToUnfollow))
				exists = true;
		
		if(exists) {
			if(following.get(currUser).contains(userToUnfollow)) {
				followers.get(userToUnfollow).remove(currUser);
				following.get(currUser).remove(userToUnfollow);
				update(currUser, userToUnfollow, 0);
			}else return "ERROR: Non segui quest'utente";
			return "OK";
		} else return "ERROR: user does not exists";
	}
	
	public String createPost(String user, String title, String contenuto) {
		if(title.isEmpty() || contenuto.isEmpty())
			return "ERROR: title and content cannot be empty";
		
		if(title.length() > 20)
			return "ERROR: la lunghezza del titolo deve essere di massimo 20 caratteri";
		if(contenuto.length() > 500)
			return "ERROR: la lunghezza del contenuto deve essere di massimo 500 caratteri";
		
		Post post = new Post(IdPost, user, title, contenuto);
		listPosts.put(IdPost, post);
		IdPost++;
		return "OK";
	}
	
	public ResponseMessage<Post> showFeed(String user){
		
		if(listPosts.isEmpty())
			return new ResponseMessage<>("Non ci sono post nel social", null);
		List<Post> postInFeed = new ArrayList<>();
		
		for(var entry : listPosts.entrySet()) {
			if(following.get(user).contains(entry.getValue().getAutore()) || doUserFollowAnyRewiner(user, entry.getValue().getId())) {	//se chi ha richiesto il feed segue l'autore del post corrente
				postInFeed.add(entry.getValue());
			}
		}
		if(postInFeed.isEmpty())
			return new ResponseMessage<>("Non hai post nel feed", postInFeed);
		return new ResponseMessage<>("OK", postInFeed);
	}
	
	public ResponseMessage<Post> viewBlog(String user){
		if(listPosts.isEmpty())
			return new ResponseMessage<>("Non ci sono post nel social", null);
		
		List<Post> myPosts = new ArrayList<>();

		for(var entry : listPosts.entrySet()) {
			if(entry.getValue().getAutore().equals(user) || entry.getValue().getRewiners().contains(user)) {
				myPosts.add(entry.getValue());
			}
		}
		
		if(myPosts.isEmpty())
			return new ResponseMessage<>("Non hai post nel blog", myPosts);
		return new ResponseMessage<>("OK", myPosts);
	}
	
	public String ratePost(String user, int idPost, int vote) {
		if(vote != 1 && vote != -1)
			return "ERROR: Vote must be 1 or -1";
		if(!listPosts.containsKey(idPost))
			return "ERROR: IdPost non esistente";
		if(listPosts.get(idPost).getAutore().equals(user))
			return "ERROR: Non puoi votare un post di cui sei autore";
		if(!following.get(user).contains(listPosts.get(idPost).getAutore()))
			if(!doUserFollowAnyRewiner(user, idPost))
				return "ERROR: Non segui l'autore di questo post e nessun rewiner del post";
		if(listPosts.get(idPost).getVoters().contains(user))
			return "ERROR: hai gia votato questo post";
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Voto voto = new Voto(vote, timestamp);
		if(vote == 1)
			listPosts.get(idPost).setUpvote(vote);
		else listPosts.get(idPost).setDownvote(vote);
		
		listPosts.get(idPost).addVote(user, voto);
		return "OK";
		
	}
	
	public String addComment(String user, int idPost, String comment) {
		if(!listPosts.containsKey(idPost))
			return "ERROR: IdPost non esistente";
		if(listPosts.get(idPost).getAutore().equals(user))
			return "ERROR: Non puoi commentare un post di cui sei autore";
		if(!following.get(user).contains(listPosts.get(idPost).getAutore()))
			if(!doUserFollowAnyRewiner(user, idPost))
				return "ERROR: Non segui l'autore di questo post e nessun rewiner del post";
		
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		listPosts.get(idPost).addComment(user, comment, timestamp);
		return "OK";
	}
	
	public ResponseMessage<Post> showPost(String user, int idpost){
		if(!listPosts.containsKey(idpost))
			return new ResponseMessage<>("IdPost non esistente", null);
		if(!following.get(user).contains(listPosts.get(idpost).getAutore()))	//se l'user che richiede non segue l'autore del post e non segue qualcuno che l ha retwittato
			if(!doUserFollowAnyRewiner(user, idpost))
				return new ResponseMessage<>("ERROR: Non segui l'autore del post e nessun rewiner del post", null);
		//utilizzo una lista perche cosi e' il tipo ResponseMessage, ma in questo caso e' formata solo da un post
		List<Post> post = new ArrayList<>();
		post.add(listPosts.get(idpost));
		return new ResponseMessage<>("OK", post);
	}
	
	public ResponseWallet<Transaction> getWallet(String user){
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(user))
				return new ResponseWallet<>("OK",u.getAllTransactions(), u.getWincoins());
		}
		return new ResponseWallet<>("ERROR: User not found", null, 0);
	}
	
	public String deletePost(String user, int idpost) {
		if(!listPosts.containsKey(idpost))
			return "ERROR: IdPost non esistente";
		if(!listPosts.get(idpost).getAutore().equals(user))
			return "ERROR: Non puoi cancellare un post di cui non sei autore";
		
		listPosts.remove(idpost);
		return "OK";
	}
	
	public String rewin(String user, int idpost) {
		if(!listPosts.containsKey(idpost))
			return "ERROR: IdPost non esistente";
		if(!following.get(user).contains(listPosts.get(idpost).getAutore()))	//se l'utente segue l'autore del post da retwittare
			if(!doUserFollowAnyRewiner(user, idpost))
				return "ERROR: Non segui l autore del post";
		
		listPosts.get(idpost).addRewiner(user);
		return "OK";
	}
	
	public boolean doUserFollowAnyRewiner(String user, int idpost) {
		for(String u : following.get(user)) {
			if(listPosts.get(idpost).getRewiners().contains(u))
				return true;
		}
		return false;
	}

	public synchronized void registerForCallback(InterfaceNotifyEvent clientInterface, String username) throws RemoteException{	
		boolean contains = clients.stream().anyMatch(client -> clientInterface.equals(client.getClient()));
		
		if(!contains) {
			clients.add(new CallbackInfo(clientInterface, username));
			System.out.println("CALLBACK SYSTEM: New client registered");
		}
		
	}
	
	public synchronized void unregisterForCallback(InterfaceNotifyEvent Client) throws RemoteException{
		CallbackInfo user = clients.stream().filter(client -> Client.equals(client.getClient())).findAny().orElse(null);
		
		if(user != null) {
			clients.remove(user);
			System.out.println("CALLBACK SYSTEM: Client unregistered");
		}
		else System.out.println("Unable to unregister client");
	}
	
	public void update(String currUser, String userToFollow, int op) throws RemoteException{
		CallbackFollowers(currUser, userToFollow, op);
	}
	
	private synchronized void CallbackFollowers(String currUser, String userToFollow, int op) throws RemoteException{
		LinkedList<InterfaceNotifyEvent> errors = new LinkedList<>();
		System.out.println("CALLBACK SYSTEM: starting callbacks");
		for(CallbackInfo info : clients) {
			InterfaceNotifyEvent client = info.getClient();
			if(info.getUsername().equals(userToFollow)) {
				try {
					client.notifyEventListFollowers(currUser, op);
				}catch(RemoteException e) {
					errors.add(client);
				}
			}
		}
		if(!errors.isEmpty()) {
			System.out.println("CALLBACK SYSTEM: Unregister clients that caused an error");
			for(InterfaceNotifyEvent n : errors) unregisterForCallback(n);
		}
		System.out.println("CALLBACK SYSTEM: Callback complete");
	}

	public static void setupServer(String path) throws IOException {
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String str;
		int port;
		
		while((str = br.readLine()) != null) {		//lettura del config file
			String[] splitLine = str.split(" ");
			switch(splitLine[0]) {
			case "#":
				continue;
			case "TCPPORT":	
				try {
					port = Integer.parseInt(splitLine[2]);
					TCPPORT = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;
				
			case "UDPPORT":
				try {
					port = Integer.parseInt(splitLine[2]);
					TCPPORT = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;
			case "MCASTPORT":
				try {
					port = Integer.parseInt(splitLine[2]);
					MCASTPORT = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;

			case "SERVER":
				indServer = splitLine[2];
				break;
				
			case "MULTICAST":
				indMulticast = splitLine[2];
				break;
				
			case "REGHOST":
				regHost = splitLine[2];
				break;
				
			case "REGPORT":
				try {
					port = Integer.parseInt(splitLine[2]);
					regPort = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;

			case "TIMEOUT":
				try {
					port = Integer.parseInt(splitLine[2]);
					timeout = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;
			
			case "RICOMPENSAAUTORE":
				try {
					port = Integer.parseInt(splitLine[2]);
					ricompensaAutore = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;
				
			case "PERIODOCALCOLO":
				try {
					port = Integer.parseInt(splitLine[2]);
					periodoCalcolo = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. TCPPORT Must be a number");
				}
				break;

			}
		}
	}
	
	public static void main(String[] args) {
		MainServer server = new MainServer();
		try {
			InterfaceServerRMI stub = (InterfaceServerRMI) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.createRegistry(5000);
			Registry r = LocateRegistry.getRegistry(5000);
			r.rebind("Server", stub);
			System.out.println("Server pronto");
		}catch(RemoteException e) {
			System.err.println("Errore: " + e.getMessage());
		}

		String path = "./config.txt";
		try {
			setupServer(path);
		} catch (IOException e) {
			e.printStackTrace();
		}		
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new MainCalcoloRicompense(listPosts, registeredUsers, ricompensaAutore), 0, periodoCalcolo, TimeUnit.SECONDS);
		server.start();
	}
}
