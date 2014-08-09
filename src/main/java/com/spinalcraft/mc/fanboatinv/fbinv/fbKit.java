//fbKit is a data structure that holds a player's inventory and armor.
//Richie 'Fanboat' Davidson 8/2014
//for SpinalCraft
/*
 * This class holds a set of armor and an inventory. This can be copied
 * from an existing player or applied to them in the form of a kit as
 * described in the fbinv system
 */

package com.spinalcraft.mc.fanboatinv.fbinv;

import org.bukkit.inventory.ItemStack;

public class fbKit {
	ItemStack[] Items;
	ItemStack[] Armors;
	
	//constructor
	public fbKit(ItemStack[] items, ItemStack[] armors)
	{
		Items = items;
		Armors = armors;
	}
}
