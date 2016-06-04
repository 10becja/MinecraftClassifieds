package me.becja10.MCClassifieds.Events;

import java.util.List;

import me.becja10.MCClassifieds.MCClassifieds;
import me.becja10.MCClassifieds.Utils.Request;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEventHandler implements Listener {

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event){
		final Player p = event.getPlayer();
		List<Request> list = MCClassifieds.playerMap.get(p.getUniqueId());
		if(list != null){
			for(Request req : list)
			{
				if(req.isPending){
					
					final String msg = ChatColor.GREEN + "You have requests pending collection. Use " +ChatColor.WHITE +"'/mcclist mine'"
							+ ChatColor.GREEN + " to collect your requests!";
					p.sendMessage(msg);					
					return;
				}
			}
		}
	}
}
