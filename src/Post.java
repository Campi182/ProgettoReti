import java.io.Serializable;
import java.util.List;

public class Post implements Serializable{

	private int IdPost;
	private String autore;
	private String titolo;
	private String contenuto;
	private int upvote;
	private int downvote;
	private List<String> commenti;
	
	public Post(int IdPost, String creator, String title, String content) {
		this.IdPost = IdPost;
		this.autore = creator;
		this.titolo = title;
		this.contenuto = content;
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
}
