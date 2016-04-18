package me.becja10.MCClassifieds.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.becja10.MCClassifieds.MCClassifieds;
import me.becja10.MCClassifieds.Utils.Messages;
import me.becja10.MCClassifieds.Utils.WizardPlayer;

public class RequestCommandHandler {

	public static boolean makeRequest(CommandSender sender) {
		if(!(sender instanceof Player))
		{
			sender.sendMessage(Messages.playersOnly());
			return true;
		}
		
		if(!sender.hasPermission("mcc.makerequest"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		Player player = (Player) sender;
		
		WizardPlayer wp = new WizardPlayer(player.getUniqueId());
		
		if(MCClassifieds.wizardPlayers.containsKey(wp.id))
		{
			return true;
		}
		
		MCClassifieds.wizardPlayers.put(wp.id, wp);
		
		sender.sendMessage(ChatColor.GREEN + "Welcome to the Minecraft Classifieds tutorial! Here you can make requests for things you want.");
		sender.sendMessage(Messages.breakLine());
		sender.sendMessage(Messages.requestItem());
		
		return true;
	}

	public static boolean fulfilRequest(CommandSender sender, String[] args) {
		
		return true;
	}
	
	public static boolean viewRequests(CommandSender sender, String[] args){
		
		return true;
	}
	
	public static boolean cancelRequest(CommandSender sender, String[] args){
		
		return true;
	}

}
