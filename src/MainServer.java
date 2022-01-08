import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
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
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;




public class MainServer extends RemoteObject implements InterfaceServerRMI{

	//------------------------strutture dati e variabili----------------- //
	private static final long serialVersionUID = 1L;
	
	//variabili dal configfile
	private static int TCPPORT;
	private static int UDPPORT;
	private static int regPort;
	private static String MCAST;
	private static int ricompensaAutore;
	private static int periodoCalcolo;
	public static String dirDatabase;
	
	private static final int bufsize = 8192;
	
	private static List<Utente> registeredUsers;
	private static List<CallbackInfo> clients; 	//lista dei clients registrati per la callback
	private static Map<String, List<String>> followers;
	private static Map<String, List<String>> following;
	private static Map<Integer, Post> listPosts;
	private static int IdPostglobal;
	
	private static Format formatter;
	//---------------------------------------------------------------------/
	
	//constructor
	public MainServer() {
		registeredUsers = Collections.synchronizedList(new ArrayList<Utente>());
		clients = new ArrayList<CallbackInfo>();
		followers = new ConcurrentHashMap<>();
		following = new ConcurrentHashMap<>();
		listPosts = new ConcurrentHashMap<>();
		IdPostglobal = 1;
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	public void start(){
		
		//RMI setup per registrazione
		//strutture per spedire le risposte/oggetti al client
		ByteArrayOutputStream baos;
		ObjectOutputStream oos;
		byte[] res = new byte[bufsize];
		String resString;
		int number = 0;
		
		try {
			//Listening per nuove connessioni
			ServerSocketChannel serverSocket = ServerSocketChannel.open();
			serverSocket.socket().bind(new InetSocketAddress(TCPPORT));
			serverSocket.configureBlocking(false);
			
			Selector selector = Selector.open();
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("SERVER IS ON - pronto sulla porta "+TCPPORT);
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
					if(key.isAcceptable()) { //accetto la connessione; registro sul selettore il canale
						ServerSocketChannel srv = (ServerSocketChannel) key.channel();
						SocketChannel client = srv.accept();
						client.configureBlocking(false);
						System.out.println("Accepted connection from " + client);
						client.register(selector, SelectionKey.OP_READ);
						key.attach(null);
					}
					else if(key.isReadable()) { //read request
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(bufsize);
						client.read(buffer);
						
						String str_received = new String(buffer.array()).trim();	//trasformo in String il buffer
						String[] split_str = str_received.split(" ");
						System.out.println("Command requested: "+ str_received);
						
						switch(split_str[0]) {
						case "login":
							ResponseMessage<String> res_Login;
							if(split_str.length != 3)
								res_Login = new ResponseMessage<>("ERROR: Usage: login <username> <password>", null);
							else {
								res_Login = login(split_str[1], split_str[2]);
								if(res_Login.getCode().equals("OK"))
									key.attach(split_str[1]);	//al login mi salvo l'username corrente
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
							if(split_str.length != 2) {
								res_listUsers = new ResponseMessage<>("ERROR: Usage: list users / list following", null);
								oos.writeObject(res_listUsers);
							}
							else if(split_str[1].equals("users")) {
								res_listUsers = listUsers((String)key.attachment());
								oos.writeObject(res_listUsers);
							}
							else if(split_str[1].equals("following")) {
								res_listFollowing = listFollowing((String)key.attachment());
								oos.writeObject(res_listFollowing);
							}
							else {
								res_listUsers = new ResponseMessage<>("ERROR: Usage: list users / list following", null);
								oos.writeObject(res_listUsers);
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
							//ogni parte compresa tra ' ' viene splittata
							Pattern pattern = Pattern.compile("'(.*?)'");
							java.util.regex.Matcher matcher = pattern.matcher(str_received);
							while(matcher.find()) {
								splitString.add(matcher.group(1));
							}
							
							if(splitString.size() != 2)
								resString = "ERROR: Usage: post '<titolo>' '<contenuto>'";
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
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							if(split_str.length == 1 || split_str.length > 3)
								resPosts = new ResponseMessage<>("ERROR: Usage: show feed/post <idpost>", null);
							else if(split_str[1].equals("feed")) {
									resPosts = showFeed((String)key.attachment());
							}
							else if(split_str[1].equals("post")) {
								if(split_str.length != 3)
									resPosts = new ResponseMessage<>("ERROR: Usage: show post <idpost>", null);
								try {
									number = Integer.parseInt(split_str[2]);
									resPosts = showPost((String)key.attachment(), number);
								}catch(NumberFormatException e) {
									resPosts = new ResponseMessage<>("ERROR: idpost must be a number", null);
								}
							
							}
							else resPosts = new ResponseMessage<>("ERROR: Usage show feed/post <idpost>", null);
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
							List<String> splitStringg = new ArrayList<>();
							if(split_str.length < 3)
								resString = "ERROR: Usage: comment <idPost> '<comment>'";
							else {
								//ogni parte compresa tra ' ' viene splittata
								Pattern pat = Pattern.compile("'(.*?)'");
								java.util.regex.Matcher mat = pat.matcher(str_received);
								while(mat.find()) {
									splitStringg.add(mat.group(1));
								}
								if(splitStringg.size() != 1)	//devo avere solo una parte
									resString = "ERROR: Usage: comment <idPost> '<comment>'";
								else {
									try {
										number = Integer.parseInt(split_str[1]);
										resString = addComment((String)key.attachment(), number, splitStringg.get(0));
									}catch(NumberFormatException e) {
										resString = "ERROR: idPost must be a number";
									}
								}
							}
							baos = new ByteArrayOutputStream();
							oos = new ObjectOutputStream(baos);
							oos.writeObject(resString);
							res = baos.toByteArray();
							break;
							
						case "delete":
							if(split_str.length != 2)
								resString = "ERROR: Usage: delete <idPost>";
							else {
								try {
									number = Integer.parseInt(split_str[1]);
									resString = deletePost((String)key.attachment(), number);
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
							if(split_str.length != 2)
								resString = "ERROR: Usage: rewin <idPost>";
							else {
								try {
									number = Integer.parseInt(split_str[1]);
									resString = rewin((String)key.attachment(), number);
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
					else if(key.isWritable()) { //write requests
						//SEND RESPONSE
						SocketChannel client = (SocketChannel) key.channel();
						client.write(ByteBuffer.wrap(res));
						key.interestOps(SelectionKey.OP_READ);
					}
				}catch(IOException | CancelledKeyException e) {
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
		Utente user = null;
		
		synchronized (registeredUsers) {
			for(Utente u : registeredUsers)
				if(u.getUsername().equals(username))
					return "ERROR: this username already exists";
			
			try {
				String cipherpsw = Hash.bytesToHex(Hash.sha256(password));
				user = new Utente(username, cipherpsw, tags);
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			}
			registeredUsers.add(user);
		}
		
		followers.putIfAbsent(username, new ArrayList<>());
		following.putIfAbsent(username, new ArrayList<>());
		
		//Scrivo nel file json i dati
		String file = dirDatabase+"Users/"+username+".json";
		updateDatabase.updateDbUser(file, user);
		
		String file1 = dirDatabase+"Follow/";
		updateDatabase.updateDbFollow(followers, file1 + "followers.json");
		updateDatabase.updateDbFollow(following, file1 + "following.json");
		
		return "Registration success";
	}
	    
	public ResponseMessage<String> login(String username, String password){

		String code = null;
		boolean tmp = false;
		List<String> followList;	//invio all'user la lista dei suoi seguaci
		
		if(username.isEmpty() || password.isEmpty())
			code = "ERROR: Username and password cannot be empty";
		else {
			for(Utente u : registeredUsers) {
				if(u.getUsername().equals(username)) {
					try {
						if(u.getPassword().equals(Hash.bytesToHex(Hash.sha256(password)))) {
							tmp = true;
							code = "OK";
						} else code = "ERROR: wrong password";
					}catch(NoSuchAlgorithmException e) {
						code = "ERROR: NoSuchAlgorithmException";
					}
				}
			}
		}
		
		//se l'utente non e' presente nelle Map non posso inviargli la sua lista dei followers
		if(tmp && followers.containsKey(username)) {
			followList = new ArrayList<>(followers.get(username));
		}
		else followList = null;
		 			
			
		if(!tmp && code == null) 
			code = "ERROR: User not found, register first";
	
		return new ResponseMessage<>(code, followList);
	}
	
	public ResponseMessage<Utente> listUsers(String username) {
		if(registeredUsers.isEmpty())
			return new ResponseMessage<>("No Users Registered", null);
		
		List<Utente> UsersTagsInCommon = new ArrayList<>();
		List<String> userTags = new ArrayList<>();
		
		//prendo i tag dell'utente corrente
		for(Utente u : registeredUsers)
			if(u.getUsername().equals(username)) {
				userTags = u.getTags();
			}
		
		//salvo tutti gli utenti con almeno un tag in comune con user
		Iterator<Utente> i = registeredUsers.iterator();
		while(i.hasNext()) {
			Utente u = i.next();
			if(!u.getUsername().equals(username)){
				for(String t : userTags) {
					if(u.getTags().contains(t)) {
						UsersTagsInCommon.add(u);
						break;
					}
				}
			}
		}
		
		if(UsersTagsInCommon.isEmpty())
			return new ResponseMessage<>("Non ci sono utenti con tag in comune", null);
		else return new ResponseMessage<>("OK", UsersTagsInCommon);
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
		
		
		Iterator<Utente> i = registeredUsers.iterator();
		while(i.hasNext()) {
			Utente u = i.next();
			if(u.getUsername().equals(userToFollow)) {
				if(!following.get(currUser).contains(userToFollow)) {	//se l'utente corrente non segue usertofollow
					followers.get(userToFollow).add(currUser);
					following.get(currUser).add(userToFollow);
					
					//aggiorno la struttura nel client
					update(currUser, userToFollow, 1);
					
					//aggiortno le strutture nel database
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
		
		Iterator<Utente> i = registeredUsers.iterator();
		while(i.hasNext()) {
			Utente u = i.next();
			if(u.getUsername().equals(userToUnfollow)) {
				if(following.get(currUser).contains(userToUnfollow)) {
					followers.get(userToUnfollow).remove(currUser);
					following.get(currUser).remove(userToUnfollow);
					
					//aggiorno la struttura dati nel client tramite callback
					update(currUser, userToUnfollow, 0);
					
					//aggiorno le strutture nel database
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
		
		//creo il post e lo aggiungo alla lista di post
		Post post = new Post(IdPostglobal, user, title, contenuto);
		listPosts.putIfAbsent(IdPostglobal, post);
		
		//aggiungo il post al database
		String path = dirDatabase+"Post/"+Integer.toString(IdPostglobal)+".json";
		updateDatabase.updateDbPost(post, path);
		IdPostglobal++;
		return "OK";
	}
	
	public ResponseMessage<Post> showFeed(String user){
		
		if(listPosts.isEmpty())
			return new ResponseMessage<>("Non ci sono post nel social", null);
		
		//per ogni post: se l'user segue l'autore o segue un rewiner lo aggiungo al feed
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
		
		//per ogni post di cui user e' l'autore o un rewiner lo aggiungo al blog
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
		if(!following.get(user).contains(listPosts.get(idPost).getAutore()) && !doUserFollowAnyRewiner(user, idPost))
			return "ERROR: Non segui l'autore di questo post e nessun rewiner del post";
		if(listPosts.get(idPost).getVoters().contains(user))
			return "ERROR: hai gia votato questo post";
		
		//Creo il voto e loaggiungo al post
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String s = formatter.format(timestamp);
		Voto voto = new Voto(vote, s);
		listPosts.get(idPost).addVote(user, voto);
		
		//aggiorno il post nel database
		String path = dirDatabase+"Post/"+Integer.toString(idPost)+".json";
		updateDatabase.updateDbPost(listPosts.get(idPost), path);
		return "OK";
		
	}
	
	public String addComment(String user, int idPost, String comment) {
		if(!listPosts.containsKey(idPost))
			return "ERROR: IdPost non esistente";
		if(listPosts.get(idPost).getAutore().equals(user))
			return "ERROR: Non puoi commentare un post di cui sei autore";
		if(!following.get(user).contains(listPosts.get(idPost).getAutore()) && !doUserFollowAnyRewiner(user, idPost))				
			return "ERROR: Non segui l'autore di questo post e nessun rewiner del post";
		
		//Inserisco il commento nel post
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String s = formatter.format(timestamp);
		listPosts.get(idPost).addComment(user, comment, s);
		
		//aggiorno il post nel database
		String path = dirDatabase+"Post/"+Integer.toString(idPost)+".json";
		updateDatabase.updateDbPost(listPosts.get(idPost), path);
		return "OK";
	}
	
	public ResponseMessage<Post> showPost(String user, int idpost){
		if(!listPosts.containsKey(idpost))
			return new ResponseMessage<>("IdPost non esistente", null);
		
	//	if(!listPosts.get(idpost).getAutore().equals(user) && !listPosts.get(idpost).getRewiners().contains(user))	
		//	return new ResponseMessage<>("ERROR: Non sei ne' autore ne' rewiner del post", null);
		
		if(!following.get(user).contains(listPosts.get(idpost).getAutore()) && !doUserFollowAnyRewiner(user, idpost)
				&& !listPosts.get(idpost).getAutore().equals(user) && !listPosts.get(idpost).getRewiners().contains(user))
			return new ResponseMessage<>("ERROR: Non sei autore ne' rewiner, non segui l'autore del post e nessun rewiner del post", null);
		
		
		//lista formata da un solo post
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
			int cambio = Integer.parseInt(n);	// 1 wincoin = "cambio" BITCOIN
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
		
		//rimuovo il post dal database
		String filename = dirDatabase+"Post/"+idpost+".json";
		updateDatabase.deleteFile(filename);
		return "OK";
	}
	
	public String rewin(String user, int idpost) {
		if(!listPosts.containsKey(idpost))
			return "ERROR: IdPost non esistente";
		if(!following.get(user).contains(listPosts.get(idpost).getAutore()) && !doUserFollowAnyRewiner(user, idpost))	//se l'utente segue l'autore del post da retwittare
			return "ERROR: Non segui l autore del post e nessun rewiner del post";
		
		listPosts.get(idpost).addRewiner(user);
		
		//aggiorno il post nel database
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
		boolean contains = false;
		Iterator<CallbackInfo> i = clients.iterator();
		while(i.hasNext()) {
			CallbackInfo user = (CallbackInfo) i.next();
			if(user.getClient().equals(clientInterface))
				contains = true;
		}
		
		if(!contains) {
			clients.add(new CallbackInfo(clientInterface, username));
			//System.out.println("CALLBACK SYSTEM: New client registered");
		}
		
	}
	
	public synchronized void unregisterForCallback(InterfaceNotifyEvent Client) throws RemoteException{
		CallbackInfo userToUnregister = null;
		Iterator<CallbackInfo> i = clients.iterator();
		while(i.hasNext()) {
			CallbackInfo user = (CallbackInfo) i.next();
			if(user.getClient().equals(Client)) {
				userToUnregister = user;
				break;
			}
		}
		
		if(userToUnregister != null) {
			clients.remove(userToUnregister);
			//System.out.println("CALLBACK SYSTEM: Client unregistered");
		}
		//else System.out.println("Unable to unregister client");
	}
	
	public void update(String currUser, String userToFollow, int op) throws RemoteException{
		CallbackFollowers(currUser, userToFollow, op);
	}
		
	private synchronized void CallbackFollowers(String currUser, String userToFollow, int op) throws RemoteException{
		
		//System.out.println("CALLBACK SYSTEM: starting callbacks");
		for(CallbackInfo info : clients) {
			InterfaceNotifyEvent client = info.getClient();
			if(info.getUsername().equals(userToFollow)) {
				try {
					client.notifyEventListFollowers(currUser, op);
				}catch(RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		
		//System.out.println("CALLBACK SYSTEM: Callback complete");
	}

	//Configurazione delle variabili globali
	public static boolean setupServer(String path) throws IOException {
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
					return false;
				}
				break;
				
			case "UDPPORT":
				try {
					port = Integer.parseInt(splitLine[2]);
					UDPPORT = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. UDP Must be a number");
					return false;
				}
				break;

			case "REGPORT":
				try {
					port = Integer.parseInt(splitLine[2]);
					regPort = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. REGPORT Must be a number");
					return false;
				}
				break;
				
			case "MCAST":
				MCAST = splitLine[2];
				break;

			case "RICOMPENSAAUTORE":
				try {
					port = Integer.parseInt(splitLine[2]);
					ricompensaAutore = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. RICOMPENSAAUTORE Must be a number");
					return false;
				}
				break;
				
			case "PERIODOCALCOLO":
				try {
					port = Integer.parseInt(splitLine[2]);
					periodoCalcolo = port;
				}catch(NumberFormatException e) {
					System.err.println("ERROR IN CONFIGFILE. PERIODOCALCOLO Must be a number");
					return false;
				}
				break;

			case "DIRDATABASE":
				dirDatabase = splitLine[2];
				break;
				
			default: break;
			}
		}
		br.close();
		return true;
	}
	
	public static void getBackupData() {
		File directory = new File(dirDatabase+"Users/");	//directory dove sono memorizzati i file dei profili utente
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
					Utente u = new Utente(username, password, tags);
					u.setTransazioni(transazioni);
					u.setGuadagno(wincoins);
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
					int upvote = 0; int downvote = 0; int idPost = 0; int n_iterazioni = 0;
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
						if(field.equals("n_iterazioni"))
							n_iterazioni = reader.nextInt();
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
					p.setValuesBackup(upvote, downvote, n_iterazioni, rewiners, voti, commenti);
					listPosts.putIfAbsent(idPost, p);
					if(idPost >= IdPostglobal)
						IdPostglobal = idPost+1;
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
				//for(var entry : followers.entrySet())
					//System.out.println("FOLLOWERS " + entry.getKey() +": "+entry.getValue());
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
		int ok = 0;	//ok = 0 -> key non presa, ok = 1 -> key presa
		String name = null;
		String key = null;
		while(reader.hasNext()) {
			JsonToken nextToken = reader.peek();
			if(ok == 0)
				name = reader.nextName();
			if(JsonToken.NAME.equals(nextToken) && ok == 0) {
				key = name;
				ok = 1;
			} 
			else if(ok == 1) {
				voti.put(key, readVoto(reader));
				ok = 0;
			}
			else reader.skipValue();
		}
		return voti;
	}
	
	public static Voto readVoto(JsonReader reader) throws IOException{
		int voto = 0;
		String timestamp = null;
		reader.beginObject();
		while(reader.hasNext()) {
			String field = reader.nextName();
			if(field.equals("voto"))
				voto = reader.nextInt();
			else if(field.equals("timestamp"))
				timestamp = reader.nextString();
			else reader.skipValue();
		}
		reader.endObject();
		Voto value = new Voto(voto, timestamp);
		return value;
	}
	
	public static Map<String, List<Comment>> readCommenti(JsonReader reader) throws IOException{
		Map<String, List<Comment>> commenti = new HashMap<>();
		int ok = 0;
		String name = null;
		String key = null;
		while(reader.hasNext()) {
			JsonToken nextToken = reader.peek();
			if(ok == 0)
				name = reader.nextName();
			if(JsonToken.NAME.equals(nextToken) && ok == 0) {
				key = name;
				ok = 1;
			}else if(ok == 1) {
				commenti.put(key, readComm(reader));
				ok = 0;
			}
			else reader.skipValue();
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
	
	public static void main(String[] args){
		
		if(args.length != 1) {
			System.err.println("Usage: needs path of config file");
			System.exit(1);
		}
		
		MainServer server = new MainServer();
		String path = args[0];
		try {
			if(!setupServer(path)) {
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
		getBackupData();
		
		try {
			InterfaceServerRMI stub = (InterfaceServerRMI) UnicastRemoteObject.exportObject(server, 0);
			
			//creazione di un registry sulla porta regPort
			LocateRegistry.createRegistry(regPort);
			Registry r = LocateRegistry.getRegistry(regPort);
			r.rebind("Server", stub);	//pubblicazione dello stub nel registry
			//System.out.println("Server pronto");
		}catch(RemoteException e) {
			System.err.println("Errore: " + e.getMessage());
		}
	
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new CalcoloRicompense(listPosts, registeredUsers, ricompensaAutore, UDPPORT, MCAST), periodoCalcolo, periodoCalcolo, TimeUnit.SECONDS);
		server.start();
	}
}
