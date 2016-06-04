package me.becja10.MCClassifieds.Utils;

import org.bukkit.ChatColor;

public class Messages {

	public static String prefix = ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "MCClassifieds" + ChatColor.GREEN + "] ";
	
	private static Message noPermission = new Message(ChatColor.DARK_RED + "You do not have permission for this command");
	private static Message reloadSuccessful = new Message(prefix + ChatColor.GREEN + "Reload successful.");
	private static Message playersOnly = new Message(ChatColor.DARK_RED + "This can only be run by players.");
	private static Message nan = new Message(ChatColor.DARK_RED + "Not a number.");
	private static Message invalidResponse = new Message(ChatColor.RED + "I'm sorry, that response is invalid. Please try again, or type \"quit\" to stop setting up a request.");
	private static Message breakLine = new Message(ChatColor.GOLD + "----------------------------");
	private static Message typeNext = new Message(ChatColor.GREEN + "Type \"Next\" to continue.");
	private static Message incompleteTransaction = new Message(ChatColor.RED + "Could not complete transaction.");
	private static Message playerNotFound = new Message(ChatColor.RED + "Player not found.");
	private static Message playerHasNoRequests = new Message(ChatColor.RED + "{0} has no requests.");
	private static Message invalidId = new Message(ChatColor.RED + "Invalid id.");
	
	public static String noPermission(){ return noPermission.getMsg();}
	public static String reloadSuccessful(){return reloadSuccessful.getMsg();}
	public static String playersOnly(){return playersOnly.getMsg();}
	public static String nan(){return nan.getMsg();}
	public static String invalidResponse(){return invalidResponse.getMsg();}
	public static String breakLine(){return breakLine.getMsg();}
	public static String typeNext(){return typeNext.getMsg();}
	public static String incompleteTransaction(){return incompleteTransaction.getMsg();}
	public static String playerNotFound(){return playerNotFound.getMsg();}
	public static String playerHasNoRequests(String player){return playerHasNoRequests.format(player);}
	public static String invalidId(){return invalidId.getMsg();}
	
	private static class Message{
		String msg;
		
		Message(String str)
		{
			msg = str;
		}
		
		String format(Object... args){
			String ret = msg;
			for(int i = 0; i < args.length; i++)
			{
				ret = ret.replace("{" + i + "}", args[i]+"");
			}
			return ret;
		}
		
		String getMsg()
		{
			return msg;
		}
	}
}
