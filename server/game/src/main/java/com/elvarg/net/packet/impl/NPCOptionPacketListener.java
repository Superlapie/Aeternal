package com.elvarg.net.packet.impl;

import com.elvarg.Server;
import com.elvarg.game.content.bosses.nightmare.NightmareEncounter;
import com.elvarg.game.World;
import com.elvarg.game.content.PetHandler;
import com.elvarg.game.content.combat.CombatFactory;
import com.elvarg.game.content.combat.magic.CombatSpell;
import com.elvarg.game.content.combat.magic.CombatSpells;
import com.elvarg.game.content.combat.method.CombatMethod;
import com.elvarg.game.content.quests.QuestHandler;
import com.elvarg.game.content.skill.skillable.impl.Fishing;
import com.elvarg.game.content.skill.skillable.impl.Fishing.FishingTool;
import com.elvarg.game.content.skill.skillable.impl.Thieving.Pickpocketing;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.impl.Barricades;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.container.shop.ShopManager;
import com.elvarg.game.model.dialogues.builders.impl.EmblemTraderDialogue;
import com.elvarg.game.content.skill.slayer.SlayerDialogue;
import com.elvarg.game.content.skill.slayer.SlayerMaster;
import com.elvarg.game.content.skill.slayer.SlayerRewards;
import com.elvarg.game.content.skill.slayer.Slayer;
import com.elvarg.game.model.dialogues.builders.impl.ParduDialogue;
import com.elvarg.game.model.PlayerStatus;
import com.elvarg.game.model.rights.PlayerRights;
import com.elvarg.game.entity.impl.npc.NPCInteractionSystem;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.impl.NightmareArea;
import com.elvarg.game.task.impl.WalkToTask;
import com.elvarg.net.packet.Packet;
import com.elvarg.net.packet.PacketConstants;
import com.elvarg.net.packet.PacketExecutor;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.util.ShopIdentifiers;

public class NPCOptionPacketListener extends NpcIdentifiers implements PacketExecutor {

    private static final int SANCTUARY_NIGHTMARE_ID = 9433;
    private static final Location SANCTUARY_NIGHTMARE_LOCATION = NightmareArea.SANCTUARY_WAKE_TILE;

    private static boolean isNightmareNpc(NPC npc) {
        int id = npc.getId();
        if (id >= 9425 && id <= 9433) {
            return true;
        }
        String currentName = npc.getCurrentDefinition() != null ? npc.getCurrentDefinition().getName() : null;
        if (currentName != null && currentName.toLowerCase().contains("nightmare")) {
            return true;
        }
        String baseName = npc.getDefinition() != null ? npc.getDefinition().getName() : null;
        return baseName != null && baseName.toLowerCase().contains("nightmare");
    }

    private static boolean isSanctuaryNightmareTrigger(NPC npc) {
        if (npc.getId() != SANCTUARY_NIGHTMARE_ID || npc.getPrivateArea() != null) {
            return false;
        }
        Location loc = npc.getLocation();
        return loc.getX() == SANCTUARY_NIGHTMARE_LOCATION.getX()
                && loc.getY() == SANCTUARY_NIGHTMARE_LOCATION.getY()
                && loc.getZ() == SANCTUARY_NIGHTMARE_LOCATION.getZ();
    }

    private static String formatCanAttackReason(CombatFactory.CanAttackResponse response) {
        return switch (response) {
            case CAN_ATTACK -> "CAN_ATTACK";
            case CANT_ATTACK_IN_AREA -> "CANT_ATTACK_IN_AREA";
            case ALREADY_UNDER_ATTACK -> "ALREADY_UNDER_ATTACK";
            case INVALID_TARGET -> "INVALID_TARGET";
            case COMBAT_METHOD_NOT_ALLOWED -> "COMBAT_METHOD_NOT_ALLOWED";
            case LEVEL_DIFFERENCE_TOO_GREAT -> "LEVEL_DIFFERENCE_TOO_GREAT";
            case NOT_ENOUGH_SPECIAL_ENERGY -> "NOT_ENOUGH_SPECIAL_ENERGY";
            case STUNNED -> "STUNNED";
            case DUEL_NOT_STARTED_YET -> "DUEL_NOT_STARTED_YET";
            case DUEL_MELEE_DISABLED -> "DUEL_MELEE_DISABLED";
            case DUEL_RANGED_DISABLED -> "DUEL_RANGED_DISABLED";
            case DUEL_MAGIC_DISABLED -> "DUEL_MAGIC_DISABLED";
            case DUEL_WRONG_OPPONENT -> "DUEL_WRONG_OPPONENT";
            case TARGET_IS_IMMUNE -> "TARGET_IS_IMMUNE";
            case CASTLE_WARS_FRIENDLY_FIRE -> "CASTLE_WARS_FRIENDLY_FIRE";
            case SLAYER_EQUIPMENT_MISSING -> "SLAYER_EQUIPMENT_MISSING";
        };
    }

    @Override
    public void execute(Player player, Packet packet) {
        int index = packet.readLEShortA();

        if (index < 0 || index > World.getNpcs().capacity()) {
            return;
        }

        final NPC npc = World.getNpcs().get(index);

        if (npc == null) {
            return;
        }

        if (!player.getLocation().isWithinDistance(npc.getLocation(), 24)) {
            return;
        }

        boolean nightmareNpc = isNightmareNpc(npc);

        if (player.busy()) {
            if (nightmareNpc) {
                // Do not block Nightmare attack interactions on stale player state.
                player.getPacketSender().sendInterfaceRemoval();
            } else {
            // Nightmare can be entered via the teleport UI path. If that interface was not
            // dismissed yet, allow this interaction once and clear it.
            boolean nightmareInterfaceOnlyBusy =
                    nightmareNpc
                            && player.getInterfaceId() > 0
                            && player.getStatus() == PlayerStatus.NONE
                            && !player.isTeleporting()
                            && !player.isNeedsPlacement()
                            && player.getForceMovement() == null
                            && player.getHitpoints() > 0;
            if (!nightmareInterfaceOnlyBusy) {
                return;
            }
            player.getPacketSender().sendInterfaceRemoval();
            }
        }

        if (player.getRights() == PlayerRights.DEVELOPER) {
            player.getPacketSender().sendMessage("InteractionInfo Id=" + npc.getId()+" "+npc.getLocation().toString());
        }

        player.setPositionToFace(npc.getLocation());

        if (isSanctuaryNightmareTrigger(npc)) {
            WalkToTask.submit(player, npc, () -> NightmareEncounter.enter(player, npc.getLocation().clone()));
            return;
        }

        // Nightmare can be interacted with through mixed opcodes depending on client menu wiring.
        // Start combat immediately, then retry once pathing settles.
        if (nightmareNpc && npc.getHitpoints() > 0) {
            // Ensure stale magic/autocast state does not block combat clicks.
            player.getCombat().setCastSpell(null);
            player.getCombat().setAutocastSpell(null);

            CombatMethod method = CombatFactory.getMethod(player);
            CombatFactory.CanAttackResponse response = CombatFactory.canAttack(player, method, npc);
            if (response == CombatFactory.CanAttackResponse.COMBAT_METHOD_NOT_ALLOWED) {
                method = CombatFactory.getMethod(player);
                response = CombatFactory.canAttack(player, method, npc);
            }

            player.getPacketSender().sendMessage("NightmareAttackDebug opcode=" + packet.getOpcode()
                    + " npcId=" + npc.getId()
                    + " response=" + formatCanAttackReason(response)
                    + " dist=" + player.calculateDistance(npc)
                    + " pSize=" + player.size()
                    + " nSize=" + npc.size()
                    + " samePrivateArea=" + (player.getPrivateArea() == npc.getPrivateArea())
                    + " playerArea=" + (player.getArea() == null ? "null" : player.getArea().getName())
                    + " npcArea=" + (npc.getArea() == null ? "null" : npc.getArea().getName()));

            player.getCombat().attack(npc);
            WalkToTask.submit(player, npc, () -> player.getCombat().attack(npc));
            return;
        }

        if (packet.getOpcode() == PacketConstants.ATTACK_NPC_OPCODE || packet.getOpcode() == PacketConstants.MAGE_NPC_OPCODE) {
            if (!npc.getCurrentDefinition().isAttackable()) {
                return;
            }
            if (npc.getHitpoints() <= 0) {
                player.getMovementQueue().reset();
                return;
            }

            if (packet.getOpcode() == PacketConstants.MAGE_NPC_OPCODE) {

                int spellId = packet.readShortA();

                CombatSpell spell = CombatSpells.getCombatSpell(spellId);

                if (spell == null) {
                    player.getMovementQueue().reset();
                    return;
                }

                player.setPositionToFace(npc.getLocation());

                player.getCombat().setCastSpell(spell);
            }

            player.getCombat().attack(npc);
            return;
        }

        WalkToTask.submit(player, npc, () -> handleInteraction(player, npc, packet));
    }

    private void handleInteraction(Player player, NPC npc, Packet packet) {

        final int opcode = packet.getOpcode();

        npc.setMobileInteraction(player);

        npc.setPositionToFace(player.getLocation());

        if (opcode == PacketConstants.FIRST_CLICK_NPC_OPCODE) {
            if (PetHandler.interact(player, npc)) {
				// Player was interacting with their pet
                return;
            }

			if (QuestHandler.firstClickNpc(player, npc)) {
				// NPC Click was handled by a quest
				return;
			}

			if (NPCInteractionSystem.handleFirstOption(player, npc)) {
				// Player is interacting with a defined NPC
				return;
			}

            switch (npc.getId()) {
                case SHOP_KEEPER_4:
                    ShopManager.open(player, ShopIdentifiers.GENERAL_STORE);
                    break;
                case CHARLIE_THE_COOK:
                    ShopManager.open(player, ShopIdentifiers.FOOD_SHOP);
                    break;
                case RICK:
                    ShopManager.open(player, ShopIdentifiers.PURE_SHOP);
                    break;
                case AJJAT:
                    ShopManager.open(player, ShopIdentifiers.ARMOR_SHOP);
                    break;
                case MAGIC_INSTRUCTOR:
                    ShopManager.open(player, ShopIdentifiers.MAGE_ARMOR_SHOP);
                    break;
                case ARMOUR_SALESMAN:
                    ShopManager.open(player, ShopIdentifiers.RANGE_SHOP);
                    break;
                case BANKER_2:
                case TZHAAR_KET_ZUH:
                    player.getBank(player.getCurrentBankTab()).open();
                    break;
                case MAKE_OVER_MAGE:
                    player.getPacketSender().sendInterfaceRemoval().sendInterface(3559);
                    player.getAppearance().setCanChangeAppearance(true);
                    break;
                case SECURITY_GUARD:
                    //DialogueManager.start(player, 2500);
                    break;
                case EMBLEM_TRADER:
                case EMBLEM_TRADER_2:
                case EMBLEM_TRADER_3:
                    player.getDialogueManager().start(new EmblemTraderDialogue());
                    break;

                case PERDU:
                    player.getDialogueManager().start(new ParduDialogue());
                    break;

                case FINANCIAL_ADVISOR:
                    //DialogueManager.start(player, 15);
                    // Removed
                    break;
                case NIEVE:
                case 6798: // Steve
                case 401: // Turael
                case 402: // Mazchna
                case 403: // Vannaka
                case 404: // Chaeldar
                case 7663: // Krystilia
                case 8623: // Konar
                case 9085: // Kuradal
                    SlayerMaster master = null;
                    for (SlayerMaster m : SlayerMaster.MASTERS) {
                        if (m.getNpcId() == npc.getId()) {
                            master = m;
                            break;
                        }
                    }
                    if (master != null) {
                        player.getDialogueManager().start(new SlayerDialogue(master));
                    }
                    break;
            }
            return;
        }


        if (opcode == PacketConstants.SECOND_CLICK_NPC_OPCODE) {
			if (PetHandler.pickup(player, npc)) {
				// Player is picking up their pet
				return;
			}

			if (Pickpocketing.init(player, npc)) {
				// Player is trying to thieve from an NPC
				return;
			}

			if (NPCInteractionSystem.handleSecondOption(player, npc)) {
				// Player is interacting with a defined NPC
				return;
			}

            switch (npc.getId()) {
                case NIEVE:
                case 6798:
                case 401:
                case 402:
                case 403:
                case 404:
                case 7663:
                case 8623:
                case 9085:
                    SlayerMaster master = null;
                    for (SlayerMaster m : SlayerMaster.MASTERS) {
                        if (m.getNpcId() == npc.getId()) {
                            master = m;
                            break;
                        }
                    }
                    if (master != null) {
                        if (player.getSlayerTask() != null) {
                            player.getPacketSender().sendMessage("You already have a Slayer task: " + player.getSlayerTask().getTask().toString());
                        } else {
                            if (Slayer.assign(player, master)) {
                                player.getPacketSender().sendMessage("You have been assigned to kill " + player.getSlayerTask().getRemaining() + " " + player.getSlayerTask().getTask().toString() + ".");
                            }
                        }
                    }
                    break;
                case BANKER:
                case BANKER_2:
                case BANKER_3:
                case BANKER_4:
                case BANKER_5:
                case BANKER_6:
                case BANKER_7:
                case TZHAAR_KET_ZUH:
                    player.getBank(player.getCurrentBankTab()).open();
                    break;
                case 1497: // Net and bait
                case 1498: // Net and bait
                    player.getSkillManager().startSkillable(new Fishing(npc, FishingTool.FISHING_ROD));
                    break;
                case RICHARD_2:
                    ShopManager.open(player, ShopIdentifiers.TEAMCAPE_SHOP);
                    break;
                case EMBLEM_TRADER:
                case EMBLEM_TRADER_2:
                case EMBLEM_TRADER_3:
                    ShopManager.open(player, ShopIdentifiers.PVP_SHOP);
                    break;
                case MAGIC_INSTRUCTOR:
                    ShopManager.open(player, ShopIdentifiers.MAGE_ARMOR_SHOP);
                    break;
                case SQUIRE_8:
                    ShopManager.open(player, ShopIdentifiers.VOID_MAGIC_SHOP);
                    break;
                case SQUIRE_6:
                    ShopManager.open(player, ShopIdentifiers.VOID_RANGED_SHOP);
                    break;

            }
            return;
        }

        if (opcode == PacketConstants.THIRD_CLICK_NPC_OPCODE) {
            if (PetHandler.morph(player, npc)) {
				// Player is morphing their pet
                return;
            }

			if (NPCInteractionSystem.handleThirdOption(player, npc)) {
				// Player is interacting with a defined NPC
				return;
			}

            switch (npc.getId()) {
                case NIEVE:
                case 6798:
                case 401:
                case 402:
                case 403:
                case 404:
                case 7663:
                case 8623:
                case 9085:
                    SlayerRewards.openRewards(player, npc.getId());
                    break;
                case EMBLEM_TRADER:
                    player.getDialogueManager().start(new EmblemTraderDialogue(), 2);
                    break;
                case MAGIC_INSTRUCTOR:
                    ShopManager.open(player, ShopIdentifiers.MAGE_RUNES_SHOP);
                    break;
            }
            return;
        }

        if (opcode == PacketConstants.FOURTH_CLICK_NPC_OPCODE) {
			if (NPCInteractionSystem.handleForthOption(player, npc)) {
				// Player is interacting with a defined NPC
				return;
			}

            switch (npc.getId()) {
                case NIEVE:
                case 6798:
                case 401:
                case 402:
                case 403:
                case 404:
                case 7663:
                case 8623:
                case 9085:
                    SlayerRewards.openRewards(player, npc.getId());
                    break;
                case EMBLEM_TRADER:
                    player.getDialogueManager().start(new EmblemTraderDialogue(), 5);
                    break;
            }
            return;
        }
    }
}
