import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.lang.Math;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MainCalcoloRicompense implements Runnable{
	
	private Map<Integer, Post> listPosts;
	private List<Utente> registeredUsers;
	private int ricompensaAutore;
	private int iterations;
	private Timestamp lastIteraction;	//quando ho finito l'ultima iterazione
	private Timestamp currTime;
	InetAddress multicastGroup;
	DatagramSocket ms;
	
	public MainCalcoloRicompense(Map<Integer, Post> listPosts, List<Utente> registeredusers, int ricompensaAutore) {
		this.listPosts = listPosts;
		this.registeredUsers = registeredusers;
		this.ricompensaAutore = ricompensaAutore;
		iterations = -1;
		lastIteraction = new Timestamp(System.currentTimeMillis());
		try {
			this.multicastGroup = InetAddress.getByName("226.226.226.226");
			this.ms = new DatagramSocket();
		}catch(Exception e) {
			System.out.println(e);
		}
		
	}
	
	public void run() {
		iterations++;
		int sommaVoti = 0, commentiUtente = 0;
		double guadagno = 0, sommatoriaCommenti = 0;
		Set<String> curatori;
		for(var entry : listPosts.entrySet()) {	//per ogni post faccio il calcolo
			System.out.println("last iteraction: " + lastIteraction);
			sommaVoti = 0; sommatoriaCommenti = 0; guadagno = 0; commentiUtente = 0; curatori = new HashSet<>();
			
			Map<String, Voto> mapVoti = entry.getValue().getMapVoti();
			for(var entry3 : mapVoti.entrySet()) {
				if(entry3.getValue().getTimestamp().after(lastIteraction)) {
					sommaVoti += entry3.getValue().getVoto();
					if(entry3.getValue().getVoto() == 1)
						curatori.add(entry3.getKey());
				}
			}
						
			Map<String, List<Comment>> mapComments = entry.getValue().getMapComments();
			for(var entry2 : mapComments.entrySet()) {
				commentiUtente = 0;
				for(Comment c : entry2.getValue()) {
					if(c.getTimestamp().after(lastIteraction)) {
						commentiUtente++;
						curatori.add(c.getAutore());
					}
				}
				sommatoriaCommenti += (2 / (1+ Math.pow(Math.E,-(commentiUtente-1))));
			}
			
			guadagno = Math.round(  ((Math.log(Math.max(sommaVoti, 0)+1) + Math.log(sommatoriaCommenti+1)  ) / iterations)*100.0) / 100.0;
			double guadagnoAutore = (guadagno*ricompensaAutore)/100;
			double guadagnoCuratore = ((guadagno*(100-ricompensaAutore))/100)/curatori.size();	//guadagno di ogni curatore
			System.out.println("GUADAGNO post " + entry.getKey() + ": " + guadagno);
			System.out.println("GUADAGNO di ogni curatore: "+ guadagnoCuratore);
			System.out.println("GUADAGNO autore: " +guadagnoAutore);
			//ho calcolato il guadagno totale del post
			for(Utente u : registeredUsers) {
				if(u.getUsername().equals(entry.getValue().getAutore()) && guadagno != 0) {	//ricompensa autore
					currTime = new Timestamp(System.currentTimeMillis());
					u.setWincoins(guadagnoAutore, currTime);
				}
				
				if(curatori.contains(u.getUsername())) {
					currTime = new Timestamp(System.currentTimeMillis());
					u.setWincoins(guadagnoCuratore, currTime);
				}
			}
		}
		

		lastIteraction = new Timestamp(System.currentTimeMillis());	//quando e' terminata l'ultima iterazione
		//Multicast communication of portafoglio update
		String message = "Portafogli aggiornati";
		byte[] buffer = message.getBytes();
		DatagramPacket dp = new DatagramPacket(buffer, buffer.length, multicastGroup, 1025);
		System.out.println("Inviando messaggio aggiornamento");
		try {
			ms.send(dp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
