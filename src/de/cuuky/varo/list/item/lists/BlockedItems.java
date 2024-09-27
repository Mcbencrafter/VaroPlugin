package de.cuuky.varo.list.item.lists;

import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import de.cuuky.varo.list.item.ItemList;

public class BlockedItems extends ItemList {

	public BlockedItems() {
		super("BlockedItems", -1, true);
	}
	
	@Override
	public void loadDefaultValues() {
		this.addItem(XMaterial.AIR.parseItem());
	}

	public boolean isBlocked(ItemStack itemstack) {
		itemstack = fixItem(itemstack);
		for (ItemStack stack : this.getItems())
			if (stack.equals(itemstack))
				return true;

		return false;
	}
}