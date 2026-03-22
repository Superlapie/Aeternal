package com.elvarg.game.content;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.grounditem.ItemOnGroundManager;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;

public final class Looting {

    private Looting() {
    }

    public static void addOrDrop(Player player, int itemId, int amount) {
        if (player == null || itemId <= 0 || amount <= 0) {
            return;
        }

        int before = player.getInventory().getAmount(itemId);
        player.getInventory().add(itemId, amount);
        int after = player.getInventory().getAmount(itemId);
        int added = Math.max(0, after - before);
        int leftover = amount - added;

        if (leftover <= 0) {
            return;
        }

        ItemOnGroundManager.register(player, new Item(itemId, leftover), player.getLocation().clone());
        String itemName = ItemDefinition.forId(itemId).getName();
        player.getPacketSender().sendMessage("You don't have enough inventory space. " + itemName + " was dropped on the ground.");
    }
}
