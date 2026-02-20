package com.elvarg.game.content.combat;

import com.elvarg.game.content.combat.hit.HitDamage;
import com.elvarg.game.content.combat.hit.PendingHit;
import com.elvarg.game.content.combat.WeaponInterfaces.WeaponInterface;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.game.model.equipment.BonusManager;
import com.elvarg.util.ItemIdentifiers;

import java.util.Set;

public final class ScytheData {

    public static final int MAX_CHARGES = 20_000;
    public static final int CHARGES_PER_SET = 100;
    public static final int BLOOD_RUNES_PER_SET = 200;
    public static final int VIALS_PER_SET = 1;

    private static final Set<Integer> CHARGED_VARIANTS = Set.of(
            ItemIdentifiers.SCYTHE_OF_VITUR,
            ItemIdentifiers.SCYTHE_OF_VITUR_3,
            ItemIdentifiers.HOLY_SCYTHE_OF_VITUR,
            ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR
    );

    private static final Set<Integer> UNCHARGED_VARIANTS = Set.of(
            ItemIdentifiers.SCYTHE_OF_VITUR_UNCHARGED_,
            ItemIdentifiers.HOLY_SCYTHE_OF_VITUR_UNCHARGED_,
            ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED_
    );

    private static final Set<Integer> VIAL_OF_BLOOD_IDS = Set.of(
            ItemIdentifiers.VIAL_OF_BLOOD,
            ItemIdentifiers.VIAL_OF_BLOOD_2,
            ItemIdentifiers.VIAL_OF_BLOOD_3,
            ItemIdentifiers.VIAL_OF_BLOOD_4
    );

    private ScytheData() {
    }

    public static boolean isScythe(int itemId) {
        return CHARGED_VARIANTS.contains(itemId) || UNCHARGED_VARIANTS.contains(itemId);
    }

    public static boolean isChargedVariant(int itemId) {
        return CHARGED_VARIANTS.contains(itemId);
    }

    public static boolean isUnchargedVariant(int itemId) {
        return UNCHARGED_VARIANTS.contains(itemId);
    }

    public static int toChargedVariant(int itemId) {
        if (itemId == ItemIdentifiers.SCYTHE_OF_VITUR_UNCHARGED_) {
            return ItemIdentifiers.SCYTHE_OF_VITUR;
        }
        if (itemId == ItemIdentifiers.HOLY_SCYTHE_OF_VITUR_UNCHARGED_) {
            return ItemIdentifiers.HOLY_SCYTHE_OF_VITUR;
        }
        if (itemId == ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED_) {
            return ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR;
        }
        return itemId;
    }

    public static int toUnchargedVariant(int itemId) {
        if (itemId == ItemIdentifiers.SCYTHE_OF_VITUR || itemId == ItemIdentifiers.SCYTHE_OF_VITUR_3) {
            return ItemIdentifiers.SCYTHE_OF_VITUR_UNCHARGED_;
        }
        if (itemId == ItemIdentifiers.HOLY_SCYTHE_OF_VITUR) {
            return ItemIdentifiers.HOLY_SCYTHE_OF_VITUR_UNCHARGED_;
        }
        if (itemId == ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR) {
            return ItemIdentifiers.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED_;
        }
        return itemId;
    }

    public static boolean canAttack(Player player) {
        int weaponId = player.getEquipment().getWeapon().getId();
        if (!isScythe(weaponId)) {
            return true;
        }
        if (player.getScytheCharges() > 0) {
            return true;
        }

        if (isChargedVariant(weaponId)) {
            setEquippedWeapon(player, toUnchargedVariant(weaponId));
        }
        player.getPacketSender().sendMessage("Your scythe has no charges. Add blood runes and vials of blood.");
        return false;
    }

    public static int getHitAmount(Mobile target) {
        int size = Math.max(1, target.size());
        if (size >= 3) {
            return 3;
        }
        if (size == 2) {
            return 2;
        }
        return 1;
    }

    public static void applyHitScaling(PendingHit hit) {
        HitDamage[] hits = hit.getHits();
        if (hits == null || hits.length <= 1) {
            return;
        }

        hits[1].setDamage((int) Math.floor(hits[1].getDamage() * 0.5));
        if (hits.length > 2) {
            hits[2].setDamage((int) Math.floor(hits[2].getDamage() * 0.25));
        }
        hit.updateTotalDamage();
    }

    public static void consumeChargeOnSuccessfulHit(PendingHit pendingHit) {
        if (pendingHit == null || !pendingHit.getAttacker().isPlayer()) {
            return;
        }
        if (pendingHit.getCombatType() != CombatType.MELEE || pendingHit.getTotalDamage() <= 0) {
            return;
        }

        Player player = pendingHit.getAttacker().getAsPlayer();
        int weaponId = player.getEquipment().getWeapon().getId();
        if (!isScythe(weaponId)) {
            return;
        }

        int charges = Math.max(0, player.getScytheCharges() - 1);
        player.setScytheCharges(charges);

        if (charges == 0 && isChargedVariant(weaponId)) {
            setEquippedWeapon(player, toUnchargedVariant(weaponId));
            player.getPacketSender().sendMessage("Your scythe runs out of charges.");
        }
    }

    public static boolean handleCheck(Player player, int itemId) {
        if (!isScythe(itemId)) {
            return false;
        }
        player.getPacketSender().sendMessage("Scythe charges: " + player.getScytheCharges() + "/" + MAX_CHARGES + ".");
        return true;
    }

    public static boolean handleUncharge(Player player, int itemId, int slot) {
        if (!isScythe(itemId)) {
            return false;
        }
        if (slot < 0 || slot >= player.getInventory().capacity()) {
            return true;
        }
        Item invItem = player.getInventory().getItems()[slot];
        if (invItem == null || invItem.getId() != itemId || !isChargedVariant(itemId)) {
            return true;
        }

        int charges = player.getScytheCharges();
        if (charges <= 0) {
            player.getInventory().setItem(slot, new Item(toUnchargedVariant(itemId)));
            player.getInventory().refreshItems();
            player.getPacketSender().sendMessage("Your scythe is already uncharged.");
            return true;
        }

        int sets = charges / CHARGES_PER_SET;
        player.setScytheCharges(0);
        player.getInventory().setItem(slot, new Item(toUnchargedVariant(itemId)));
        if (sets > 0) {
            player.getInventory().add(ItemIdentifiers.BLOOD_RUNE, sets * BLOOD_RUNES_PER_SET);
            player.getInventory().add(ItemIdentifiers.VIAL_OF_BLOOD, sets * VIALS_PER_SET);
        }
        player.getInventory().refreshItems();
        player.getPacketSender().sendMessage("You uncharge your scythe.");
        return true;
    }

    public static boolean handleItemOnItemCharge(Player player, int firstItemId, int secondItemId, int scytheSlot) {
        int scytheId = -1;
        int otherId = -1;

        if (isScythe(firstItemId)) {
            scytheId = firstItemId;
            otherId = secondItemId;
        } else if (isScythe(secondItemId)) {
            scytheId = secondItemId;
            otherId = firstItemId;
        }

        if (scytheId == -1) {
            return false;
        }

        boolean chargingInput = otherId == ItemIdentifiers.BLOOD_RUNE || VIAL_OF_BLOOD_IDS.contains(otherId);
        if (!chargingInput) {
            player.getPacketSender().sendMessage("You cannot charge the scythe with that.");
            return true;
        }

        int runeCount = player.getInventory().getAmount(ItemIdentifiers.BLOOD_RUNE);
        int vialCount = 0;
        for (int vialId : VIAL_OF_BLOOD_IDS) {
            vialCount += player.getInventory().getAmount(vialId);
        }
        int possibleSets = Math.min(runeCount / BLOOD_RUNES_PER_SET, vialCount / VIALS_PER_SET);
        if (possibleSets <= 0) {
            player.getPacketSender().sendMessage("You need blood runes and vials of blood to charge the scythe.");
            return true;
        }

        int freeCapacity = MAX_CHARGES - player.getScytheCharges();
        if (freeCapacity <= 0) {
            player.getPacketSender().sendMessage("Your scythe is already fully charged.");
            return true;
        }

        int setsToAdd = Math.min(possibleSets, freeCapacity / CHARGES_PER_SET);
        if (setsToAdd <= 0) {
            player.getPacketSender().sendMessage("Your scythe is already fully charged.");
            return true;
        }

        player.getInventory().delete(ItemIdentifiers.BLOOD_RUNE, setsToAdd * BLOOD_RUNES_PER_SET);
        int vialsToDelete = setsToAdd * VIALS_PER_SET;
        for (int vialId : VIAL_OF_BLOOD_IDS) {
            if (vialsToDelete <= 0) {
                break;
            }
            int amount = player.getInventory().getAmount(vialId);
            if (amount <= 0) {
                continue;
            }
            int take = Math.min(amount, vialsToDelete);
            player.getInventory().delete(vialId, take);
            vialsToDelete -= take;
        }

        player.setScytheCharges(player.getScytheCharges() + (setsToAdd * CHARGES_PER_SET));

        if (scytheSlot >= 0 && scytheSlot < player.getInventory().capacity()) {
            Item slotItem = player.getInventory().getItems()[scytheSlot];
            if (slotItem != null && slotItem.getId() == scytheId && isUnchargedVariant(scytheId)) {
                player.getInventory().setItem(scytheSlot, new Item(toChargedVariant(scytheId)));
            }
        }

        player.getInventory().refreshItems();
        player.getPacketSender().sendMessage("You charge your scythe to " + player.getScytheCharges() + "/" + MAX_CHARGES + ".");
        return true;
    }

    private static void setEquippedWeapon(Player player, int weaponId) {
        player.getEquipment().setItem(Equipment.WEAPON_SLOT, new Item(weaponId));
        player.getEquipment().refreshItems();
        BonusManager.update(player);
        WeaponInterfaces.assign(player);
    }
}

