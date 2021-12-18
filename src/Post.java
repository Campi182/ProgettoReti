import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Post implements Serializable{

	private int IdPost;
	private String autore;
	private String titolo;
	private String contenuto;
	private int upvote;
	private int downvote;
	private Map<String, Voto> voti;
	private Map<String, List<Comment>> commenti;
	private Set<String> rewiners;
	
	public Post(int IdPost, String creator, String title, String content) {
		this.IdPost = IdPost;
		this.autore = creator;
		this.titolo = title;
		this.contenuto = content;
		voti = new HashMap<>();
		commenti = new HashMap<>();
		rewiners = new HashSet<>();
	}
	
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
	
	public int getUpvote() {
		return this.upvote;
	}
	
	public int getDownvote() {
		return this.downvote;
	}
	
	public Set<String> getRewiners(){
		return this.rewiners;
	}
	
	public void setUpvote(int vote) {
		this.upvote += vote;
	}
	
	public void setDownvote(int vote) {
		this.downvote += vote;
	}
	
	public void addVote(String user, Voto voto) {
		voti.put(user, voto);
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
	
	public void addRewiner(String user) {
		this.rewiners.add(user);
	}
	
	public void addComment(String autore, String commento, Timestamp timestamp) {
		Comment comm = new Comment(autore, commento, timestamp);
		if(!commenti.containsKey(autore))
			commenti.put(autore, new ArrayList<>());
		commenti.get(autore).add(comm);
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
}
