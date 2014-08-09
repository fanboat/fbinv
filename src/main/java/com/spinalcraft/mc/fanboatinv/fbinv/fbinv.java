//fbinv inventory management plugin for minecraft with bukkit
//Richie 'Fanboat' Davidson 8/2014
//for SpinalCraft
//lots of help from http://wiki.bukkit.org/Plugin_Tutorial
/*
 * Players can have their inventories removed and replaced with one of
 * multiple kits, then later have the kit stripped and the original
 * inventories restored. May be manually applied through commands or
 * triggered by external cues. Utilizes a database to safely store player
 * inventories.
 */

package com.spinalcraft.mc.fanboatinv.fbinv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class fbinv extends JavaPlugin {
	
	//input stuff
	//db stuff
	//TODO get that db url
	String DB_URL = "http://mc.spinalcraft.com";
	String USER = "root";
	String PASS = "password";
	Connection conn = null;
	Statement stmt = null;
	//logic stuff
	//these should probably not be global, fix eventually
	boolean invBool = true;//state of the player's inventory (FALSE = natural, TRUE = kit)
	ItemStack armStore[];//stored armor of existing player
	ItemStack invStore[];//stored inventory of existing player

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("fbinv")) {//if standard swap command is called
			//sender.sendMessage("you called fbinv");
			if (args.length > 2) {
		       sender.sendMessage("Too many arguments!");
		       return false;
		    } 
		    if (args.length < 1) {
		    	//apply directly to sender
		    	if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player");
					sender.sendMessage("please use /fbinv <playername>");
					sender.sendMessage("or /fbinv <playername> <kitnumber>");
				} else {
					//forward info to swapping method
					String myTarget = ((Player) sender).getName();
					String[] args2;
					args2 = new String[2];
					args2[0] = myTarget;
					args2[1] = "0";
					swapInv(sender, cmd, label, args2);
				}
				return true;
		    }
		    swapInv(sender, cmd, label, args);
			return true;
		} 
		
		if (cmd.getName().equalsIgnoreCase("fbinvr")) {//if restore command is called
			if (args.length > 1) {
				sender.sendMessage("Too many arguments!");
				return false;
			}
			if (args.length < 1) {
		    	//apply directly to sender
		    	if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player");
					sender.sendMessage("please use /fbinv <playername>");
					sender.sendMessage("or /fbinv <playername> <kitnumber>");
				} else {
					//forward info to swapping method
					String myTarget = ((Player) sender).getName();
					String[] args2;
					args2 = new String[1];
					args2[0] = myTarget;
					reloadKit(sender, args2);
				}
				return true;
		    }
		    reloadKit(sender, args);
			return true;
		}
		
		return false; 
	}
	
	@Override
    public void onEnable() {
        //Insert logic to be performed when the plugin is enabled?
		getLogger().info("onEnable has been invoked!");
		//Bukkit.broadcastMessage("onEnable has been invoked!");
    }
 
    @Override
    public void onDisable() {
        //Insert logic to be performed when the plugin is disabled?
    	getLogger().info("onDisable has been invoked!");
    	//Bukkit.broadcastMessage("onDisable has been invoked!");
    }
    
    public boolean swapInv(CommandSender sender, Command cmd, String label, String[] args){
    	String targetStr;
    	int kitNum;
    	boolean ret;
    	//sender.sendMessage("This is working so far");
    	if (!isInt(args[0])){//if the first argument is not an integer
    		//It is the target's name
    		targetStr = args[0];
    		if (args.length > 1){
    			if (isInt(args[1])){//if the second argument is an integer
        			//choose that kit
        			kitNum = Integer.parseInt(args[1]);
        		}
        		else {//otherwise
        			//rotate back or do nothing
        			kitNum = 0;
        		}
    		}
    		else {//otherwise
    			//rotate back or do nothing
    			kitNum = 0;
    		}
    	}
    	else {//if the first argument is an integer
    		//choose that kit
    		kitNum = Integer.parseInt(args[0]);
    		//make sure sender is player
    		if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player");
				sender.sendMessage("please use /fbinv <playername>");
				sender.sendMessage("or /fbinv <playername> <kitnumber>");
				return false;
    		}
			else {
				//assign sender as player
	    		targetStr = ((Player) sender).getName();
			}
    	}
    	
    	//Check if player is on server
    	Player target = Bukkit.getServer().getPlayer(targetStr);//TODO??
    	if (target == null) {
            sender.sendMessage(targetStr + " is not online!");
            return false;
        }
    	
    	//check DB for player bool
    	//invBool = ...//TODO
    	
    	if (invBool) {//If the player's inv is in a natural state
    		//Send them to the kit switch
    		if (kitNum != 0) {
    			ret = giveKit(sender, target, kitNum);
    		}
    		else {
    			sender.sendMessage(targetStr + " is not holding a kit!");
    			ret = false;
    		}
    	}
    	else {//if the player is holding a kit
    		//Send them to the storage switch
    		ret = restoreInv(sender, target);
    	}
    	return ret;
    }
    
    public boolean giveKit(CommandSender sender, Player target, int kitNum){
    	//copy player's inv to storage, clear inv, give kit
    	
    	//String targetStr;
    	ItemStack kitArmor[];//desired armor from kit
    	kitArmor = new ItemStack[4];
    	ItemStack kitItems[];//desired inventory from kit
    	kitItems = new ItemStack[2];
    	
    	//targetStr = ((Player) target).getName();
    	//sender.sendMessage("You want to give a kit to "+targetStr);
    	//sender.sendMessage("You want to give kit number "+kitNum);
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	invStore = inventorymain.getContents();//obtain current items
    	armStore = inventorymain.getArmorContents();//obtain current armor
    	invBool = false;//switch bool to false
    	//TODO store the player's inventory, armor bool and kitnum here
    	
    	inventorymain.clear();//delete the player's current inventory
    	
    	//Here I'm manually building the kit, but it'll pull the relevant kit from the db
    	//in later revisions
    	kitItems[0] = new ItemStack(Material.BAKED_POTATO, 10);
    	kitItems[1] = new ItemStack(Material.DIAMOND, 10);
    	
    	//Set armor into array
    	//NOTE!: Critical that this order be preserved!
    	//Boots, Leggings, Chestplate, Helmet
    	//changing this order will cause bugs
    	kitArmor[0] = new ItemStack(Material.LEATHER_BOOTS, 1);
    	kitArmor[1] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
    	kitArmor[2] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
    	kitArmor[3] = new ItemStack(Material.LEATHER_HELMET, 1);
    	
    	inventorymain.addItem(kitItems);//supply the player with the designated items
    	inventorymain.setArmorContents(kitArmor);//equip player with designated armor
    	
    	return true;
    }
    
    public boolean reloadKit(CommandSender sender, String[] args){
    	//reload the kit of a player who is carrying one
    	
    	String targetStr;
    	ItemStack kitArmor[];//desired armor from kit
    	kitArmor = new ItemStack[4];
    	ItemStack kitItems[];//desired inventory from kit
    	kitItems = new ItemStack[2];
    	
    	targetStr = args[0];
    	
    	//TODO pull the target's bool and kitnum from db
    	
    	Player target = Bukkit.getServer().getPlayer(targetStr);//TODO??
    	if (target == null) {
            sender.sendMessage(targetStr + " is not online!");
            return false;
        }
    	if (invBool) {//if the player is not carrying a kit
    		sender.sendMessage(targetStr + " does not have a kit to reload!");
    		return false;
    	}
    	
    	//elsewise reload the player's kit
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	inventorymain.clear();//delete the player's current inventory
    	
    	//Here I'm manually building the kit, but it'll pull the relevant kit from the db
    	//in later revisions. USE KITNUM FROM DB TO SELECT KIT
    	kitItems[0] = new ItemStack(Material.BAKED_POTATO, 10);
    	kitItems[1] = new ItemStack(Material.DIAMOND, 10);
    	
    	//Set armor into array
    	//NOTE!: Critical that this order be preserved!
    	//Boots, Leggings, Chestplate, Helmet
    	//changing this order will cause bugs
    	kitArmor[0] = new ItemStack(Material.LEATHER_BOOTS, 1);
    	kitArmor[1] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
    	kitArmor[2] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
    	kitArmor[3] = new ItemStack(Material.LEATHER_HELMET, 1);
    	
    	inventorymain.addItem(kitItems);//supply the player with the designated items (in full, undamaged)
    	inventorymain.setArmorContents(kitArmor);//equip player with designated armor (undamaged)
    	
    	return true;
    }
    
    public boolean restoreInv(CommandSender sender, Player target){
    	//delete player's inventory, restore the stored items, clear the storage
    	
    	String targetStr;
    	targetStr = ((Player) target).getName();
    	//sender.sendMessage("You want to restore the inventory of "+targetStr);
    	
    	//Pull player's inventory from storage
    	//TODO right now this is just a debug test setup
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	inventorymain.setContents(invStore);//restore player's equipment
    	//There's a good chance this line will cause an error if the player was wearing fewer than 4 armor pieces
    	inventorymain.setArmorContents(armStore);//equip player with stored armor
    	invBool = true;
    	
    	//TODO Set player's storage inventory and armor as empty, record new invBool in db
    	
    	return true;
    }
    
    //connect to the ol' db
    public void connectToDatabase() {
		// Establish an SQL connection and create the database
		// if it's not already there.

		// In theory, this only needed to be done once.
		if (conn != null)
			return;

		try {
			String query;

			// Connect to SQL server
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();

			// Make sure the database exists
			stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS FBinv");

			// Use the database
			query = "USE FBinv";
			stmt.executeQuery(query);

			// Make sure the Library table exists
			query = "CREATE TABLE IF NOT EXISTS Library(filename VARCHAR(255) PRIMARY KEY, playbackTime VARCHAR(16), videoCodec VARCHAR(16), audioCodec VARCHAR(16))";
			stmt.executeUpdate(query);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    
    public static boolean isInt(String string) {
    	try {
    		Integer.parseInt(string);
    	} 
    	catch (NumberFormatException nFE) {
    		return false;
    	}
    return true;
    }
}
