package me.becja10.MCClassifieds.Utils;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import me.becja10.MCClassifieds.MCClassifieds;

public class Request {
	
	public int id;
	public ItemStack item;
	public int amount;
	public int price;
	public UUID requestingPlayer;
	public long createDate;
	
	public Request(ItemStack item, int price, int amount, UUID id){
		this.id = MCClassifieds.getNextId();
		this.item = item;
		this.amount = amount;
		this.price = price;
		this.requestingPlayer = id;
		this.createDate = System.currentTimeMillis();
	}

}
