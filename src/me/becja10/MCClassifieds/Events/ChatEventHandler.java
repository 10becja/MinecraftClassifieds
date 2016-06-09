package me.becja10.MCClassifieds.Events;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import com.earth2me.essentials.Enchantments;

import me.becja10.MCClassifieds.MCClassifieds;
import me.becja10.MCClassifieds.Utils.Messages;
import me.becja10.MCClassifieds.Utils.Request;
import me.becja10.MCClassifieds.Utils.WizardPlayer;

public class ChatEventHandler implements Listener {

	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onAsyncChat(AsyncPlayerChatEvent event){
		
		Player player = event.getPlayer();
		if(!MCClassifieds.wizardPlayers.containsKey(player.getUniqueId()))
			return;
		else
			event.setCancelled(true);
		
		WizardPlayer wp = MCClassifieds.wizardPlayers.get(player.getUniqueId());
		
		String[] input = event.getMessage().split(" ");
		if(input.length < 1)
			return;
		
		if(input[0].equalsIgnoreCase("quit")){
			MCClassifieds.wizardPlayers.remove(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN + "Cancelling request. Good Bye!");
			return;
		}
		
		if(input[0].equalsIgnoreCase("next")){
			wp.goToNextStep();
			player.sendMessage(wp.getPromptForStep());
			return;
		}
		
		switch(wp.wizStep){
			case 0: //select item
				selectItemStep(player, wp, input);
				return;

			case 1: //select enchantment(s) and level
				selectEnchantments(player, wp, input);
				return;
				
			case 2: //select potion effect(s)
				
				
				return;
			case 3: //select quantity
				selectQuantity(player, wp, input);
				return;
				
			case 4: //select price
				selectPrice(player, wp, input);
				return;
				
			case 5: //confirm
				if(input[0].equalsIgnoreCase("confirm"))
				{
					if(wp.price <= MCClassifieds.econ.getBalance(player))
					{
						EconomyResponse r = MCClassifieds.econ.withdrawPlayer(player, wp.price);
						if(r.transactionSuccess())
						{
							Request req = wp.createRequest();
							MCClassifieds.newRequest(req);
							player.sendMessage(Messages.prefix + ChatColor.GREEN + "Request added! $" + req.price + " has been deducted from your account."
									+ " If the request is canceled, you will be refunded your money.");
						}
						else{
							player.sendMessage(Messages.incompleteTransaction());
						}
					}
					else{
						player.sendMessage(ChatColor.RED + "You do not have enough money to create this request. Request cancelled.");
					}
					MCClassifieds.wizardPlayers.remove(player.getUniqueId());
				}				
				return;		
		}		
	}

	private void selectPrice(Player player, WizardPlayer wp, String[] input) {
		int price;
		try{
			price = Integer.parseInt(input[0]);
		}catch(NumberFormatException ex){
			invalidResponse(player, wp);
			return;
		}
		
		if(price <= 0){
			invalidResponse(player, wp);
			return;
		}
		if(price > MCClassifieds.econ.getBalance(player)){
			player.sendMessage(ChatColor.RED + "You would not be able to pay this amount with your current balance. "
					+ "Please enter an amount less than your balance, or \"Quit\" to exit the wizard.");
			player.sendMessage(Messages.breakLine());
			return;					
		}
		
		wp.price = price;
		wp.wizStep = 5;
		player.sendMessage(wp.getPromptForStep());
		player.sendMessage(Messages.breakLine());
		return;
	}

	private void selectQuantity(Player player, WizardPlayer wp, String[] input) {
		int amount;
		try{
			amount = Integer.parseInt(input[0]);
		}catch(NumberFormatException ex){
			invalidResponse(player, wp);
			return;
		}
		
		if(amount <= 0){
			invalidResponse(player, wp);
			return;
		}
						
		if(amount > wp.item.getMaxStackSize() * 36)
		{
			player.sendMessage(ChatColor.RED + "This amount is too high!");
			return;
		}
		
		wp.amount = amount;
		wp.wizStep = 4;
		player.sendMessage(wp.getPromptForStep());
		player.sendMessage(Messages.breakLine());
		return;
	}

	private void selectEnchantments(Player player, WizardPlayer wp, String[] input) {
		ItemStack dummy = new ItemStack(wp.item);
		Enchantment en; int level;
		try{
			en = Enchantments.getByName(input[0]);
			if(en == null)
				throw new NumberFormatException();
			level = (input.length > 1) ? Integer.parseInt(input[1]) : 1;
		}catch(NumberFormatException ex){
			invalidResponse(player, wp);
			return;
		}
		try{
			dummy.addEnchantment(en, level);
		}catch(IllegalArgumentException ex){
			player.sendMessage(ChatColor.RED+"That enchantment is not possible. Please try again.");
			player.sendMessage(Messages.breakLine());
			return;
		}
		
		wp.item.addEnchantment(en, level);
		
		player.sendMessage(ChatColor.GREEN + "Enchantment added! Add another, or type \"Next\" to continue.");
		player.sendMessage(Messages.breakLine());
		
		return;
	}

	private void selectItemStep(Player player, WizardPlayer wp, String[] input) {
		ItemStack item = null;
		try {
			item = new ItemStack(MCClassifieds.itemDb.get(input[0]));
		} catch (Exception e) {
			invalidResponse(player, wp);
			return;
		}				
		item.setAmount(1);
		
		if(MCClassifieds.blacklistItems.contains(item.getType().toString())){
			player.sendMessage(ChatColor.RED + "This item is not allowed!");
			return;
		}
		if(item.getType() == Material.SPLASH_POTION || item.getType() == Material.POTION){
			player.sendMessage(ChatColor.RED + "Potions are not yet supported. Please enter another item, or type \"Quit\" to exit.");
			return;
		}
		wp.item = item;
		for(Enchantment en : Enchantment.values()){
			ItemStack temp = new ItemStack(wp.item);
			try{
				temp.addEnchantment(en, 1);
			}catch(Exception ex){
				if(ex instanceof IllegalArgumentException)
					continue;
				else{ //this is to catch trying to request blocks that are unobtainable such as melon stems
					player.sendMessage(ChatColor.RED + "This item is not allowed!");
					return;
				}
			}
			wp.possbileEnchantments.add(en);
		}
		if(wp.possbileEnchantments.isEmpty()){
			if(wp.item.getType() == Material.SPLASH_POTION || wp.item.getType() == Material.POTION)
				wp.wizStep = 2;
			else
				wp.wizStep = 3;
		}
		else
			wp.wizStep = 1;
		
		player.sendMessage(wp.getPromptForStep());
		player.sendMessage(Messages.breakLine());
		return;
	}
	
	private void invalidResponse(Player player, WizardPlayer wp) {
		player.sendMessage(Messages.invalidResponse());
		player.sendMessage(Messages.breakLine());
	}

}
