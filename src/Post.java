import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Post implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int IdPost;
	private String autore;
	private String titolo;
	private String contenuto;
	private int upvote;
	private int downvote;
	private int n_iterazioni;
	private Map<String, Voto> voti;
	private Map<String, List<Comment>> commenti;
	private Set<String> rewiners;
	
	public Post(int IdPost, String creator, String title, String content) {
		this.IdPost = IdPost;
		this.autore = creator;
		this.titolo = title;
		this.contenuto = content;
		n_iterazioni = 1;
		voti = new ConcurrentHashMap<>();
		commenti = new ConcurrentHashMap<>();
		rewiners = new HashSet<String>();
	}
	
	//GETTERS
	
	public int getId() {
		return this.IdPost;
	}
	
	public String getAutore() {
		return this.autore;
	}
	
	public String getTitolo() {
		return this.titolo;
	}
	
	public String getContenuto() {
		return this.contenuto;
	}
	
	public int getIterazioni() {
		return this.n_iterazioni;
	}
	
	public int getUpvote() {
		return this.upvote;
	}
	
	public int getDownvote() {
		return this.downvote;
	}
	
	public Set<String> getRewiners(){
		return this.rewiners;
	}
	
	public Set<String> getVoters(){
		return voti.keySet();
	}
	
	public Set<Voto> getVoti(){
		Set<Voto> res = new HashSet<>();
		for(var entry : voti.entrySet())
			res.add(entry.getValue());
		return res;
	}
	
	public Map<String, List<Comment>> getMapComments(){
		return this.commenti;
	}
	
	public Map<String, Voto> getMapVoti(){
		return this.voti;
	}
	
	public List<Comment> getComments(){
		List<Comment> res = new ArrayList<>();
		for(var entry : commenti.entrySet())
			for(Comment c : entry.getValue())
				res.add(c);
		return res;
	}
	
	//SETTERS
	
	public void setValuesBackup(int upvote, int downvote, int n_iterazioni, Set<String> rewiners, Map<String, Voto> voti, Map<String, List<Comment>> commenti) {
		this.upvote = upvote;
		this.downvote = downvote;
		this.n_iterazioni = n_iterazioni;
		this.rewiners = rewiners;
		this.voti = voti;
		this.commenti = commenti;
	}
	
	
	public void addVote(String user, Voto voto) {
		voti.putIfAbsent(user, voto);
		if(voto.getVoto() == 1)
			this.upvote++;
		else this.downvote++;
	}
	
	public void addRewiner(String user) {
		this.rewiners.add(user);
	}
	
	public void addComment(String autore, String commento, String timestamp) {
		Comment comm = new Comment(autore, commento, timestamp);
		if(!commenti.containsKey(autore))
			commenti.putIfAbsent(autore, new ArrayList<>());
		commenti.get(autore).add(comm);
	}
	
	public void plusIteraction() {
		this.n_iterazioni++;
	}
	
}
