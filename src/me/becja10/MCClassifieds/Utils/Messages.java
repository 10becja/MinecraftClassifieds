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
	private static Message requestItem = new Message(ChatColor.GREEN + "Please enter the item you would like to request (eg. \"Diamond Sword\").");
	
	public static String noPermission(){ return noPermission.getMsg();}
	public static String reloadSuccessful(){return reloadSuccessful.getMsg();}
	public static String playersOnly(){return playersOnly.getMsg();}
	public static String nan(){return nan.getMsg();}
	public static String invalidResponse(){return invalidResponse.getMsg();}
	public static String breakLine(){return breakLine.getMsg();}
	public static String requestItem(){return requestItem.getMsg();}
	
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
