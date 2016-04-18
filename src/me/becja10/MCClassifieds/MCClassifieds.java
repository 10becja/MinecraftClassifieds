package me.becja10.MCClassifieds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import me.becja10.MCClassifieds.Commands.RequestCommandHandler;
import me.becja10.MCClassifieds.Events.ChatEventHandler;
import me.becja10.MCClassifieds.Utils.Messages;
import me.becja10.MCClassifieds.Utils.Request;
import me.becja10.MCClassifieds.Utils.WizardPlayer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.IItemDb;

public class MCClassifieds extends JavaPlugin implements Listener{

	public static MCClassifieds instance;
	public final static Logger logger = Logger.getLogger("Minecraft");
	
	public static int nextId;
	public static List<Request> requests;
	public static HashMap<UUID, WizardPlayer> wizardPlayers;
	public static IItemDb itemDb;

	
	private String configPath;
	private FileConfiguration config;
	private FileConfiguration outConfig;
	
	//Config Settings
	
	public static int requestLimit; private String requestLimitstr = "Request limit per player";

	
		
	private void loadConfig(){
		configPath = this.getDataFolder().getAbsolutePath() + File.separator + "config.yml";
		config = YamlConfiguration.loadConfiguration(new File(configPath));
		outConfig = new YamlConfiguration();
		
		requestLimit = config.getInt(requestLimitstr, 5);
		
		
		outConfig.set(requestLimitstr, requestLimit);
				
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
		
		nextId = 0;
		requests = new ArrayList<Request>();
		wizardPlayers = new HashMap<UUID, WizardPlayer>();
		
		itemDb = new Essentials().getItemDb();
		
		manager.registerEvents(new ChatEventHandler(), this);
		
		loadConfig();		
	}
		
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info(pdfFile.getName() + " Has Been Disabled!");
		saveConfig(outConfig, configPath);

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
				
			case "mcrequest":
				return RequestCommandHandler.makeRequest(sender);
			case "mcfulfil":
				return RequestCommandHandler.fulfilRequest(sender, args);
		}
		return true;
	}

	public static int getNextId() {
		nextId++;
		return nextId;
	}	
}

