package me.becja10.MCClassifieds.Commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import me.becja10.MCClassifieds.MCClassifieds;
import me.becja10.MCClassifieds.Utils.Messages;
import me.becja10.MCClassifieds.Utils.Request;
import me.becja10.MCClassifieds.Utils.WizardPlayer;

public class RequestCommandHandler {

	public static boolean makeRequest(CommandSender sender) {
		if(!(sender instanceof Player))
		{
			sender.sendMessage(Messages.playersOnly());
			return true;
		}
		
		if(!sender.hasPermission("mcc.request"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		Player player = (Player) sender;
		
		if(MCClassifieds.wizardPlayers.containsKey(player.getUniqueId()))
		{
			return true;
		}
		
		if(MCClassifieds.playerAtRequestLimit(player.getUniqueId())){
			sender.sendMessage(Messages.prefix + ChatColor.RED + "You can not make any more requests until your other requests have been completed.");
			return true;
		}
		
		WizardPlayer wp = new WizardPlayer(player.getUniqueId());		
		
		MCClassifieds.wizardPlayers.put(wp.id, wp);
		
		sender.sendMessage(wp.getPromptForStep());
		sender.sendMessage(Messages.breakLine());
		
		return true;
	}

	public static boolean fulfillRequest(CommandSender sender, String[] args) {
		if(!(sender instanceof Player))
		{
			sender.sendMessage(Messages.playersOnly());
			return true;
		}
		
		if(!sender.hasPermission("mcc.fulfill"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		if(args.length != 1)
			return false;
		
		int id;
		try{
			id = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException ex){
			return false;
		}
		
		if(id > MCClassifieds.activeRequests.size() || id < 1)
		{
			sender.sendMessage(ChatColor.RED + "Invalid Request Id.");
			return true;
		}
		
		Player player = (Player) sender;
		Request req = MCClassifieds.activeRequests.get(id - 1);
		ItemStack original = new ItemStack(req.item); //make a copy in case transaction fails and the item was updated
		
		if(doesInvContainRequest(player.getInventory(), req)){		
				//at this point, we know that they have the proper item(s), so do the transaction
				EconomyResponse r = MCClassifieds.econ.depositPlayer(player, req.price);
				if(r.transactionSuccess())
				{
					ItemStack toRemove = new ItemStack(req.item);
					toRemove.setAmount(req.amount);
					player.getInventory().removeItem(toRemove);
					player.updateInventory();
					player.sendMessage(ChatColor.GREEN + "You have been paid " + ChatColor.GOLD + "$" + req.price);
					MCClassifieds.logger.info(player.getName() + " was credited with " + req.amount + " for selling " + 
																	req.amount + " " + req.item + " to " +
																	Bukkit.getOfflinePlayer(req.requestingPlayer).getName());
					MCClassifieds.fulfillRequest(req);
				}
				else{
					req.item = original;
					sender.sendMessage(Messages.incompleteTransaction());
				}
		}
		else{
			sender.sendMessage(ChatColor.RED + "You do not have the required item(s) for this request.");
		}	
		
		return true;
	}
	
	public static boolean viewRequests(CommandSender sender, String[] args){
		if(!sender.hasPermission("mcc.list"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		if(args.length > 2)
			return false;
		
		int pageParam = (args.length == 2) ? 1 : 0; 
		
		int page = 1;
		boolean pageSent = false;
		try{
			if(args.length > 0){
				page = Integer.parseInt(args[pageParam]);
				pageSent = true;
			}
		}catch(NumberFormatException e){
			pageSent = false;
		}
		
		if(page <= 0) page = 1;
		
		List<Request> listToUse = MCClassifieds.activeRequests;
		
		boolean forPerson = false;
		
		//if they sent a parameter, and the first param isn't the page, then it was a name
		if(!pageSent && args.length > 0){
			String name = args[0];
			Player player = null;
			if(name.equalsIgnoreCase("mine") && sender instanceof Player){
				player = (Player) sender;
			}
			else{
				if(!sender.hasPermission("mcc.list.other"))
				{
					sender.sendMessage(Messages.noPermission());
					return true;
				}
				player = Bukkit.getPlayer(name);
			}
			if(player != null){
				listToUse = MCClassifieds.playerMap.get(player.getUniqueId());
				if(listToUse == null){
					sender.sendMessage(Messages.playerHasNoRequests(player.getName()));
					return true;
				}
				forPerson = true;
				sender.sendMessage(ChatColor.BLUE + "Displaying requests for " + player.getName() + ":");
				sender.sendMessage(ChatColor.WHITE + "White" + ChatColor.BLUE+ " ids have not been completed," + ChatColor.GREEN + 
						" green" + ChatColor.BLUE + " ids are pending collection");
			}
			else{
				sender.sendMessage(Messages.playerNotFound());
				return true;
			}
		}
		
		if(!forPerson)
			sender.sendMessage(ChatColor.BLUE + "Displaying active requests:");
		
		displayListToSender(sender, listToUse, page);		
		
		return true;
	}

	public static boolean cancelRequest(CommandSender sender, String[] args){
		if(!sender.hasPermission("mcc.cancel"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		if(args.length < 1)
		{
			if(!sender.hasPermission("mcc.cancel.other"))
			{
				sender.sendMessage(ChatColor.RED + "Usage is " + ChatColor.WHITE + "/mcccancel <id>" + ChatColor.RED);
				sender.sendMessage(ChatColor.RED + "Use " + ChatColor.WHITE + "/mcclist mine" + ChatColor.RED + " for the correct ID");
				return true;
			}
			return false;
		}
		
		int idParam = 0;
		boolean nameSent = false;
		
		if(args.length == 2){
			idParam = 1;
			nameSent = true;
		}
		
		if(!nameSent && !(sender instanceof Player)){
			sender.sendMessage(Messages.playersOnly());
			return true;
		}
		
		if(nameSent && !sender.hasPermission("mcc.cancel.other"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		int id = 0;
		
		try{
			if(args.length > 0){
				id = Integer.parseInt(args[idParam]) - 1;
			}
		}catch(NumberFormatException e){
			if(!sender.hasPermission("mcc.cancel.other"))
			{
				sender.sendMessage(ChatColor.RED + "Usage is " + ChatColor.WHITE + "/mcccancel <id>" + ChatColor.RED);
				sender.sendMessage(ChatColor.RED + "Use " + ChatColor.WHITE + "/mcclist mine" + ChatColor.RED + " for the correct ID");
				return true;
			}
			return false;
		}
		
		if(id < 0)
		{
			if(!sender.hasPermission("mcc.cancel.other"))
			{
				sender.sendMessage(ChatColor.RED + "Usage is " + ChatColor.WHITE + "/mcccancel <id>" + ChatColor.RED);
				sender.sendMessage(ChatColor.RED + "Use " + ChatColor.WHITE + "/mcclist mine" + ChatColor.RED + " for the correct ID");
				return true;
			}
			return false;
		}
		
		UUID pid = null;
		
		if(nameSent){
			String name = args[0];
			Player p = Bukkit.getPlayer(name);
			if(p != null){
				pid = p.getUniqueId();
			}
			else{
				sender.sendMessage(Messages.playerNotFound());
				return true;
			}
		}
		else{
			pid = ((Player) sender).getUniqueId();
		}
		
		MCClassifieds.cancelRequest(sender, pid, id);		
		return true;
	}
	
	public static boolean getRequest(CommandSender sender, String[] args){
		if(!sender.hasPermission("mcc.get"))
		{
			sender.sendMessage(Messages.noPermission());
			return true;
		}
		
		if(!(sender instanceof Player)){
			sender.sendMessage(Messages.playersOnly());
			return true;
		}
		
		if(args.length != 1)
		{
			sender.sendMessage(ChatColor.RED + "Usage is " + ChatColor.WHITE + "/mccget <id>" + ChatColor.RED);
			sender.sendMessage(ChatColor.RED + "Use " + ChatColor.WHITE + "/mcclist mine" + ChatColor.RED + " for the correct ID");
			return true;
		}
		
		int id = 0;
		
		try{
			if(args.length > 0){
				id = Integer.parseInt(args[0]) - 1;
			}
		}catch(NumberFormatException e){
			return false;
		}
		
		if(id < 0)
			return false;
				
		MCClassifieds.getRequest((Player) sender, id);		
		return true;
	}
	
	private static void displayListToSender(CommandSender sender, List<Request> listToUse, int page) {
		int startIdx, stopIdx, maxPage;
		
		Collections.sort(listToUse);
		
		maxPage = (listToUse.size() / 10) + 1;
		
		page = Math.min(maxPage, page);
		
		startIdx = (page - 1) * 10;
		stopIdx = Math.min(listToUse.size(), startIdx + 10);
		
		sender.sendMessage(ChatColor.GRAY + "Page " + page + " of " + maxPage);
		sender.sendMessage(Messages.breakLine());
		for(int i = startIdx; i < stopIdx; i++){
			Request req = listToUse.get(i);
			String toSend = req.isPending ? ChatColor.GREEN + "" : ChatColor.WHITE + "";
			toSend += (i + 1) + ". ";
			toSend += ChatColor.YELLOW + "" + req.amount + " ";
			toSend += ChatColor.AQUA + getItemDisplayName(req.item) + " ";
			toSend += ChatColor.BLUE + "for " + ChatColor.GOLD + "$" + req.price + " ";
			toSend += ChatColor.BLUE + "- " + Bukkit.getOfflinePlayer(req.requestingPlayer).getName();
			
			sender.sendMessage(toSend);
		}	
	}
	
	private static String getItemDisplayName(ItemStack item){
		String display = MCClassifieds.getItemName(item);
		Map<Enchantment, Integer> map = item.getEnchantments();
		if(!map.isEmpty()){
			String prefix = "";
			for(Enchantment en : map.keySet()){
				prefix += MCClassifieds.getEnchantmentCommonName(en) + " " + map.get(en) + " "; 
			}
			display = prefix + display;
		}
		
		return display;
	}
			
	private static boolean doesInvContainRequest(PlayerInventory inv, Request req){
		int amountInInventory = 0;
		for(ItemStack item : inv.getStorageContents()){
			if(item == null)
				continue;
			ItemStack dummy = new ItemStack(item);
			
			//clear out any repair penalty to check if everything else is the same.
			if(dummy.hasItemMeta() && dummy.getItemMeta() instanceof Repairable)
			{
				Repairable rmeta = (Repairable) dummy.getItemMeta();
				rmeta.setRepairCost(0);
				dummy.setItemMeta((ItemMeta) rmeta);
			}
			
			if(dummy.isSimilar(req.item)){
				amountInInventory += dummy.getAmount();
			}
			
			if(amountInInventory >= req.amount){
				//set the requested item equal to this item to carry over repair costs. Everything else should remain the same.
				//however, this guarantees that the item taken is the item given.
				req.item = new ItemStack(item); 
				req.item.setAmount(1);
				return true;
			}
		}
		
		return false;
	}

}
