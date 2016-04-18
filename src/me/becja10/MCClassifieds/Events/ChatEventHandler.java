package me.becja10.MCClassifieds.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import me.becja10.MCClassifieds.MCClassifieds;
import me.becja10.MCClassifieds.Utils.Messages;
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
		
		String input = event.getMessage().replace(" ", "");
		
		
		//Figure out where they are in the wizard
		
		//they haven't given an item yet
		if(wp.item == null){
			ItemStack item = null;
			try {
				item = MCClassifieds.itemDb.get(input);
			} catch (Exception e) {
				player.sendMessage(Messages.invalidResponse());
				player.sendMessage(Messages.breakLine());
				player.sendMessage(Messages.requestItem());
				return;
			}
			wp.item = item;
		}
		
	}

}
