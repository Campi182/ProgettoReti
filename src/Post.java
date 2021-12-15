import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Post implements Serializable{

	private int IdPost;
	private String autore;
	private String titolo;
	private String contenuto;
	private int upvote;
	private int downvote;
	private Set<String> voters;
	private List<Comment> commenti;
	private Set<String> rewiners;
	
	public Post(int IdPost, String creator, String title, String content) {
		this.IdPost = IdPost;
		this.autore = creator;
		this.titolo = title;
		this.contenuto = content;
		voters = new HashSet<>();
		commenti = new ArrayList<>();
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
	
	public Set<String> getVoters(){
		return this.voters;
	}
	public List<Comment> getComments(){
		return this.commenti;
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
	
	public void addVoters(String user) {
		this.voters.add(user);
	}
	
	public void addRewiner(String user) {
		this.rewiners.add(user);
	}
	
	public void addComment(String autore, String commento) {
		Comment comm = new Comment(autore, commento);
		this.commenti.add(comm);
	}
}
