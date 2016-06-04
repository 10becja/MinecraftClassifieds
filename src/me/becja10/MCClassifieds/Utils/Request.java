package me.becja10.MCClassifieds.Utils;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class Request implements Comparable<Request>{
	
	public UUID id;
	public ItemStack item;
	public int amount;
	public int price;
	public UUID requestingPlayer;
	public Long createDate;
	public boolean isPending;
	
	public Request(ItemStack item, int price, int amount, UUID uuid){
		id = UUID.randomUUID();
		this.item = item;
		this.amount = amount;
		this.price = price;
		this.requestingPlayer = uuid;
		this.createDate = System.currentTimeMillis();
		isPending = false;
	}
	
	public Request(String id){
		this.id = UUID.fromString(id);
	}

	@Override
	public int compareTo(Request arg0) {
		return createDate.compareTo(arg0.createDate);
	}

}
