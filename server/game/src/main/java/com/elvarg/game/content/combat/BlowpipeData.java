package com.elvarg.game.content.combat;

import com.elvarg.game.content.combat.ranged.RangedData.Ammunition;
import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.game.model.equipment.BonusManager;
import com.elvarg.util.ItemIdentifiers;

import java.util.Set;

public final class BlowpipeData {

    public static final int MAX_SCALES = 16_383;
    public static final int MAX_DARTS = 16_383;

    private static final Set<Integer> BLOWPIPE_IDS = Set.of(
            ItemIdentifiers.TOXIC_BLOWPIPE_EMPTY_,
            ItemIdentifiers.TOXIC_BLOWPIPE
    );

    private static final Set<Integer> LOADABLE_DART_IDS = Set.of(
            ItemIdentifiers.BRONZE_DART,
            ItemIdentifiers.IRON_DART,
            ItemIdentifiers.STEEL_DART,
            ItemIdentifiers.BLACK_DART,
            ItemIdentifiers.MITHRIL_DART,
            ItemIdentifiers.ADAMANT_DART,
            ItemIdentifiers.RUNE_DART,
            ItemIdentifiers.AMETHYST_DART,
            ItemIdentifiers.DRAGON_DART
    );

    private BlowpipeData() {
    }

    public static boolean isBlowpipe(int itemId) {
        return BLOWPIPE_IDS.contains(itemId);
    }

    public static boolean isDartLoadable(int itemId) {
        return LOADABLE_DART_IDS.contains(itemId);
    }

    public static boolean hasDartsLoaded(Player player) {
        return player.getBlowpipeDarts() > 0 && isDartLoadable(player.getBlowpipeDartItemId());
    }

    public static Ammunition getLoadedAmmunition(Player player) {
        if (!hasDartsLoaded(player)) {
            return null;
        }
        return Ammunition.getFor(player.getBlowpipeDartItemId());
    }

    public static boolean handleItemOnItemLoad(Player player, int firstItemId, int secondItemId, int blowpipeSlot) {
        int blowpipeId = -1;
        int loadId = -1;

        if (isBlowpipe(firstItemId)) {
            blowpipeId = firstItemId;
            loadId = secondItemId;
        } else if (isBlowpipe(secondItemId)) {
            blowpipeId = secondItemId;
            loadId = firstItemId;
        }

        if (blowpipeId == -1) {
            return false;
        }

        if (loadId == ItemIdentifiers.ZULRAHS_SCALES) {
            int capacity = MAX_SCALES - player.getBlowpipeScales();
            if (capacity <= 0) {
                player.getPacketSender().sendMessage("Your Toxic blowpipe is already fully charged with scales.");
                return true;
            }

            int available = player.getInventory().getAmount(ItemIdentifiers.ZULRAHS_SCALES);
            int load = Math.min(available, capacity);
            if (load <= 0) {
                return true;
            }

            player.setBlowpipeScales(player.getBlowpipeScales() + load);
            player.getInventory().delete(ItemIdentifiers.ZULRAHS_SCALES, load);
            ensureChargedVariantInInventory(player, blowpipeSlot, blowpipeId);
            notifyState(player);
            return true;
        }

        if (!isDartLoadable(loadId)) {
            player.getPacketSender().sendMessage("You can only load unpoisoned darts and Zulrah's scales into the blowpipe.");
            return true;
        }

        int loadedType = player.getBlowpipeDartItemId();
        if (player.getBlowpipeDarts() > 0 && loadedType != -1 && loadedType != loadId) {
            player.getPacketSender().sendMessage("You need to unload your current darts before loading another type.");
            return true;
        }

        int capacity = MAX_DARTS - player.getBlowpipeDarts();
        if (capacity <= 0) {
            player.getPacketSender().sendMessage("Your Toxic blowpipe is already fully loaded with darts.");
            return true;
        }

        int available = player.getInventory().getAmount(loadId);
        int load = Math.min(available, capacity);
        if (load <= 0) {
            return true;
        }

        if (player.getBlowpipeDarts() <= 0) {
            player.setBlowpipeDartItemId(loadId);
        }
        player.setBlowpipeDarts(player.getBlowpipeDarts() + load);
        player.getInventory().delete(loadId, load);
        ensureChargedVariantInInventory(player, blowpipeSlot, blowpipeId);
        notifyState(player);
        return true;
    }

    public static boolean handleUncharge(Player player, int itemId, int slot) {
        if (handleUnloadDarts(player, itemId, slot)) {
            return true;
        }
        return handleUnchargeScales(player, itemId, slot);
    }

    public static boolean handleUnloadDarts(Player player, int itemId, int slot) {
        if (!isBlowpipe(itemId)) {
            return false;
        }
        if (slot < 0 || slot >= player.getInventory().capacity()) {
            return true;
        }

        Item invItem = player.getInventory().getItems()[slot];
        if (invItem == null || invItem.getId() != itemId) {
            return true;
        }

        int darts = player.getBlowpipeDarts();
        int dartId = player.getBlowpipeDartItemId();
        if (darts <= 0 || dartId <= 0) {
            player.getPacketSender().sendMessage("Your Toxic blowpipe has no darts loaded.");
            return true;
        }

        int neededSlots = 0;
        if (!player.getInventory().contains(dartId)) {
            neededSlots++;
        }
        if (neededSlots > player.getInventory().getFreeSlots()) {
            player.getPacketSender().sendMessage("You need more inventory space to unload your darts.");
            return true;
        }

        player.setBlowpipeDarts(0);
        player.getInventory().add(dartId, darts);
        syncInventoryVariant(player, slot);
        syncWeaponVariant(player);
        BonusManager.update(player);
        player.getPacketSender().sendMessage("You unload the darts from your Toxic blowpipe.");
        notifyState(player);
        return true;
    }

    public static boolean handleUnchargeScales(Player player, int itemId, int slot) {
        if (!isBlowpipe(itemId)) {
            return false;
        }
        if (slot < 0 || slot >= player.getInventory().capacity()) {
            return true;
        }

        Item invItem = player.getInventory().getItems()[slot];
        if (invItem == null || invItem.getId() != itemId) {
            return true;
        }

        int scales = player.getBlowpipeScales();
        if (scales <= 0) {
            player.getPacketSender().sendMessage("Your Toxic blowpipe has no scales loaded.");
            return true;
        }

        int neededSlots = 0;
        if (!player.getInventory().contains(ItemIdentifiers.ZULRAHS_SCALES)) {
            neededSlots++;
        }
        if (neededSlots > player.getInventory().getFreeSlots()) {
            player.getPacketSender().sendMessage("You need more inventory space to remove the scales.");
            return true;
        }

        player.setBlowpipeScales(0);
        player.getInventory().add(ItemIdentifiers.ZULRAHS_SCALES, scales);
        syncInventoryVariant(player, slot);
        syncWeaponVariant(player);
        BonusManager.update(player);
        player.getPacketSender().sendMessage("You uncharge your Toxic blowpipe and recover the scales.");
        notifyState(player);
        return true;
    }

    public static void notifyState(Player player) {
        int scales = player.getBlowpipeScales();
        int dartCount = player.getBlowpipeDarts();

        String darts = "darts";
        if (dartCount > 0 && player.getBlowpipeDartItemId() > 0) {
            ItemDefinition def = ItemDefinition.forId(player.getBlowpipeDartItemId());
            darts = (def != null ? def.getName() : "darts").toLowerCase();
        }

        StringBuilder message = new StringBuilder("Your Toxic blowpipe has ")
                .append(scales)
                .append(" scales and ")
                .append(dartCount)
                .append(" ")
                .append(darts)
                .append(" loaded.");

        if (scales <= 0 && dartCount <= 0) {
            message.append(" It has no scales or darts loaded.");
        } else if (scales <= 0) {
            message.append(" It has no scales loaded.");
        } else if (dartCount <= 0) {
            message.append(" It has no darts loaded.");
        }

        player.getPacketSender().sendMessage(message.toString());
    }

    public static int consumeScaleShots(Player player, int shots) {
        if (shots <= 0 || player.getBlowpipeScales() <= 0) {
            return 0;
        }

        int consumed = 0;
        for (int i = 0; i < shots; i++) {
            // OSRS behavior: 1 in 3 shots does not consume a scale.
            if (com.elvarg.util.Misc.getRandom(2) == 0) {
                continue;
            }
            if (player.getBlowpipeScales() <= 0) {
                break;
            }
            player.setBlowpipeScales(player.getBlowpipeScales() - 1);
            consumed++;
        }
        return consumed;
    }

    public static boolean hasRequiredCharges(Player player, int shotsRequired) {
        if (player.getBlowpipeScales() <= 0) {
            player.getPacketSender().sendMessage("You must load Zulrah scales into your Toxic blowpipe.");
            return false;
        }
        if (!hasDartsLoaded(player) || player.getBlowpipeDarts() < shotsRequired) {
            player.getPacketSender().sendMessage("You must load darts into your Toxic blowpipe.");
            return false;
        }
        return true;
    }

    public static void syncWeaponVariant(Player player) {
        boolean hasCharges = player.getBlowpipeScales() > 0 || player.getBlowpipeDarts() > 0;
        int equipped = player.getEquipment().get(Equipment.WEAPON_SLOT).getId();

        if (!isBlowpipe(equipped)) {
            return;
        }

        int desired = hasCharges ? ItemIdentifiers.TOXIC_BLOWPIPE : ItemIdentifiers.TOXIC_BLOWPIPE_EMPTY_;
        if (equipped == desired) {
            return;
        }

        player.getEquipment().setItem(Equipment.WEAPON_SLOT, new Item(desired));
        player.getEquipment().refreshItems();
        BonusManager.update(player);
        WeaponInterfaces.assign(player);

        player.getCombat().setRangedWeapon(com.elvarg.game.content.combat.ranged.RangedData.RangedWeapon.getFor(player));
    }

    private static void ensureChargedVariantInInventory(Player player, int slot, int itemId) {
        if (slot < 0 || slot >= player.getInventory().capacity()) {
            return;
        }
        if (itemId == ItemIdentifiers.TOXIC_BLOWPIPE) {
            return;
        }
        Item slotItem = player.getInventory().getItems()[slot];
        if (slotItem != null && isBlowpipe(slotItem.getId())) {
            player.getInventory().setItem(slot, new Item(ItemIdentifiers.TOXIC_BLOWPIPE));
            player.getInventory().refreshItems();
        }
    }

    private static void syncInventoryVariant(Player player, int slot) {
        if (slot < 0 || slot >= player.getInventory().capacity()) {
            return;
        }
        Item current = player.getInventory().getItems()[slot];
        if (current == null || !isBlowpipe(current.getId())) {
            return;
        }
        boolean hasCharges = player.getBlowpipeScales() > 0 || player.getBlowpipeDarts() > 0;
        int desired = hasCharges ? ItemIdentifiers.TOXIC_BLOWPIPE : ItemIdentifiers.TOXIC_BLOWPIPE_EMPTY_;
        if (current.getId() != desired) {
            player.getInventory().setItem(slot, new Item(desired));
        }
        player.getInventory().refreshItems();
    }
}
