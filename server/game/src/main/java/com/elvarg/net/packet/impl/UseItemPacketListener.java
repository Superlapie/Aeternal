package com.elvarg.net.packet.impl;

import com.elvarg.game.World;
import com.elvarg.game.content.cannon.DwarfCannon;
import com.elvarg.game.content.combat.BlowpipeData;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.ScytheData;
import com.elvarg.game.content.minigames.impl.CastleWars;
import com.elvarg.game.content.skill.impl.smithing.AnvilSmithing;
import com.elvarg.game.content.skill.impl.smithing.Smelting;
import com.elvarg.game.content.skill.impl.smithing.SmeltingData;
import com.elvarg.game.content.skill.impl.hunter.Birdhouses;
import com.elvarg.game.content.skill.skillable.impl.Cooking;
import com.elvarg.game.content.skill.skillable.impl.Firemaking;
import com.elvarg.game.content.skill.skillable.impl.Fletching;
import com.elvarg.game.content.skill.skillable.impl.Herblore;
import com.elvarg.game.content.skill.skillable.impl.Crafting;
import com.elvarg.game.content.TormentedSynapseCrafting;
import com.elvarg.game.content.skill.skillable.impl.Cooking.Cookable;
import com.elvarg.game.content.skill.skillable.impl.Firemaking.LightableLog;
import com.elvarg.game.content.skill.skillable.impl.Prayer.AltarOffering;
import com.elvarg.game.content.skill.skillable.impl.Prayer.BuriableBone;
import com.elvarg.game.entity.impl.grounditem.ItemOnGround;
import com.elvarg.game.entity.impl.grounditem.ItemOnGroundManager;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.NPCInteractionSystem;
import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.object.MapObjects;
import com.elvarg.game.entity.impl.object.impl.WebHandler;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.container.impl.Bank;
import com.elvarg.game.model.equipment.BonusManager;
import com.elvarg.game.model.menu.CreationMenu;
import com.elvarg.game.definition.ObjectDefinition;
import com.elvarg.game.task.impl.WalkToTask;
import com.elvarg.net.packet.Packet;
import com.elvarg.net.packet.PacketConstants;
import com.elvarg.net.packet.PacketExecutor;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.ObjectIdentifiers;

import java.util.Arrays;
import java.util.Optional;


public class UseItemPacketListener extends ItemIdentifiers implements PacketExecutor {

    private static void itemOnItem(Player player, Packet packet) {
        int usedWithSlot = packet.readUnsignedShort();
        int itemUsedSlot = packet.readUnsignedShortA();
        if (usedWithSlot < 0 || itemUsedSlot < 0 || itemUsedSlot >= player.getInventory().capacity() || usedWithSlot >= player.getInventory().capacity())
            return;
        Item used = player.getInventory().getItems()[itemUsedSlot];
        Item usedWith = player.getInventory().getItems()[usedWithSlot];

        player.getPacketSender().sendInterfaceRemoval();
        player.getSkillManager().stopSkillable();

        if (Birdhouses.handleItemOnItem(player, used.getId(), usedWith.getId())) {
            return;
        }

        //Herblore
        if (Herblore.makeUnfinishedPotion(player, used.getId(), usedWith.getId())
                || Herblore.finishPotion(player, used.getId(), usedWith.getId())
                || Herblore.concatenate(player, used, usedWith)) {
            return;
        }

        //Fletching
        if (Fletching.fletchLog(player, used.getId(), usedWith.getId())
                || Fletching.stringBow(player, used.getId(), usedWith.getId())
                || Fletching.fletchAmmo(player, used.getId(), usedWith.getId())
                || Fletching.fletchCrossbow(player, used.getId(), usedWith.getId())) {
            return;
        }

        //Crafting
        if (Crafting.craftGem(player, used.getId(), usedWith.getId())) {
            return;
        }

        if (TormentedSynapseCrafting.handleItemOnItem(player, used.getId(), usedWith.getId())) {
            return;
        }

        //Firemaking
        if (Firemaking.init(player, used.getId(), usedWith.getId())) {
            return;
        }

        int scytheSlot = -1;
        if (ScytheData.isScythe(used.getId())) {
            scytheSlot = itemUsedSlot;
        } else if (ScytheData.isScythe(usedWith.getId())) {
            scytheSlot = usedWithSlot;
        }
        if (ScytheData.handleItemOnItemCharge(player, used.getId(), usedWith.getId(), scytheSlot)) {
            return;
        }

        //Granite clamp on Granite maul
        if ((used.getId() == GRANITE_CLAMP || usedWith.getId() == GRANITE_CLAMP)
                && (used.getId() == GRANITE_MAUL || usedWith.getId() == GRANITE_MAUL)) {
            if (player.busy() || CombatFactory.inCombat(player)) {
                player.getPacketSender().sendMessage("You cannot do that right now.");
                return;
            }
            if (player.getInventory().contains(GRANITE_MAUL)) {
                player.getInventory().delete(GRANITE_MAUL, 1).delete(GRANITE_CLAMP, 1).add(GRANITE_MAUL_3, 1);
                player.getPacketSender().sendMessage("You attach your Granite clamp onto the maul..");
            }
            return;
        }

        //Hilt on dragon defender
        if ((used.getId() == DRAGON_DEFENDER || usedWith.getId() == DRAGON_DEFENDER)
                && (used.getId() == AVERNIC_DEFENDER_HILT || usedWith.getId() == AVERNIC_DEFENDER_HILT)) {
            if (player.busy() || CombatFactory.inCombat(player)) {
                player.getPacketSender().sendMessage("You cannot do that right now.");
                return;
            }
            if (player.getInventory().contains(DRAGON_DEFENDER) && player.getInventory().contains(AVERNIC_DEFENDER_HILT)) {
                player.getInventory().delete(DRAGON_DEFENDER, 1).delete(AVERNIC_DEFENDER_HILT, 1).add(AVERNIC_DEFENDER, 1);
                player.getPacketSender().sendMessage("You attach your Avernic hilt onto the Dragon defender..");
            }
            return;
        }

        if (BlowpipeData.handleItemOnItemLoad(player, used.getId(), usedWith.getId(),
                BlowpipeData.isBlowpipe(used.getId()) ? itemUsedSlot : usedWithSlot)) {
            BlowpipeData.syncWeaponVariant(player);
            BonusManager.update(player);
            return;
        }
    }

    private static void itemOnNpc(final Player player, Packet packet) {
        final int id = packet.readShortA();
        final int index = packet.readShortA();
        final int slot = packet.readLEShort();

        if (index < 0 || index > World.getNpcs().capacity()) {
            return;
        }

        if (slot < 0 || slot > player.getInventory().getItems().length) {
            return;
        }

        NPC npc = World.getNpcs().get(index);
        if (npc == null) {
            return;
        }
        Item item = player.getInventory().getItems()[slot];
        if (item == null || item.getId() != id) {
            return;
        }

        WalkToTask.submit(player, npc, () -> {
            if (NPCInteractionSystem.handleUseItem(player, npc, id, slot)) {
                // Player is using an item on a defined NPC
                return;
            }

            switch (id) {
                default:
                    player.getPacketSender().sendMessage("Nothing interesting happens.");
                    break;
            }
        });
    }

    @SuppressWarnings("unused")
    private static void itemOnObject(Player player, Packet packet) {
        int interfaceType = packet.readShort();
        final int objectId = packet.readShort();
        final int objectY = packet.readLEShortA();
        final int itemSlot = packet.readLEShort();
        final int objectX = packet.readLEShortA();
        final int itemId = packet.readShort();

        if (itemSlot < 0 || itemSlot >= player.getInventory().capacity())
            return;

        final Item item = player.getInventory().getItems()[itemSlot];

        if (item == null || item.getId() != itemId)
            return;

        final Location position = new Location(objectX, objectY, player.getLocation().getZ());

        GameObject object = MapObjects.get(player, objectId, position);
        if (object == null) {
            ObjectDefinition clickedDef = ObjectDefinition.forId(objectId);
            if (isSpinningWheelObjectId(objectId) || isSpinningWheelDefinition(clickedDef)) {
                object = new GameObject(objectId, position, 10, 0, player.getPrivateArea());
            }
        }

        // Make sure the object actually exists in the region...
        if (object == null) {
            return;
        }

        final GameObject resolvedObject = object;

        player.setPositionToFace(position);

        WalkToTask.submit(player, resolvedObject, () -> {
            if (Birdhouses.handleItemOnObject(player, item, resolvedObject)) {
                return;
            }
            switch (resolvedObject.getId()) {
                case 6: {
                    if (DwarfCannon.isObject(resolvedObject)) {
                        player.getDwarfCannon().handleCannonBallOnCannon(resolvedObject, item);
                        return;
                    }
                }
                case ObjectIdentifiers.STOVE_4: //Edgeville Stove
                case ObjectIdentifiers.FIRE_5: //Player-made Fire
                case ObjectIdentifiers.FIRE_23: //Barb village fire
                    //Handle cooking on objects..
                    Optional<Cookable> cookable = Cookable.getForItem(item.getId());
                    if (cookable.isPresent()) {
                        player.getPacketSender().sendCreationMenu(new CreationMenu("How many would you like to cook?", Arrays.asList(cookable.get().getCookedItem()), (productId, amount) -> {
                            player.getSkillManager().startSkillable(new Cooking(resolvedObject, cookable.get(), amount));
                        }));
                        return;
                    }
                    //Handle bonfires..
                    if (resolvedObject.getId() == ObjectIdentifiers.FIRE_5) {
                        Optional<LightableLog> log = LightableLog.getForItem(item.getId());
                        if (log.isPresent()) {
                            player.getPacketSender().sendCreationMenu(new CreationMenu("How many would you like to burn?", Arrays.asList(log.get().getLogId()), (productId, amount) -> {
                                player.getSkillManager().startSkillable(new Firemaking(log.get(), resolvedObject, amount));
                            }));
                            return;
                        }
                    }
                    break;
                case ObjectIdentifiers.WEB:
                    if (!WebHandler.isSharpItem(item)) {
                        player.sendMessage("Only a sharp blade can cut through this sticky web.");
                        return;
                    }
                    WebHandler.handleSlashWeb(player, resolvedObject, true);
                    break;
                case 409: //Bone on Altar
                    Optional<BuriableBone> b = BuriableBone.forId(item.getId());
                    if (b.isPresent()) {
                        player.getPacketSender().sendCreationMenu(new CreationMenu("How many would you like to offer?", Arrays.asList(itemId), (productId, amount) -> {
                            player.getSkillManager().startSkillable(new AltarOffering(b.get(), resolvedObject, amount));
                        }));
                    }
                    break;
                default:
                    player.getPacketSender().sendMessage("Nothing interesting happens.");
                    break;
            }
            if (Bank.useItemOnDepositBox(player, item, itemSlot, resolvedObject)) {
                return;
            }

            if (CastleWars.handleItemOnObject(player, item, resolvedObject)) {
                return;
            }
            
            if (isFurnaceObject(resolvedObject.getId()) && item.getId() == ItemIdentifiers.STEEL_BAR && player.getInventory().contains(ItemIdentifiers.AMMO_MOULD)) {
                player.getPacketSender().sendCreationMenu(new CreationMenu("How many would you like to make?", Arrays.asList(ItemIdentifiers.CANNONBALL), (productId, amount) -> {
                    Smelting.startSmelting(player, SmeltingData.CANNONBALL, amount);
                }));
                return;
            }

            if (TormentedSynapseCrafting.handleItemOnObject(player, item, resolvedObject.getId())) {
                return;
            }

            // Handle bar on anvil for smithing
            if (isAnvilObject(resolvedObject.getId())) {
                SmeltingData barData = SmeltingData.forBarId(item.getId());
                if (barData != null) {
                    AnvilSmithing.openSmithingInterface(player);
                }
                return;
            }

            // Explicit support: flax on spinning wheel -> bow string creation menu.
            if (item.getId() == ItemIdentifiers.FLAX
                    && (isSpinningWheelObjectId(resolvedObject.getId()) || isSpinningWheelDefinition(resolvedObject.getDefinition()))) {
                Crafting.spinFlax(player);
                return;
            }
        });
    }

    private static boolean isSpinningWheelObjectId(int objectId) {
        return objectId == ObjectIdentifiers.SPINNING_WHEEL
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_2
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_3
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_4
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_5
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_6
                || objectId == ObjectIdentifiers.SPINNING_WHEEL_7
                || objectId == ObjectIdentifiers.SPINNING_MACHINE;
    }

    private static boolean isSpinningWheelDefinition(ObjectDefinition defs) {
        if (defs == null || defs.getName() == null) {
            return false;
        }
        return defs.getName().toLowerCase().contains("spinning wheel");
    }

    private static boolean isAnvilObject(int objectId) {
        return objectId == ObjectIdentifiers.ANVIL
                || objectId == ObjectIdentifiers.ANVIL_2
                || objectId == ObjectIdentifiers.ANVIL_3
                || objectId == ObjectIdentifiers.ANVIL_4
                || objectId == ObjectIdentifiers.ANVIL_5
                || objectId == ObjectIdentifiers.ANVIL_6;
    }

    private static boolean isFurnaceObject(int objectId) {
        switch (objectId) {
            case ObjectIdentifiers.FURNACE_18:
            case ObjectIdentifiers.FURNACE:
            case ObjectIdentifiers.FURNACE_2:
            case ObjectIdentifiers.FURNACE_3:
            case ObjectIdentifiers.FURNACE_4:
            case ObjectIdentifiers.FURNACE_5:
            case ObjectIdentifiers.FURNACE_6:
            case ObjectIdentifiers.FURNACE_7:
            case ObjectIdentifiers.FURNACE_8:
            case ObjectIdentifiers.FURNACE_9:
            case ObjectIdentifiers.FURNACE_10:
            case ObjectIdentifiers.FURNACE_11:
            case ObjectIdentifiers.FURNACE_12:
            case ObjectIdentifiers.FURNACE_13:
            case ObjectIdentifiers.FURNACE_14:
            case ObjectIdentifiers.FURNACE_15:
            case ObjectIdentifiers.FURNACE_16:
            case ObjectIdentifiers.FURNACE_17:
            case ObjectIdentifiers.FURNACE_19:
            case ObjectIdentifiers.FURNACE_20:
            case ObjectIdentifiers.SMALL_FURNACE:
            case ObjectIdentifiers.SMALL_FURNACE_2:
            case ObjectIdentifiers.BROKEN_FURNACE:
            case ObjectIdentifiers.REPAIRED_FURNACE:
            case ObjectIdentifiers.REPAIRED_FURNACE_2:
            case ObjectIdentifiers.CHARCOAL_FURNACE:
            case ObjectIdentifiers.CHARCOAL_FURNACE_2:
            case ObjectIdentifiers.CHARCOAL_FURNACE_3:
            case ObjectIdentifiers.CHARCOAL_FURNACE_4:
            case ObjectIdentifiers.LOVAKITE_FURNACE:
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("unused")
    private static void itemOnPlayer(Player player, Packet packet) {
        int interfaceId = packet.readUnsignedShortA();
        int targetIndex = packet.readUnsignedShort();
        int itemId = packet.readUnsignedShort();
        int slot = packet.readLEShort();
        if (slot < 0 || slot >= player.getInventory().capacity() || targetIndex >= World.getPlayers().capacity())
            return;
        Player target = World.getPlayers().get(targetIndex);
        if (target == null) {
            return;
        }
        Item item = player.getInventory().get(slot);

        if (item == null || !player.getInventory().contains(itemId)) {
            return;
        }

        WalkToTask.submit(player, target, () -> {
            CastleWars.handleItemOnPlayer(player, target, item);
        });
    }

    @SuppressWarnings("unused")
    private static void itemOnGroundItem(Player player, Packet packet) {
        int interfaceId = packet.readLEShort();
        int inventory_item = packet.readShortA();
        int ground_item_id = packet.readShort();
        int y = packet.readShortA();
        int unknown = packet.readLEShortA();
        int x = packet.readShort();
        //Verify item..
        if (!player.getInventory().contains(inventory_item)) {
            return;
        }

        //Verify ground item..
        Optional<ItemOnGround> groundItem = ItemOnGroundManager.getGroundItem(Optional.of(player.getUsername()), ground_item_id, new Location(x, y));
        if (!groundItem.isPresent()) {
            return;
        }

        Location item_position = groundItem.get().getLocation();

        WalkToTask.submit(player, groundItem.get(), () -> {
            player.setPositionToFace(item_position);

            switch (inventory_item) {
                case TINDERBOX:
                    Optional<LightableLog> log = LightableLog.getForItem(ground_item_id);
                    if (log.isPresent()) {
                        player.getSkillManager().startSkillable(new Firemaking(log.get(), groundItem.get()));
                        return;
                    }
                    break;
            }
        });
    }


    @Override
    public void execute(Player player, Packet packet) {
        if (player.getHitpoints() <= 0)
            return;
        switch (packet.getOpcode()) {
            case PacketConstants.ITEM_ON_ITEM:
                itemOnItem(player, packet);
                break;
            case PacketConstants.ITEM_ON_OBJECT:
                itemOnObject(player, packet);
                break;
            case PacketConstants.ITEM_ON_GROUND_ITEM:
                itemOnGroundItem(player, packet);
                break;
            case PacketConstants.ITEM_ON_NPC:
                itemOnNpc(player, packet);
                break;
            case PacketConstants.ITEM_ON_PLAYER:
                itemOnPlayer(player, packet);
                break;
        }
    }
}
