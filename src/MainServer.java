import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
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
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;




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
	
	
	public static final String dirDatabase = "./Database/";
	private static int PORT = 6789; //!!DA PRENDERE DAL CONFIG FILE
	private static Selector selector = null;
	private static List<CallbackInfo> clients; 

	//STRUTTURA DATI CHE MEMORIZZA GLI UTENTI REGISTRATI
	private static List<Utente> registeredUsers;
	private static Map<String, List<String>> followers;
	private static Map<String, List<String>> following;
	private static Map<Integer, Post> listPosts;
	private static int IdPostglobal;
	
	private static Format formatter;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = new ArrayList<Utente>();
		clients = new ArrayList<>();
		followers = new HashMap<>();
		following = new HashMap<>();
		listPosts = new HashMap<>();
		IdPostglobal = 1;
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	
	
	public void start() throws NoSuchAlgorithmException {
		
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
								resPosts = new ResponseMessage<>("ERROR: Usage: show feed/post <idpost>", null);
							else {
								if(split_str[1].equals("feed")) {
									resPosts = showFeed((String)key.attachment());
								}
								
								else if(split_str[1].equals("post")) {
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
								else resPosts = new ResponseMessage<>("ERROR: Usage show feed/post <idpost>", null);
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
							if(split_str.length == 1 ) {
								resWallet = getWallet((String)key.attachment());
							}
							else if(split_str.length == 2 && split_str[1].equals("btc")) {
								resWallet = getWalletInBitcoin((String)key.attachment());
							}
							else resWallet = new ResponseWallet<>("ERROR: Usage: wallet / wallet btc", null, 0);
							
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
		Utente user = null;
		try {
			String cipherpsw = Hash.bytesToHex(Hash.sha256(password));
			user = new Utente(username, cipherpsw, tags);
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		registeredUsers.add(user);
		followers.put(username, new ArrayList<>());
		following.put(username, new ArrayList<>());
		
		//Scrivo nel file json i dati
		String file = dirDatabase+username+".json";
		updateDatabase.updateDbUser(file, user);
		return "Registration success";
	}
	    
	public ResponseMessage<String> login(String username, String password) throws NoSuchAlgorithmException {

		String code = null;
		boolean tmp = false;
		List<String> followList;	//invio all'user la lista dei suoi seguaci
		
		if(username.isEmpty() || password.isEmpty())
			code = "ERROR: Username and password cannot be empty";
		else {
			for(Utente u : registeredUsers) {
				if(u.getUsername().equals(username)) {
					if(u.getPassword().equals(Hash.bytesToHex(Hash.sha256(password)))) {
						tmp = true;
						code = "OK";
					} else {
						code = "ERROR: wrong password";
						System.out.println("Utente: " +Hash.bytesToHex(Hash.sha256(u.getPassword())));
						System.out.println("Scritta: " + Hash.bytesToHex(Hash.sha256(password)));
					}
				}
			}
		}
		
		if(tmp) {
			followList = new ArrayList<>(followers.get(username));
		}
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
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(userToFollow)) {
				if(!following.get(currUser).contains(userToFollow)) {	//se l'utente corrente non segue usertofollow
					followers.get(userToFollow).add(currUser);
					following.get(currUser).add(userToFollow);
					update(currUser, userToFollow, 1);
					
					String file = dirDatabase+"Follow/";
					updateDatabase.updateDbFollow(followers, file + "followers.json");
					updateDatabase.updateDbFollow(following, file + "following.json");
					return "OK";
				} else return "ERROR: Segui gia' quest'utente";
			}
		}
		return "ERROR: Quest'utente non esiste";
	}
	
	public String unfollow(String currUser, String userToUnfollow) throws RemoteException {
		if(userToUnfollow.isEmpty())
			return "ERROR: username cannot be empty";
		if(currUser.equals(userToUnfollow))
			return "ERROR: Non puoi unfolloware te stesso";
		
		
		for(Utente u : registeredUsers) {
			if(u.getUsername().equals(userToUnfollow)) {
				if(following.get(currUser).contains(userToUnfollow)) {
					followers.get(userToUnfollow).remove(currUser);
					following.get(currUser).remove(userToUnfollow);
					update(currUser, userToUnfollow, 0);
					
					String file = dirDatabase+"Follow/";
					updateDatabase.updateDbFollow(followers, file + "followers.json");
					updateDatabase.updateDbFollow(following, file + "following.json");
					return "OK";
				}else return "ERROR: Non segui quest'utente";
			}
		}
		return "ERROR: Quest'utente non esiste";
	}
	
	public String createPost(String user, String title, String contenuto) {
		if(title.isEmpty() || contenuto.isEmpty())
			return "ERROR: title and content cannot be empty";
		
		if(title.length() > 20)
			return "ERROR: la lunghezza del titolo deve essere di massimo 20 caratteri";
		if(contenuto.length() > 500)
			return "ERROR: la lunghezza del contenuto deve essere di massimo 500 caratteri";
		
		Post post = new Post(IdPostglobal, user, title, contenuto);
		listPosts.put(IdPostglobal, post);
		String path = dirDatabase+"Post/"+Integer.toString(IdPostglobal)+".json";
		updateDatabase.updateDbPost(post, path);
		IdPostglobal++;
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
		String s = formatter.format(timestamp);
		Voto voto = new Voto(vote, s);
		if(vote == 1)
			listPosts.get(idPost).setUpvote(vote);
		else listPosts.get(idPost).setDownvote(vote);
		
		listPosts.get(idPost).addVote(user, voto);
		
		String path = dirDatabase+"Post/"+Integer.toString(idPost)+".json";
		updateDatabase.updateDbPost(listPosts.get(idPost), path);
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
		String s = formatter.format(timestamp);
		listPosts.get(idPost).addComment(user, comment, s);
		
		String path = dirDatabase+"Post/"+Integer.toString(idPost)+".json";
		updateDatabase.updateDbPost(listPosts.get(idPost), path);
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
	
	public ResponseWallet<Transaction> getWalletInBitcoin(String user){
		String URL = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";
		try {
			java.net.URL url = new URL(URL);
			InputStream in = url.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String n = reader.readLine();
			int cambio = Integer.parseInt(n);	// 1 euro = "cambio" BITCOIN
			reader.close();
			in.close();
			double soldi = 0; double value = 0;
			for(Utente u : registeredUsers) {
				if(u.getUsername().equals(user))
					soldi = u.getWincoins();
			}
			value = Math.round((soldi/cambio)*100.000)/100.000;
			return new ResponseWallet<>("OK", null, value);
		}catch(Exception e) {
			e.printStackTrace();
			return new ResponseWallet<>("ERROR: Exception", null, 0);
		}
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
		
		String path = dirDatabase+"Post/"+Integer.toString(idpost)+".json";
		updateDatabase.updateDbPost(listPosts.get(idpost), path);
		
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
		
		//CONFIGURAZIONE DELLE VARIABILI GLOBALI
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
	
	
	
	public static void getBackupData() {
		Gson gson = new Gson();
		File directory = new File(dirDatabase);	//directory dove sono memorizzati i file dei profili utente
		File[] files = directory.listFiles();
		for(File f : files) {
			try {
				if(f.isFile()) {
					InputStream is = new FileInputStream(f);
					JsonReader reader = new JsonReader(new InputStreamReader(is));
					String username = null;
					String password = null;
					List<Transaction> transazioni = null;
					List<String> tags = null;
					double wincoins = 0;
					
					reader.beginObject();
					while(reader.hasNext()) {
						String field = reader.nextName();
						if(field.equals("username"))
							username = reader.nextString();
						else if(field.equals("password"))
							password = reader.nextString();
						else if(field.equals("tags")) {
							reader.beginArray();
							tags = readTags(reader);
							reader.endArray();
						}
						else if(field.equals("wincoins"))
							wincoins = reader.nextDouble();
						else if(field.equals("transazioni")) {
							transazioni = readTransazioni(reader);
						}
						else reader.skipValue();
					}
					reader.endObject();
					//reader.close();
					Utente u = new Utente(username, password, null);
					registeredUsers.add(u);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		File dir2 = new File(dirDatabase+"Post");	//cartella salvataggio Post
		File[] files2 = dir2.listFiles();
		for(File f : files2) {
			try {
				if(f.isFile()) {
					InputStream is = new FileInputStream(f);
					JsonReader reader = new JsonReader(new InputStreamReader(is));
					String autore = null;
					String titolo = null;
					String contenuto = null;
					int upvote = 0; int downvote = 0; int idPost = 0;
					Map<String, Voto> voti = new HashMap<>();
					Map<String, List<Comment>> commenti = new HashMap<>();
					Set<String> rewiners = new HashSet<>();
					
					reader.beginObject();
					while(reader.hasNext()) {
						String field = reader.nextName();
						if(field.equals("IdPost"))
							idPost = reader.nextInt();
						if(field.equals("upvote"))
							upvote = reader.nextInt();
						if(field.equals("downvote"))
							downvote = reader.nextInt();
						if(field.equals("autore"))
							autore = reader.nextString();
						if(field.equals("titolo"))
							titolo = reader.nextString();
						if(field.equals("contenuto"))
							contenuto = reader.nextString();
						if(field.equals("voti")) {
							reader.beginObject();
							voti = readVoti(reader);
							reader.endObject();
						}
						if(field.equals("commenti")) {
							reader.beginObject();
							commenti = readCommenti(reader);
							reader.endObject();
						}
						if(field.equals("rewiners")) {
							reader.beginArray();
							rewiners = readRewiners(reader);
							reader.endArray();
						}
					}
					reader.endObject();
					Post p = new Post(idPost, autore, titolo, contenuto);
					p.setDownvote(downvote);
					p.setUpvote(upvote);
					p.setRewiners(rewiners);
					p.setVoti(voti);
					p.setCommenti(commenti);
					listPosts.put(idPost, p);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		File fileFollowers = new File(dirDatabase+"Follow/followers.json");
		File fileFollowing = new File(dirDatabase+"Follow/following.json");
		try {
			fileFollowers.createNewFile();
			fileFollowing.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			if(fileFollowers.length() != 0) {
				@SuppressWarnings("unchecked")
				Map<String, List<String>> f = new Gson().fromJson(new FileReader(fileFollowers), Map.class);
				followers.putAll(f);
				@SuppressWarnings("unchecked")
				Map<String, List<String>> v = new Gson().fromJson(new FileReader(fileFollowing), Map.class);
				if(!v.isEmpty())
					following.putAll(v);
				for(var entry : followers.entrySet())
					System.out.println("FOLLOWERS " + entry.getKey() +": "+entry.getValue());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	public static List<String> readTags(JsonReader reader) throws IOException{
		List<String> tags = new ArrayList<>();
		while(reader.hasNext()) {
			JsonToken nextToken = reader.peek();
			String tag = null;
			if(JsonToken.STRING.equals(nextToken)) {
				tag = reader.nextString();
				tags.add(tag);
			} else reader.skipValue();
		}
		return tags;
	}
	
	public static Set<String> readRewiners(JsonReader reader) throws IOException{
		Set<String> rewiners = new HashSet<>();
		while(reader.hasNext()) {
			JsonToken nextToken = reader.peek();
			String name = null;
			if(JsonToken.STRING.equals(nextToken)) {
				name = reader.nextString();
				rewiners.add(name);
			}else reader.skipValue();
		}
		return rewiners;
	}
	
	public static List<Transaction> readTransazioni(JsonReader reader) throws IOException{
		List<Transaction> transazioni = new ArrayList<>();
		reader.beginArray();
		while(reader.hasNext()) {
			double value = 0;
			String timestamp = null;
			reader.beginObject();
			while(reader.hasNext()) {
				String field = reader.nextName();
				if(field.equals("value")) {
					value = reader.nextDouble();
				}
				else if(field.equals("timestamp")) {
					timestamp = reader.nextString();
				}
				else reader.skipValue();
			}
			reader.endObject();
			transazioni.add(new Transaction(value, timestamp));
		}
		reader.endArray();
		return transazioni;
	}
	
	public static Map<String, Voto> readVoti(JsonReader reader) throws IOException {
		Map<String, Voto> voti = new HashMap<>();
		
		while(reader.hasNext()) {
			String key = null;
			Voto voto = null;
			JsonToken nextToken = reader.peek();
			if(JsonToken.STRING.equals(nextToken)) {
				int v = 0; 
				String timestamp = null;
				key = reader.nextString();
				reader.beginObject();
				while(reader.hasNext()) {
					String field = reader.nextName();
					if(field.equals("voto"))
						v = reader.nextInt();
					if(field.equals("timestamp"))
						timestamp = reader.nextString();
				}
				voto = new Voto(v, timestamp);
				voti.put(key, voto);
			} else reader.skipValue();
		}
		return voti;
	}
	
	public static Map<String, List<Comment>> readCommenti(JsonReader reader) throws IOException{
		Map<String, List<Comment>> commenti = new HashMap<>();
		int ok = 0;
		String name = null;
		while(reader.hasNext()) {
			String key = null;
			List<Comment> comments = null;
			while(reader.hasNext()) {
				JsonToken nextToken = reader.peek();
				if(ok == 0)
					name = reader.nextName();
				if(JsonToken.NAME.equals(nextToken) && ok == 0) {
					key = name;
					ok = 1;
				}else if(ok == 1) {
					comments = readComm(reader);
					ok = 2;
				}
				else reader.skipValue();
			}
			ok = 0;
			commenti.put(key, comments);
		}
		return commenti;
	}
	
	public static List<Comment> readComm(JsonReader reader) throws IOException{
		List<Comment> ret = new ArrayList<>();
		reader.beginArray();		
		while(reader.hasNext()) {
			String autore = null;
			String commento = null;
			String timestamp = null;
			reader.beginObject();
			while(reader.hasNext()) {
				String field = reader.nextName();
				if(field.equals("autore"))
					autore = reader.nextString();
				else if(field.equals("commento"))
					commento = reader.nextString();
				else if(field.equals("timestamp"))
					timestamp = reader.nextString();
				else reader.skipValue();
			}
			reader.endObject();
			ret.add(new Comment(autore, commento, timestamp));
		}
		reader.endArray();
		return ret;
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException {
		
		if(args.length != 1) {
			System.err.println("Usage: needs path of config file");
			System.exit(1);
		}
		
		MainServer server = new MainServer();
		String path = args[0];
		try {
			setupServer(path);
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		getBackupData();
		
		try {
			InterfaceServerRMI stub = (InterfaceServerRMI) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.createRegistry(5000);
			Registry r = LocateRegistry.getRegistry(5000);
			r.rebind("Server", stub);
			System.out.println("Server pronto");
		}catch(RemoteException e) {
			System.err.println("Errore: " + e.getMessage());
		}

		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new MainCalcoloRicompense(listPosts, registeredUsers, ricompensaAutore), 0, periodoCalcolo, TimeUnit.SECONDS);
		server.start();
	}
}
