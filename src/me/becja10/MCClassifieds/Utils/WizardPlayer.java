package me.becja10.MCClassifieds.Utils;

import java.util.UUID;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class WizardPlayer {
	
	public UUID id;
	public ItemStack item;
	public Enchantment enchantment;
	public PotionEffect effect;
	public int effectLevel;
	public int enchantmentLevel;
	
	public WizardPlayer(UUID id){
		this.id = id;
		item = null;
		enchantment = null;
		effect = null;
		effectLevel = 0;
		enchantmentLevel = 0;
	}

}
