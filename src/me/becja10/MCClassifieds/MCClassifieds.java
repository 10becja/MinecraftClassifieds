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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
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
	
	private List<String> defaultBlacklist;
	
	//Config Settings
	
	public static int requestLimit; private String _requestLimit = "Request limit per player";
	public static int timeLimit; private String _timeLimit = "Number of days before request expires";
	public static List<String> blacklistItems; private String _blacklistItems = "Items Blacklist";
	
		
	private void loadConfig(){
		configPath = this.getDataFolder().getAbsolutePath() + File.separator + "config.yml";
		config = YamlConfiguration.loadConfiguration(new File(configPath));
		outConfig = new YamlConfiguration();
		
		requestLimit = config.getInt(_requestLimit, 5);
		timeLimit = config.getInt(_timeLimit, 3);
		
		blacklistItems = config.contains(_blacklistItems) ? config.getStringList(_blacklistItems) : defaultBlacklist;
				
		outConfig.set(_requestLimit, requestLimit);
		outConfig.set(_timeLimit, timeLimit);
		outConfig.set(_blacklistItems, blacklistItems);
		
		saveConfig(outConfig, configPath);				
	}
	
	private void saveConfig(FileConfiguration config, String path)
	{
        try{config.save(path);}
        catch(IOException exception){logger.warning("Unable to write to the configuration file at \"" + path + "\"");}
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
		
		defaultBlacklist = new ArrayList<String>();
		defaultBlacklist.add(Material.BEDROCK.toString());
		defaultBlacklist.add(Material.SOIL.toString());
		defaultBlacklist.add(Material.DOUBLE_PLANT.toString());
		defaultBlacklist.add(Material.AIR.toString());
		defaultBlacklist.add(Material.BARRIER.toString());
		defaultBlacklist.add(Material.ENDER_PORTAL_FRAME.toString());
		defaultBlacklist.add(Material.MONSTER_EGG.toString());
		defaultBlacklist.add(Material.MONSTER_EGGS.toString());
		defaultBlacklist.add(Material.LAVA.toString());
		defaultBlacklist.add(Material.WATER.toString());
		defaultBlacklist.add(Material.COMMAND.toString());
		defaultBlacklist.add(Material.COMMAND_CHAIN.toString());
		defaultBlacklist.add(Material.COMMAND_REPEATING.toString());
		defaultBlacklist.add(Material.COMMAND_MINECART.toString());
		
		loadConfig();		
		
		if (!setupEconomy() ) {
			logger.severe(pdfFile.getName() + " - Disabled due to no Vault dependency found!");
            manager.disablePlugin(this);
            return;
        }
		
		ess = (IEssentials)manager.getPlugin("Essentials");
		
		if(!ess.isEnabled()){
			logger.severe("Could not load Essentials. Disabling plugin.");
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
		manager.registerEvents(new PlayerEventHandler(), this);
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
			case "mccadmin":
				if(sender instanceof Player && !sender.hasPermission("mcc.admin"))
					sender.sendMessage(Messages.noPermission());
				else{
					loadConfig();
					sender.sendMessage(Messages.reloadSuccessful());
				}
				return true;
			case "mcc":
				return displayCommands(sender);
			case "mccrequest":
				return RequestCommandHandler.makeRequest(sender);
			case "mccfulfill":
				return RequestCommandHandler.fulfillRequest(sender, args);
			case "mcclist":
				systemCancelExpiredRequests();
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
			return requests.size() >= requestLimit;
		}		
		return false;
	}
	
	public static String getEnchantmentCommonName(Enchantment enc){
		switch(enc.getName()){
		case "ARROW_DAMAGE":
			return "Power";
		case "ARROW_FIRE":
			return "Flame";
		case "ARROW_INFINITE":
			return "Infinity";
		case "ARROW_KNOCKBACK":
			return "Punch";
		case "DAMAGE_ALL":
			return "Sharpness";
		case "DAMAGE_ARTHROPODS":
			return "BaneOfArthropods ";
		case "DAMAGE_UNDEAD":
			return "Smite";
		case "DEPTH_STRIDER":
			return "DepthStrider";
		case "DIG_SPEED":
			return "Effeciency";
		case "DURABILITY":
			return "Unbreaking";
		case "FIRE_ASPECT":
			return "FireAspect";
		case "FROST_WALKER":
			return "FrostWalker";
		case "KNOCKBACK":
			return "Knockback";
		case "LOOT_BONUS_BLOCKS":
			return "Fortune";
		case "LOOT_BONUS_MOBS":
			return "Looting";
		case "LUCK":
			return "LuckOfTheSea";
		case "LURE":
			return "Lure";
		case "MENDING":
			return "Mending";
		case "OXYGEN":
			return "Respiration";
		case "PROTECTION_ENVIRONMENTAL":
			return "Protection";
		case "PROTECTION_EXPLOSIONS":
			return "BlastProtection";
		case "PROTECTION_FALL":
			return "FeatherFalling";
		case "PROTECTION_FIRE":
			return "FireProtection";
		case "PROTECTION_PROJECTILE":
			return "ProjectileProtection";
		case "SILK_TOUCH":
			return "SilkTouch";
		case "THORNS":
			return "Thorns";
		case "WATER_WORKER":
			return "AquaAffinity";
		}
		return enc.getName();
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
			if(idx > -1){
				req.isPending = true;
				requests.set(idx, req);
				playerMap.put(req.requestingPlayer, requests);
			}
		}
	}
		
	public static void cancelRequest(Request req){
		int idx = playerMap.get(req.requestingPlayer).indexOf(req);
		logger.info("[MCCLassifieds] " + Bukkit.getOfflinePlayer(req.requestingPlayer).getName() + "'s "
				+ "request expired and was automatically cancelled.");
		cancelRequest(Bukkit.getConsoleSender(), req.requestingPlayer, idx);
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
	
	public static String getItemName(ItemStack item){
		String itemName = MCClassifieds.itemDb.name(item);
		if(itemName == null)
			itemName = item.getType().name().toLowerCase(); 
		
		return itemName;
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
	
	private void systemCancelExpiredRequests() {
		List<Request> toCancel = new ArrayList<Request>();
		for(Request req : activeRequests){
			if(!req.isPending && (System.currentTimeMillis() - req.createDate) > timeLimit*86400000){
				toCancel.add(req);
			}
		}
		for(Request req : toCancel){
			cancelRequest(req);				
		}
	}
	
	private boolean displayCommands(CommandSender sender){
		String mccadmin = ChatColor.GOLD + "/mccadmin" + ChatColor.WHITE + ": ";
		String mccrequest = ChatColor.GOLD + "/mccrequest" + ChatColor.WHITE + ": ";
		String mcclist = ChatColor.GOLD + "/mcclist (page)" + ChatColor.WHITE + ": ";
		String mccfulfill = ChatColor.GOLD + "/mccfulfill <id>" + ChatColor.WHITE + ": ";
		String mccget = ChatColor.GOLD + "/mccget <id>" + ChatColor.WHITE + ": ";
		String mcccancel = ChatColor.GOLD + "/mcccancel <id>" + ChatColor.WHITE + ": ";
		
		String msg = "";
		
		if(sender.hasPermission("mcc.admin"))
			sender.sendMessage(mccadmin + "Reloads the config file.");
		
		if(sender.hasPermission("mcc.request"))
			sender.sendMessage(mccrequest + "Initiates a wizard to help create requests.");
		
		if(sender.hasPermission("mcc.list"))
		{
			msg = mcclist + "Displays a list of all active requests, with an optional page." 
						  + " You can use " + ChatColor.GOLD + "/mcclist mine (page)" + ChatColor.WHITE
						  + "To see all of your own requests.";
			if(sender.hasPermission("mcc.list.other")){
				msg += " You can also use " + ChatColor.GOLD + "/mcclist playerName " + ChatColor.WHITE
					+  "to view another players requests.";
			}
			sender.sendMessage(msg);
		}
		
		if(sender.hasPermission("mcc.fulfill"))
			sender.sendMessage(mccfulfill + "Fulfill a request with the given id, found from doing " + ChatColor.GOLD + "/mcclist");
		
		if(sender.hasPermission("mcc.get"))
			sender.sendMessage(mccget + "Get a completed request. Use " + ChatColor.GOLD + "/mcclist mine" + ChatColor.RESET 
					+ " to get the proper id");
		
		if(sender.hasPermission("mcc.cancel")){
			msg = mcccancel + "Cancels a request. Use " + ChatColor.GOLD + "/mcclist mine " + ChatColor.RESET + "to get the proper id.";
			if(sender.hasPermission("mcc.cancel.other")){
				msg += " You can also use " + ChatColor.GOLD + "/mcccancel (playername) <id>" + ChatColor.RESET 
						+ " to cancel another players request";
			}
			sender.sendMessage(msg);
		}		
		
		return true;
	}
}

