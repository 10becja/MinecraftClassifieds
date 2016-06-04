package me.becja10.MCClassifieds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import me.becja10.MCClassifieds.Commands.RequestCommandHandler;
import me.becja10.MCClassifieds.Events.ChatEventHandler;
import me.becja10.MCClassifieds.Events.PlayerEventHandler;
import me.becja10.MCClassifieds.Utils.Messages;
import me.becja10.MCClassifieds.Utils.Request;
import me.becja10.MCClassifieds.Utils.RequestManager;
import me.becja10.MCClassifieds.Utils.WizardPlayer;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.api.IItemDb;

public class MCClassifieds extends JavaPlugin implements Listener{

	public static MCClassifieds instance;
	public final static Logger logger = Logger.getLogger("Minecraft");
	
	public static List<Request> activeRequests;
	public static HashMap<UUID, List<Request>> playerMap;
	public static HashMap<UUID, WizardPlayer> wizardPlayers;
	public static IItemDb itemDb;
	public static Economy econ;
	
	public HashMap<String, PotionEffect> possibleEffects;

	private String configPath;
	private FileConfiguration config;
	private FileConfiguration outConfig;
	
	private IEssentials ess;
	
	//Config Settings
	
	public static int requestLimit; private String requestLimitstr = "Request limit per player";
	public static int timeLimit; private String timeLimitstr = "Number of days before request expires";
	
		
	private void loadConfig(){
		configPath = this.getDataFolder().getAbsolutePath() + File.separator + "config.yml";
		config = YamlConfiguration.loadConfiguration(new File(configPath));
		outConfig = new YamlConfiguration();
		
		requestLimit = config.getInt(requestLimitstr, 5);
		timeLimit = config.getInt(timeLimitstr, 3);
		
		
		outConfig.set(requestLimitstr, requestLimit);
		outConfig.set(timeLimitstr, timeLimit);
		
		saveConfig(outConfig, configPath);				
	}
	
	private void saveConfig(FileConfiguration config, String path)
	{
        try{config.save(path);}
        catch(IOException exception){logger.info("Unable to write to the configuration file at \"" + path + "\"");}
	}
	
	@Override
	public void onEnable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		PluginManager manager = getServer().getPluginManager();

		logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " has been enabled!");
		instance = this;
		
		activeRequests = new ArrayList<Request>();
		wizardPlayers = new HashMap<UUID, WizardPlayer>();
		playerMap = new HashMap<UUID, List<Request>>();
		
		loadConfig();		
		
		if (!setupEconomy() ) {
			logger.severe(pdfFile.getName() + " - Disabled due to no Vault dependency found!");
            manager.disablePlugin(this);
            return;
        }
		
		ess = (IEssentials)manager.getPlugin("Essentials");
		
		if(!ess.isEnabled()){
			logger.warning("Could not load Essentials. Disabling plugin.");
			manager.disablePlugin(this);
			return;
		}
		else{
			logger.info("Essentials hooked!");
		}
		
		itemDb = ess.getItemDb();
		
		RequestManager.setUpManager(this, logger);
		
		for(String id : RequestManager.getIds()){
			Request req = RequestManager.getRequest(id);
			if(req != null && !req.isPending){
				activeRequests.add(req);
			}
			addToPlayerMap(req);			
		}
		
		Collections.sort(activeRequests);
				
		manager.registerEvents(new ChatEventHandler(), this);
		manager.registerEvents(new PlayerEventHandler(), this);;
	}
		
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info(pdfFile.getName() + " Has Been Disabled!");
		saveConfig(outConfig, configPath);
		RequestManager.saveRequests();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		switch(cmd.getName().toLowerCase()){ 
			case "mccreload":
				if(sender instanceof Player && !sender.hasPermission(""))
					sender.sendMessage(Messages.noPermission());
				else{
					loadConfig();
					sender.sendMessage(Messages.reloadSuccessful());
				}
				return true;
				
			case "mccrequest":
				return RequestCommandHandler.makeRequest(sender);
			case "mccfulfill":
				return RequestCommandHandler.fulfillRequest(sender, args);
			case "mcclist":
				for(Request req : activeRequests){
					if(!req.isPending && (System.currentTimeMillis() - req.createDate) > timeLimit*86400000){
						int idx = playerMap.get(req.requestingPlayer).indexOf(req);
						logger.info("[MCCLassifieds] " + Bukkit.getOfflinePlayer(req.requestingPlayer).getName() + "'s "
								+ "request expired and was automatically cancelled.");
						cancelRequest(Bukkit.getConsoleSender(), req.requestingPlayer, idx);				
					}
				}
				return RequestCommandHandler.viewRequests(sender, args);
			case "mcccancel":
				return RequestCommandHandler.cancelRequest(sender, args);
			case "mccget":
				return RequestCommandHandler.getRequest(sender, args);
		}
		return true;
	}
	
	public static boolean playerAtRequestLimit(UUID id){
		List<Request> requests = playerMap.get(id);
		if(requests != null){
			return requests.size() > requestLimit;
		}		
		return false;
	}
	
	public static void newRequest(Request req){
		activeRequests.add(req);
		addToPlayerMap(req);
		RequestManager.storeRequest(req);
	}
	
	public static void fulfillRequest(Request req){
		activeRequests.remove(req);
		RequestManager.setPending(req);

		List<Request> requests = playerMap.get(req.requestingPlayer);
		if(requests != null){
			int idx = requests.indexOf(req);
			System.out.println(idx);
			if(idx > -1){
				System.out.println(req.isPending);
				req.isPending = true;
				requests.set(idx, req);
				playerMap.put(req.requestingPlayer, requests);
			}
		}
	}
	
	public static void cancelRequest(CommandSender sender, UUID pid, int idx){
		
		List<Request> list = playerMap.get(pid);
		OfflinePlayer player = Bukkit.getOfflinePlayer(pid);
		if(list == null){
			sender.sendMessage(Messages.playerHasNoRequests(player.getName()));
			return;
		}
		
		if(idx >= list.size())
		{
			sender.sendMessage(Messages.invalidId());
			return;
		}
		Request req = list.get(idx);
		if(req.isPending){
			sender.sendMessage(ChatColor.RED + "Can not remove requests pending collection.");
			if(sender instanceof Player && ((Player) sender).getUniqueId() == pid)
				sender.sendMessage(ChatColor.RED + "use /mccget " + (idx+1) + " to collect your items.");
			return;
		}
		
		EconomyResponse r = econ.depositPlayer(player, req.price);
		if(r.transactionSuccess())
		{
			sender.sendMessage(ChatColor.GOLD + "Added $" + req.price + " to " + player.getName() + "'s acount.");
			activeRequests.remove(req);
			list.remove(idx);
			RequestManager.deleteRequest(req);
		}
		else{
			sender.sendMessage(ChatColor.RED + "Could not remove request due to transaction failure");
		}		
	}
	
	public static void getRequest(Player player, int id){
		List<Request> list = playerMap.get(player.getUniqueId());
		if(list == null){
			player.sendMessage(ChatColor.RED + "You have no pending requests!");
			return;
		}
		if(id >= list.size())
		{
			player.sendMessage(Messages.invalidId());
			return;
		}
		
		Request req = list.get(id);
		if(!req.isPending){
			player.sendMessage(ChatColor.RED + "This request has not been fulfilled yet. Use "+ ChatColor.WHITE + "'/mcclist mine'" 
							 + ChatColor.RED +" to view your requests "
							 + "and select one with a " + ChatColor.GREEN + "green " + ChatColor.RED + "id.");
			return;
		}
		Inventory inv = player.getInventory();
		int emptyCount = 0;
		for(ItemStack item : inv.getStorageContents())
		{
			if(item == null)
				emptyCount++;
		}
		double spotsNeeded = Math.ceil((double)req.amount / req.item.getMaxStackSize());
		if(spotsNeeded > emptyCount){
			player.sendMessage(ChatColor.RED + "You do not have enough open spaces in your inventory to collect your items."
					+ " Please empty some space and run the command again.");
			return;
		}
		int remaining = req.amount;
		while(remaining > 0){
			ItemStack temp = new ItemStack(req.item);
			temp.setAmount(Math.min(temp.getMaxStackSize(), remaining));
			int emptySpot = inv.firstEmpty();
			inv.setItem(emptySpot, temp);
			remaining = remaining - temp.getAmount();
		}
		
		player.updateInventory();
		list.remove(id);
		RequestManager.deleteRequest(req);
		player.sendMessage(ChatColor.GREEN + "Your items have been placed in your inventory!");
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	private static void addToPlayerMap(Request req) {
		List<Request> requests = playerMap.get(req.requestingPlayer);
		if(requests == null)
			requests = new ArrayList<Request>();
		requests.add(req);
		playerMap.put(req.requestingPlayer, requests);
	}
}

