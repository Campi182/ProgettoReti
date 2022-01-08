import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class updateDatabase {

	private static GsonBuilder builder = new GsonBuilder();
	private static Gson gson = builder.create();
	
	public static void updateDbUser(String filename, Utente user) {
		//builder.setDateFormat("yyyy/MM/dd HH:mm:ss");
		try(FileOutputStream os = new FileOutputStream(filename);
			FileChannel oc = os.getChannel();
				){
			ByteBuffer buf = ByteBuffer.allocate(8192);
			byte[] data = gson.toJson(user).getBytes();
			for(int l = 0; l < data.length; l+=8192) {
				buf.clear();
				buf.put(data, l, Math.min(8192, data.length-l));
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
	}
	
	public static void updateDbPost(Post post, String filename) {
		//builder.setDateFormat("yyyy/MM/dd HH:mm:ss");
		try(FileOutputStream os = new FileOutputStream(filename);
			FileChannel oc = os.getChannel();
				){
			ByteBuffer buf = ByteBuffer.allocate(8192);
			byte[] data = gson.toJson(post).getBytes();
			for(int l = 0; l < data.length; l+=8192) {
				buf.clear();
				buf.put(data, l, Math.min(8192, data.length-l));
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
	}
	
	public static void updateDbFollow(Map<String, List<String>> follow, String filename) {
		try(FileOutputStream os = new FileOutputStream(filename);
				FileChannel oc = os.getChannel();
					){
				ByteBuffer buf = ByteBuffer.allocate(8192);
				byte[] data = gson.toJson(follow).getBytes();
				for(int l = 0; l < data.length; l+=8192) {
					buf.clear();
					buf.put(data, l, Math.min(8192, data.length-l));
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
	}

	public static void deleteFile(String filename) {
		File f = new File(filename);
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir);
		if(f.delete())
			System.out.println("deleted");
		else System.err.println("failed");
	}

}
