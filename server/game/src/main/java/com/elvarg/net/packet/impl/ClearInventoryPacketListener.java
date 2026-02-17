package com.elvarg.net.packet.impl;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.container.impl.Inventory;
import com.elvarg.net.packet.Packet;
import com.elvarg.net.packet.PacketExecutor;

/**
 * This packet listener clears all items from the player's inventory.
 * Used by the ::clearinv command.
 */
public class ClearInventoryPacketListener implements PacketExecutor {

    @Override
    public void execute(Player player, Packet packet) {
        if (player == null) {
            return;
        }

        // Clear all 28 inventory slots
        Item[] items = player.getInventory().getItems();
        int itemsCleared = 0;
        
        for (int slot = 0; slot < items.length; slot++) {
            Item item = items[slot];
            if (item != null && item.getId() > 0) {
                itemsCleared++;
            }
            // Set slot to empty (-1, 0)
            player.getInventory().setItem(slot, new Item(-1, 0));
        }
        
        // Refresh the inventory
        player.getInventory().refreshItems();
        
        // Send confirmation message
        player.getPacketSender().sendMessage("Cleared " + itemsCleared + " items from your inventory.");
    }
}
