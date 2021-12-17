import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.Math;

public class MainCalcoloRicompense implements Runnable{
	
	private Map<Integer, Post> listPosts;
	private List<Utente> registeredUsers;
	private int iterations;
	private Timestamp lastIteraction;	//quando ho finito l'ultima iterazione
	private Timestamp currTime;
	
	public MainCalcoloRicompense(Map<Integer, Post> listPosts, List<Utente> registeredusers) {
		this.listPosts = listPosts;
		this.registeredUsers = registeredusers;
		iterations = -1;
		lastIteraction = new Timestamp(System.currentTimeMillis());	
	}
	
	public void run() {
		iterations++;
		System.out.println("ITERAZIONE " + iterations);
		int sommaVoti = 0, commentiUtente = 0;
		double guadagno = 0, sommatoriaCommenti = 0;
		
		System.out.println("len posts " + listPosts.size());
		for(var entry : listPosts.entrySet()) {	//per ogni post faccio il calcolo
			System.out.println("last iteraction: " + lastIteraction);
			sommaVoti = 0; sommatoriaCommenti = 0; guadagno = 0; commentiUtente = 0;
			Set<Voto> voti = entry.getValue().getVoti();
			for(Voto v : voti) {
				if(v.getTimestamp().after(lastIteraction)) {	//se il voto e dopo l'ultima iterazione allora e recente e va contato
					sommaVoti += v.getVoto();
				}
			}
			
			Map<String, List<Comment>> mapComments = entry.getValue().getMapComments();
			for(var entry2 : mapComments.entrySet()) {
				commentiUtente = 0;
				for(Comment c : entry2.getValue()) {
					if(c.getTimestamp().after(lastIteraction))
						commentiUtente++;
				}
				sommatoriaCommenti += (2 / (1+ Math.pow(Math.E,-(commentiUtente-1))));
			}
			
			guadagno = Math.round(  ((Math.log(Math.max(sommaVoti, 0)+1) + Math.log(sommatoriaCommenti+1)  ) / iterations)*100.0) / 100.0;
			System.out.println("GUADAGNO post " + entry.getKey() + ": " + guadagno);
			for(Utente u : registeredUsers)
				if(u.getUsername().equals(entry.getValue().getAutore()) && guadagno != 0) {
					currTime = new Timestamp(System.currentTimeMillis());
					u.setWincoins(guadagno, currTime);
				}
		}
		

		lastIteraction = new Timestamp(System.currentTimeMillis());	//quando e' terminata l'ultima iterazione
		System.out.println("TERMINATA ITERAZIONE");
	}

}
