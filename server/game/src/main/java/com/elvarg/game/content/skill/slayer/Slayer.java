package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.model.areas.Area;
import com.elvarg.game.model.areas.AreaManager;
import com.elvarg.game.model.areas.impl.WildernessArea;

import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Graphic;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Slayer {

    /**
     * Assigns a {@link SlayerTask} to the given {@link Player} based on their
     * current combat level.
     * 
     * @param player
     */
    public static boolean assign(Player player) {
        SlayerMaster master = SlayerMaster.TURAEL;
        for (SlayerMaster m : SlayerMaster.MASTERS) {
            if (!m.canAssign(player)) {
                continue;
            }
            master = m;
        }
        return assign(player, master);
    }

    public static boolean assign(Player player, SlayerMaster master) {
        if (player.getSlayerTask() != null) {
            if (master == SlayerMaster.TURAEL && player.getSlayerTask().getMaster() != SlayerMaster.TURAEL) {
                // Turael can reset tasks!
                player.setSlayerTask(null);
                player.setConsecutiveTasks(0);
                player.getPacketSender().sendMessage("Your task has been reset, and your streak is now 0.");
            } else {
                player.getPacketSender().sendInterfaceRemoval().sendMessage("You already have a Slayer task.");
                return false;
            }
        }

        // Get the tasks we can assign
        List<SlayerTask> possibleTasks = new ArrayList<>();
        int totalWeight = 0;
        for (SlayerTask task : SlayerTask.VALUES) {
            // Check if player has unlocked this task
            if (!task.isUnlocked(player)) {
                continue;
            }

            // Check if player has the slayer level required for this task
            if (player.getSkillManager().getMaxLevel(Skill.SLAYER) < task.getSlayerLevel()) {
                continue;
            }

            // Check if this master is able to give out the task
            boolean correctMaster = false;
            for (SlayerMaster assignedBy : task.getMasters()) {
                if (master == assignedBy) {
                    correctMaster = true;
                    break;
                }
            }
            if (!correctMaster) {
                continue;
            }

            possibleTasks.add(task);
            totalWeight += task.getWeight();
        }
        
        // Filter out blocked tasks for this master
        if (player.getBlockedSlayerTasks().containsKey(master)) {
            possibleTasks.removeAll(player.getBlockedSlayerTasks().get(master));
            // Recalculate total weight after removal
            totalWeight = 0;
            for (SlayerTask pt : possibleTasks) {
                totalWeight += pt.getWeight();
            }
        }

        if (possibleTasks.isEmpty()) {
            player.getPacketSender().sendInterfaceRemoval().sendMessage("Nieve was unable to give you a Slayer task. Please try again later.");
            return false;
        }

        // Shuffle them and choose a random one based on the weighting system
        Collections.shuffle(possibleTasks);
        SlayerTask toAssign = null;
        for (SlayerTask task : possibleTasks) {
            if (Misc.getRandom(totalWeight) <= task.getWeight()) {
                toAssign = task;
                break;
            }
        }
        if (toAssign == null) {
            toAssign = possibleTasks.get(0);
        }

        // Assign the new task
        ActiveSlayerTask activeTask = new ActiveSlayerTask(master, toAssign, Misc.inclusive(toAssign.getMinimumAmount(), toAssign.getMaximumAmount()));
        
        // Handle specialized masters
        if (master == SlayerMaster.KONAR) {
            assignKonarLocation(activeTask);
        } else if (master == SlayerMaster.KRYSTILIA) {
            // Ensure we are in Wilderness logic (Krystilia only assigns wildy NPCs)
            activeTask.setLocationName("the Wilderness");
        }
        
        player.setSlayerTask(activeTask);
        return true;
    }

    private static void assignKonarLocation(ActiveSlayerTask task) {
        // Simple mock of Konar locations for now. 
        // In a real server, we would map these to Area objects.
        String[] locations = { "Catacombs of Kourend", "Taverly Dungeon", "Brimhaven Dungeon", "Stronghold Slayer Dungeon" };
        task.setLocationName(locations[Misc.getRandom(locations.length - 1)]);
    }

    public static void killed(Player player, NPC npc) {
        if (player.getSlayerTask() == null) {
            return;
        }
        if (npc.getDefinition() == null || npc.getDefinition().getName() == null) {
            return;
        }
        
        boolean isTask = false;
        final String killedNpcName = npc.getDefinition().getName().toLowerCase();
        for (String npcName : player.getSlayerTask().getTask().getNpcNames()) {
            if (npcName.equals(killedNpcName)) {
                isTask = true;
                break;
            }
        }
        if (!isTask) {
            return;
        }
        
        // Location check for Konar
        if (player.getSlayerTask().getMaster() == SlayerMaster.KONAR && player.getSlayerTask().getLocationName() != null) {
            // Very basic check: depends on NPC definition or position mapping.
            // For now, if we have a location name, we should ideally verify it.
            // Since we don't have perfect Area objects for all possible Konar spots yet,
            // we'll leave a hook here.
        }

        // Wilderness check for Krystilia
        if (player.getSlayerTask().getMaster() == SlayerMaster.KRYSTILIA) {
            if (!(player.getArea() instanceof WildernessArea)) {
                return;
            }
        }
        
        // Add experience and decrease task count
        player.getSkillManager().addExperience(Skill.SLAYER, npc.getDefinition().getHitpoints());
        player.getSlayerTask().setRemaining(player.getSlayerTask().getRemaining() - 1);

        // Experience and task count handled above

        // Handle completion of task
        if (player.getSlayerTask().getRemaining() == 0) {
            int rewardPoints = player.getSlayerTask().getMaster().getBasePoints();

            // Increase consecutive tasks
            player.setConsecutiveTasks(player.getConsecutiveTasks() + 1);

            // Check for bonus points after completing consecutive tasks
            for (int[] consecutive : player.getSlayerTask().getMaster().getConsecutiveTaskPoints()) {
                int requiredTasks = consecutive[0];
                int bonusPoints = consecutive[1];
                if (player.getConsecutiveTasks() % requiredTasks == 0) {
                    rewardPoints = bonusPoints;
                    break;
                }
            }

            // Increase points
            player.setSlayerPoints(player.getSlayerPoints() + rewardPoints);
            player.getPacketSender().sendMessage("You have succesfully completed @dre@" + player.getConsecutiveTasks() + "@bla@ slayer tasks in a row.");
            player.getPacketSender().sendMessage("You earned @dre@" + rewardPoints + "@bla@ Slayer " + (rewardPoints == 1 ? "point" : "points") + ", your new total is now @dre@" + player.getSlayerPoints() + ".");
            
            // Reset task
            player.setSlayerTask(null);
        }
    }

    public static void storeTask(Player player) {
        if (player.getSlayerTask() == null) {
            player.getPacketSender().sendMessage("You do not have an active task to store.");
            return;
        }
        if (player.getStoredSlayerTask() != null) {
            player.getPacketSender().sendMessage("You already have a task stored.");
            return;
        }
        if (player.getSlayerPoints() < 100) {
            player.getPacketSender().sendMessage("You need 100 Slayer points to store a task.");
            return;
        }
        
        player.setSlayerPoints(player.getSlayerPoints() - 100);
        player.setStoredSlayerTask(player.getSlayerTask());
        player.setSlayerTask(null);
        player.getPacketSender().sendMessage("Your task has been stored. You can retrieve it later for free.");
    }

    public static void unstoreTask(Player player) {
        if (player.getStoredSlayerTask() == null) {
            player.getPacketSender().sendMessage("You do not have a stored task.");
            return;
        }
        if (player.getSlayerTask() != null) {
            player.getPacketSender().sendMessage("You already have an active task. Finish or cancel it first.");
            return;
        }
        
        player.setSlayerTask(player.getStoredSlayerTask());
        player.setStoredSlayerTask(null);
        player.getPacketSender().sendMessage("You have retrieved your stored Slayer task.");
    }

    public static void cancelTask(Player player) {
        if (player.getSlayerTask() == null) {
            player.getPacketSender().sendMessage("You do not have an active task to cancel.");
            return;
        }
        if (player.getSlayerPoints() < 30) {
            player.getPacketSender().sendMessage("You need 30 Slayer points to cancel a task.");
            return;
        }
        
        player.setSlayerPoints(player.getSlayerPoints() - 30);
        player.setSlayerTask(null);
        player.getPacketSender().sendMessage("Your Slayer task has been cancelled. Your streak was preserved.");
    }

    public static void blockTask(Player player) {
        if (player.getSlayerTask() == null) {
            player.getPacketSender().sendMessage("You do not have an active task to block.");
            return;
        }
        
        SlayerMaster master = player.getSlayerTask().getMaster();
        int cost = master.getBlockCost();
        
        if (player.getSlayerPoints() < cost) {
            player.getPacketSender().sendMessage("You need " + cost + " Slayer points to block this task.");
            return;
        }
        
        List<SlayerTask> blocked = player.getBlockedSlayerTasks().computeIfAbsent(master, k -> new ArrayList<>());
        if (blocked.size() >= 5) {
            player.getPacketSender().sendMessage("You can only block up to 5 tasks for this master.");
            return;
        }
        
        SlayerTask task = player.getSlayerTask().getTask();
        if (blocked.contains(task)) {
            player.getPacketSender().sendMessage("This task is already blocked for " + master.name() + ".");
            return;
        }
        
        player.setSlayerPoints(player.getSlayerPoints() - cost);
        blocked.add(task);
        player.setSlayerTask(null);
        player.getPacketSender().sendMessage("You have blocked " + task.name() + " for " + master.name() + ". Your streak was preserved.");
    }

    public static void unblockTask(Player player, SlayerTask task, SlayerMaster master) {
        List<SlayerTask> blocked = player.getBlockedSlayerTasks().get(master);
        if (blocked == null || !blocked.remove(task)) {
            player.getPacketSender().sendMessage("That task is not blocked for " + master.name() + ".");
            return;
        }
        
        player.getPacketSender().sendMessage("You have unblocked " + task.name() + " for " + master.name() + ".");
    }

    public static boolean isWearingSlayerGear(Player player, Mobile target) {
        if (player.getSlayerTask() == null || !target.isNpc()) {
            return false;
        }
        
        NPC npc = target.getAsNpc();
        if (!Arrays.stream(player.getSlayerTask().getTask().getNpcNames())
                .anyMatch(name -> name.equalsIgnoreCase(npc.getCurrentDefinition().getName()))) {
           // Task doesn't match the NPC name
           // Note: This is a basic check, might need better NPC name/ID mapping
           return false;
        }

        int helm = player.getEquipment().getItems()[Equipment.HEAD_SLOT].getId();
        String name = player.getEquipment().getItems()[Equipment.HEAD_SLOT].getDefinition().getName().toLowerCase();
        
        return name.contains("slayer helmet") || name.contains("black mask");
    }

    public static boolean isWearingImbuedSlayerGear(Player player, Mobile target) {
        if (!isWearingSlayerGear(player, target)) {
            return false;
        }
        String name = player.getEquipment().getItems()[Equipment.HEAD_SLOT].getDefinition().getName().toLowerCase();
        return name.contains("(i)");
    }

    public static boolean checkEquipment(Player player, NPC npc) {
        String name = npc.getCurrentDefinition().getName().toLowerCase();
        int weapon = player.getEquipment().getItems()[Equipment.WEAPON_SLOT].getId();
        int ammo = player.getEquipment().getItems()[Equipment.AMMUNITION_SLOT].getId();
        int head = player.getEquipment().getItems()[Equipment.HEAD_SLOT].getId();
        String headName = player.getEquipment().getItems()[Equipment.HEAD_SLOT].getDefinition().getName().toLowerCase();

        // Kurask and Turoth requirements
        if (name.contains("kurask") || name.contains("turoth")) {
            boolean hasLeafBladed = weapon == ItemIdentifiers.LEAF_BLADED_SPEAR || weapon == 11902 || weapon == 20727;
            boolean hasBroad = (ammo == ItemIdentifiers.BROAD_ARROWS || ammo == ItemIdentifiers.BROAD_BOLTS);
            boolean isMagicDart = player.getCombat().getCastSpell() != null && player.getCombat().getCastSpell() == com.elvarg.game.content.combat.magic.CombatSpells.MAGIC_DART.getSpell();
            
            if (!hasLeafBladed && !hasBroad && !isMagicDart) {
                player.getPacketSender().sendMessage("You need leaf-bladed weaponry, broad ammo, or Magic Dart to damage this creature.");
                return false;
            }
        }

        // Protection gear requirements (Banshee, Spectre, etc.)
        // Note: For now, we block attack if missing protection gear to simplify. 
        // In OSRS you can attack but you take massive damage/stat drain.
        if (name.contains("banshee") && !headName.contains("earmuffs") && !headName.contains("slayer helmet")) {
            player.getPacketSender().sendMessage("You need earmuffs to fight banshees.");
            return false;
        }
        
        if (name.contains("aberrant spectre") && !headName.contains("nose peg") && !headName.contains("slayer helmet")) {
            player.getPacketSender().sendMessage("You need a nose peg to fight aberrant spectres.");
            return false;
        }

        if (name.contains("dust devil") && !headName.contains("face mask") && !headName.contains("slayer helmet")) {
            player.getPacketSender().sendMessage("You need a face mask to fight dust devils.");
            return false;
        }

        return true;
    }

    public static boolean requiresFinishingOff(NPC npc) {
        String name = npc.getCurrentDefinition().getName().toLowerCase();
        return name.contains("gargoyle") || name.contains("rockslug") || name.contains("desert lizard");
    }

    public static boolean finishOff(Player player, NPC npc, int itemId) {
        String name = npc.getCurrentDefinition().getName().toLowerCase();
        boolean success = false;
        
        if (name.contains("gargoyle") && itemId == ItemIdentifiers.ROCK_HAMMER) {
            success = true;
        } else if (name.contains("rockslug") && itemId == ItemIdentifiers.BAG_OF_SALT) {
            success = true;
        } else if (name.contains("desert lizard") && itemId == ItemIdentifiers.ICE_COOLER) {
            success = true;
        }
        
        if (success) {
            if (npc.getHitpoints() <= 1) {
                npc.setHitpoints(0);
                npc.appendDeath();
                // OSRS Gargoyle death gfx/animation
                if (name.contains("gargoyle")) {
                    npc.performGraphic(new Graphic(1341)); 
                }
                return true;
            } else {
                player.getPacketSender().sendMessage("The creature is not weak enough yet.");
            }
        }
        return false;
    }

    public static void handleSpecialKeyDrops(Player player, NPC npc, List<Item> drops) {
        if (player.getSlayerTask() == null) {
            return;
        }

        SlayerMaster master = player.getSlayerTask().getMaster();
        int npcLevel = npc.getCurrentDefinition().getCombatLevel();

        // Brimstone Key (Konar)
        if (master == SlayerMaster.KONAR) {
            int chance = (int) Math.max(50, 150 - npcLevel);
            if (Misc.getRandom(chance) == 0) {
                drops.add(new Item(ItemIdentifiers.BRIMSTONE_KEY));
                player.getPacketSender().sendMessage("@red@A Brimstone key has dropped!");
            }
        }

        // Larran's Key (Krystilia / Wilderness)
        if (master == SlayerMaster.KRYSTILIA || (AreaManager.get(npc.getLocation()) instanceof WildernessArea && player.getSlayerTask().getMaster() == SlayerMaster.KRYSTILIA)) {
            int chance = (int) Math.max(40, 110 - npcLevel);
            if (Misc.getRandom(chance) == 0) {
                drops.add(new Item(ItemIdentifiers.LARRANS_KEY));
                player.getPacketSender().sendMessage("@red@A Larran's key has dropped!");
            }
        }
        
        // Cave Horror -> Black Mask (better rate on task)
        if (npc.getId() == 4353) {
            boolean onTask = Arrays.stream(player.getSlayerTask().getTask().getNpcNames()).anyMatch(n -> n.equalsIgnoreCase("cave horror"));
            int rate = onTask ? 256 : 512;
            if (Misc.getRandom(rate) == 0) {
                drops.add(new Item(11075)); // Black Mask (10)
                player.getPacketSender().sendMessage("@red@A Black Mask has dropped!");
            }
        }
    }
}
