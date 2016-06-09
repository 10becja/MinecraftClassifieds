package me.becja10.MCClassifieds.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RequestManager {

	private static Logger logger;
	private static FileConfiguration config = null;
	private static File Requests = null;
	private static String path;
	
	public static FileConfiguration getRequests() {
		/*
		 * <req UUID>:
		 *   item: 
		 *   amount:
		 *   price:
		 *   requestionPlayer:
		 *   createDate:
		 *   isPending:
		 */
		if (config == null)
			reloadRequests();
		return config;
	}

	public static void reloadRequests() {
		if (Requests == null)
			Requests = new File(path);
		config = YamlConfiguration.loadConfiguration(Requests);
	}
	
	public static void saveRequests() {
		if ((config == null) || (Requests == null))
			return;
		try {
			getRequests().save(Requests);
		} catch (IOException ex) {
			logger.warning("Unable to write to the file \"" + path + "\"");
		}
	}
	
	public static void setUpManager(JavaPlugin plugin, Logger log){
		path = plugin.getDataFolder().getAbsolutePath()	+ File.separator + "Requests.yml".toLowerCase();
		reloadRequests();		
	}
	
	public static void storeRequest(Request req){
		String key = req.id.toString()+".";
		config.set(key+"item", req.item);
		config.set(key+"amount", req.amount);
		config.set(key+"price", req.price);
		config.set(key+"requestingPlayer", req.requestingPlayer.toString());
		config.set(key+"createDate", req.createDate);
		
		config.set(key+"isPending", false);
		saveRequests();
	}
	
	public static Request getRequest(String id){
		Request req = null;
		if(config.contains(id)){
			req = new Request(id);

			req.item = config.getItemStack(id+".item");
			req.amount = config.getInt(id+".amount");
			req.price = config.getInt(id+".price");
			req.requestingPlayer = UUID.fromString(config.getString(id+".requestingPlayer"));
			req.createDate = config.getLong(id+".createDate");
			req.isPending = config.getBoolean(id+".isPending");
		}
		return req;
	}
	
	public static void deleteRequest(Request req){
		if(config.contains(req.id.toString())){
			config.set(req.id.toString(), null);
			saveRequests();
		}
	}
	
	public static void setPending(Request req){
		String id = req.id.toString();
		if(config.contains(id)){
			config.set(id+".item", req.item);//update the item in case it's changed
			config.set(id+".isPending", true);
			saveRequests();
		}
	}
	
	public static Set<String> getIds(){
		return config.getKeys(false);
	}
}
