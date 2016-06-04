package me.becja10.MCClassifieds.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.becja10.MCClassifieds.MCClassifieds;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class WizardPlayer {
	
	public UUID id;
	public ItemStack item;
	public int amount;
	public int price;
	public List<Enchantment> possbileEnchantments;
	public PotionEffect effect;
	public int effectLevel;
	public int enchantmentLevel;
	public int wizStep; //0: item, 1: enchant, 2: potion, 3: amount, 4: price, 5: confirm
	
	public WizardPlayer(UUID id){
		this.id = id;
		item = null;
		amount = 0;
		price = 0;
		possbileEnchantments = new ArrayList<Enchantment>();
		effect = null;
		effectLevel = 0;
		enchantmentLevel = 0;
		wizStep = 0;
	}
	
	public void goToNextStep(){
		if(wizStep == 1)
		{
			if(item.getMaxStackSize() == 1){
				amount = 1;
				wizStep = 4;
			}
			else
				wizStep = 3;
		}
		
		return; //only enchantment allows for next ATM
	}
	
	public String getPromptForStep(){
		
		switch(wizStep){
		case 0:
			return ChatColor.GREEN + "Please enter the item you would like to request (eg. \"DiamondSword\" or \"264\").";
		case 1:
			return ChatColor.GREEN + "Please pick an enchantment and level (eg \"Sharpness 3\") from this list. "
					+ "Once you've added all the "
					+ "enchantments you want, type \"Next\" to continue";
		case 2:
			return ChatColor.GREEN + "Please select a potion effect";
		case 3:
			return ChatColor.GREEN + "How many of this item do you want to get?";
		case 4:
			return ChatColor.GREEN + "How much will you pay?";
		case 5:
			return confirmMessage();
		}
		return "";
	}
	
	private String confirmMessage(){
		String ret = "";
		
		ret += ChatColor.GREEN + "You've finished the wizard and have requested:\n";
		ret += ChatColor.GOLD + "     " + amount + " " +  MCClassifieds.itemDb.name(item) + " for $" + price + "\n";
		Map<Enchantment, Integer> map = item.getEnchantments();
		if(!map.isEmpty()){
			ret += ChatColor.GREEN + "Enchanted with:\n";
			for(Enchantment en : map.keySet()){
				ret += ChatColor.GOLD + "     " + en.getName() + " " + map.get(en) + "\n";
			}
		}
		
		ret += ChatColor.GREEN + "To finalize your request, type \"Confirm\".";		
		return ret;
	}

	public Request createRequest() {
		return new Request(item, price, amount, id);
	}

}
