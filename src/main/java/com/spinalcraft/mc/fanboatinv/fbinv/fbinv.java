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

//NOTE!: Critical that this order be preserved!
    	//Boots, Leggings, Chestplate, Helmet

package com.spinalcraft.mc.fanboatinv.fbinv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

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
	/*
	String DB_URL = "http://mc.spinalcraft.com";
	String USER = "root";
	String PASS = "password";
	Connection conn = null;
	Statement stmt = null;
	*/
	//logic stuff
	//these should not be global, fix eventually
	boolean myGlobalInvBool = true;
	fbKit myGlobalfbKit;
	int myGlobalKitNum = 0;
	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("fb")) {
			String chooseCom = args[0];
			String arg[] = new String[args.length-1];
			arg = Arrays.copyOfRange(args, 1, args.length);
			sender.sendMessage("Test1");//TODO
			
			if (chooseCom.equals("i")){//if standard swap command is called
				sender.sendMessage("Test2");//TODO
				//This command is used to give the player a kit OR restore their old inventory
				if (arg.length < 1) {
			    	//apply directly to sender
			    	if (!(sender instanceof Player)) {
						sender.sendMessage("This command can only be run by a player");
						sender.sendMessage("please use /fb i <playername>");
						sender.sendMessage("or /fb i <playername> <kitnumber>");
						return true;
					} else {
						//forward info to swapping method
						String myTarget = ((Player) sender).getName();
						String[] args2;
						args2 = new String[2];
						args2[0] = myTarget;
						args2[1] = "0";
						return swapInv(sender, cmd, label, args2);
					}
			    }
			    return swapInv(sender, cmd, label, arg);
			}
			if (chooseCom.equals("r")){//if reload command is called
				//This command is used to reload the player's kit if they are carrying one
				if (arg.length > 1) {
					sender.sendMessage("Too many arguments!");
					return true;
				}
				if (arg.length < 1) {
			    	//apply directly to sender
			    	if (!(sender instanceof Player)) {
						sender.sendMessage("This command can only be run by a player");
						sender.sendMessage("please use /fb r <playername>");
						return true;
					} else {
						//forward info to swapping method
						String myTarget = ((Player) sender).getName();
						String[] args2;
						args2 = new String[1];
						args2[0] = myTarget;
						return reloadKit(sender, args2);
					}
			    }
			    return reloadKit(sender, arg);
			}
			if (chooseCom.equals("w")){//if write-kit command is called
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player");
					sender.sendMessage("please use /fb w <playername> <Kit Number>");
					return true;
				}
				if (arg.length > 2) {
					sender.sendMessage("Too many arguments!");
					return true;
				}
				if (arg.length < 1) {
					sender.sendMessage("Please specify a Kit number!");
					return true;
				}
				if (arg.length == 1){
					if (!isInt(arg[0])){//if the argument is not an integer
						sender.sendMessage("Please use /fb w <Kit Number>");
						return true;
					}
					else {
						int kitNum = Integer.parseInt(arg[0]);
						String targetStr = ((Player) sender).getName();
						return writeKit(sender, targetStr, kitNum);
					}
				}
				else {
					if (!isInt(arg[1])){//if the second argument is not an integer
						sender.sendMessage("Please use /fb w <playername> <Kit Number>");
						return true;
					}
					else {//make sure player is online
						Player target = Bukkit.getServer().getPlayer(arg[0]);//TODO??
				    	if (target == null) {
				            sender.sendMessage(arg[0] + " is not online!");
				            return true;
				        }
						int kitNum = Integer.parseInt(arg[0]);
						return writeKit(sender, arg[0], kitNum);
					}
				}
			}
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
    	sender.sendMessage("This is working so far");//TODO
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
				sender.sendMessage("please use /fb i <playername>");
				sender.sendMessage("or /fb i <playername> <kitnumber>");
				return true;
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
            return true;
        }
    	
    	if (checkInvBool(targetStr)) {//If the player's inv is in a natural state
    		//Send them to the kit switch
    		if (kitNum != 0) {//if a kit has been assigned
    			return giveKit(sender, target, kitNum);
    		}
    		else {
    			sender.sendMessage(targetStr + " is not holding a kit!");
    			return true;
    		}
    	}
    	else {//if the player is holding a kit
    		//Send them to the storage switch
    		if (kitNum != 0){//if attempting to assign a kit
    			sender.sendMessage(targetStr + " is already holding a kit!");
    			sender.sendMessage("Remove kit before assigning new one");
    			return true;
    		}
    		else {
    			return restoreInv(sender, target);
    		}
    	}
    }
    
    public boolean giveKit(CommandSender sender, Player target, int kitNum){
    	//copy player's inv to storage, clear inv, give kit

    	ItemStack armStore[];//armor to be stored of target player
    	ItemStack invStore[];//inventory to be stored of target player
    	ItemStack kitArmor[];//desired armor from kit
    	ItemStack kitItems[];//desired inventory from kit
    	fbKit myKit;//kit to be given to player
    	
    	String targetStr = ((Player) target).getName();
    	//sender.sendMessage("You want to give a kit to "+targetStr);
    	//sender.sendMessage("You want to give kit number "+kitNum);
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	invStore = inventorymain.getContents();//obtain current items
    	armStore = inventorymain.getArmorContents();//obtain current armor
    	
    	//store player's current gear in db
    	if (!recordInv(targetStr, false, invStore, armStore, kitNum)){
    		sender.sendMessage("Could not write to database!");
    		return true;
    	}
    	
    	inventorymain.clear();//delete the player's current inventory
    	inventorymain.setBoots(new ItemStack(Material.AIR, 1));//clear all armor
    	inventorymain.setLeggings(new ItemStack(Material.AIR, 1));
    	inventorymain.setChestplate(new ItemStack(Material.AIR, 1));
    	inventorymain.setHelmet(new ItemStack(Material.AIR, 1));
    	
    	myKit = chooseKit(checkKitNum(targetStr));//get the desired kit 
    	kitItems = myKit.Items;
    	kitArmor = myKit.Armors;
    	
    	inventorymain.addItem(kitItems);//supply the player with the designated items
    	inventorymain.setArmorContents(kitArmor);//equip player with designated armor
    	
    	return true;
    }
    
    public boolean reloadKit(CommandSender sender, String[] args){
    	//reload the kit of a player who is carrying one
    	
    	String targetStr;
    	ItemStack kitArmor[];//desired armor from kit
    	ItemStack kitItems[];//desired inventory from kit
    	fbKit myKit;
    	
    	targetStr = args[0];
    	
    	Player target = Bukkit.getServer().getPlayer(targetStr);
    	if (target == null) {
            sender.sendMessage(targetStr + " is not online!");
            return true;
        }
    	if (checkInvBool(targetStr)) {//if the player is not carrying a kit
    		sender.sendMessage(targetStr + " does not have a kit to reload!");
    		return true;
    	}
    	
    	//elsewise reload the player's kit
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	inventorymain.clear();//delete the player's current inventory
    	inventorymain.setBoots(new ItemStack(Material.AIR, 1));//clear all armor
    	inventorymain.setLeggings(new ItemStack(Material.AIR, 1));
    	inventorymain.setChestplate(new ItemStack(Material.AIR, 1));
    	inventorymain.setHelmet(new ItemStack(Material.AIR, 1));
    	
    	myKit = chooseKit(checkKitNum(targetStr));//get the desired kit 
    	kitItems = myKit.Items;
    	kitArmor = myKit.Armors;
    	
    	inventorymain.addItem(kitItems);//supply the player with the designated items (in full, undamaged)
    	inventorymain.setArmorContents(kitArmor);//equip player with designated armor (undamaged)
    	
    	return true;
    }
    
    public boolean restoreInv(CommandSender sender, Player target){
    	//delete player's inventory, restore the stored items, clear the storage
    	
    	//ItemStack armStore[];//stored armor of target player
    	//ItemStack invStore[];//stored inventory of target player
    	fbKit myStore;//stored armor and inventory of target player
    	String targetStr;
    	
    	targetStr = ((Player) target).getName();
    	myStore = retrieveInv(targetStr);
    	
    	PlayerInventory inventorymain = target.getInventory();//connect to target's inventory
    	inventorymain.clear();//delete the player's current inventory
    	inventorymain.setBoots(new ItemStack(Material.AIR, 1));//clear all armor
    	inventorymain.setLeggings(new ItemStack(Material.AIR, 1));
    	inventorymain.setChestplate(new ItemStack(Material.AIR, 1));
    	inventorymain.setHelmet(new ItemStack(Material.AIR, 1));
    	
    	inventorymain.setContents(myStore.Items);//restore player's equipment
    	inventorymain.setArmorContents(myStore.Armors);//equip player with stored armor
    	
    	return true;
    }
    
    public boolean writeKit(CommandSender sender, String targetStr, int kitNum){
    	//Store player's current inventory as a template for later use as a kit
    	sender.sendMessage("This doesn't do anything yet");
    	return true;
    }
    
    public boolean recordInv(String targetStr, boolean invBool, ItemStack[] invStore, ItemStack[] armStore, int kitNum){
    	//Record player's information for later retrieval
    	
    	//This method will write to a db, but right now will just copy to some global vars which will later be removed
    	myGlobalInvBool = invBool;
    	myGlobalfbKit = new fbKit(invStore,armStore);
    	myGlobalKitNum = kitNum;
    	
    	return true;
    }
    
    public boolean checkInvBool(String targetStr){
    	//This will lookup the invBool for the given playername and return it
    	return myGlobalInvBool;
    }
    
    public int checkKitNum(String targetStr){
    	//This will lookup the player's current kit and return it
    	return myGlobalKitNum;
    }
    
    public fbKit retrieveInv(String targetStr){
    	//This will lookup the player's old inventory and return it, and overwrite it to empty
    	ItemStack[] emptyStack = new ItemStack[1];
    	emptyStack[0] = new ItemStack(Material.AIR, 1);
    	
    	myGlobalInvBool = true;
    	myGlobalKitNum = 0;
    	fbKit myKit = myGlobalfbKit;
    	myGlobalfbKit = new fbKit(emptyStack,emptyStack);
    	return myKit;
    }
    
    public fbKit chooseKit(int kitNum){
    	//Pull a kit from the db and return it
    	
    	//Here I'm manually building the kit, but it'll pull the relevant kit from the db
    	//in later revisions. USE KITNUM FROM DB TO SELECT KIT
    	ItemStack kitArmor[];//desired armor from kit
    	kitArmor = new ItemStack[4];
    	ItemStack kitItems[];//desired inventory from kit
    	kitItems = new ItemStack[2];
    	fbKit myKit;
    	
    	kitItems[0] = new ItemStack(Material.BAKED_POTATO, 10);
    	kitItems[1] = new ItemStack(Material.DIAMOND, 10);
    	
    	kitArmor[0] = new ItemStack(Material.LEATHER_BOOTS, 1);
    	kitArmor[1] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
    	kitArmor[2] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
    	kitArmor[3] = new ItemStack(Material.LEATHER_HELMET, 1);
    	
    	myKit = new fbKit(kitItems,kitArmor);
    	
    	return myKit;
    }
    
    //connect to the ol' db
    /*
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
	*/
	
    
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
