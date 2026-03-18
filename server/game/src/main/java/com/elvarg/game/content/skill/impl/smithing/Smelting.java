package com.elvarg.game.content.skill.impl.smithing;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.Misc;

public class Smelting {

    public static final int SMELTING_ANIMATION = 899;
    public static final int RING_OF_FORGING = 2568;
    public static final int GOLDSMITH_GAUNTLETS = 776;

    private static final String RING_CHARGES_ATTR = "ring_of_forging_charges";

    public static void openFurnaceInterface(Player player) {
        if (!hasAnyBarsToSmelt(player)) {
            player.getPacketSender().sendMessage("You don't have any ores that you can smelt.");
            return;
        }

        player.getPacketSender().sendChatboxInterface(2400);
        player.getPacketSender().sendString(2401, "What would you like to smelt?");
        player.getPacketSender().sendString(2402, "Click here to continue");
        player.setAttribute("furnace_interface_open", true);

        for (SmeltingData bar : SmeltingData.values()) {
            if (bar.getFrameId() != -1 && player.getSkillManager().getCurrentLevel(Skill.SMITHING) >= bar.getLevelRequirement()) {
                player.getPacketSender().sendInterfaceModel(bar.getFrameId(), bar.getBarId(), 150);
            }
        }
    }

    public static void handleSmeltCommand(Player player, String barName) {
        SmeltingData selectedBar = SmeltingData.forName(barName);
        if (selectedBar == null) {
            player.getPacketSender().sendMessage("Invalid bar. Try: bronze, iron, silver, steel, gold, mithril, adamantite, runite.");
            return;
        }

        if (player.getSkillManager().getCurrentLevel(Skill.SMITHING) < selectedBar.getLevelRequirement()) {
            player.getPacketSender().sendMessage("You need level " + selectedBar.getLevelRequirement() + " Smithing to smelt " + selectedBar.getName().toLowerCase() + " bars.");
            return;
        }

        if (!selectedBar.hasRequiredOres(player)) {
            player.getPacketSender().sendMessage("You don't have the required ores to smelt " + selectedBar.getName().toLowerCase() + " bars.");
            return;
        }

        startSmelting(player, selectedBar, 1);
    }

    public static boolean handleMakeXButton(Player player, int buttonId) {
        Object furnaceOpen = player.getAttribute("furnace_interface_open");
        if (!(furnaceOpen instanceof Boolean) || !((Boolean) furnaceOpen)) {
            return false;
        }

        for (SmeltingData bar : SmeltingData.values()) {
            if (bar.getFrameId() == -1) {
                continue;
            }
            if (buttonId == bar.getFrameId()) {
                startSmelting(player, bar, 1);
                return true;
            }
            int[] buttons = bar.getButtons();
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] != buttonId) {
                    continue;
                }

                if (player.getSkillManager().getCurrentLevel(Skill.SMITHING) < bar.getLevelRequirement()) {
                    player.getPacketSender().sendMessage("You need a Smithing level of " + bar.getLevelRequirement() + " to smelt this bar.");
                    return true;
                }

                if (i == 3) {
                    player.setEnteredAmountAction((input) -> startSmelting(player, bar, input));
                    player.getPacketSender().sendEnterAmountPrompt("How many bars would you like to smelt?");
                    return true;
                }

                int amount = i == 0 ? 1 : i == 1 ? 5 : 10;
                startSmelting(player, bar, amount);
                return true;
            }
        }

        return false;
    }

    public static void startSmelting(Player player, SmeltingData barData, int amount) {
        if (!canSmelt(player, barData)) {
            return;
        }

        int maxAmount = Math.min(amount, barData.getMaxBars(player));
        if (maxAmount <= 0) {
            player.getPacketSender().sendMessage("You don't have enough ores to make any bars.");
            return;
        }

        player.getPacketSender().sendInterfaceRemoval();
        player.setAttribute("furnace_interface_open", false);
        TaskManager.submit(new SmeltingTask(player, barData, maxAmount));
    }

    private static boolean hasAnyBarsToSmelt(Player player) {
        int smithingLevel = player.getSkillManager().getCurrentLevel(Skill.SMITHING);
        for (SmeltingData bar : SmeltingData.values()) {
            if (smithingLevel >= bar.getLevelRequirement() && bar.hasRequiredOres(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSmelt(Player player, SmeltingData barData) {
        if (player.getSkillManager().getCurrentLevel(Skill.SMITHING) < barData.getLevelRequirement()) {
            player.getPacketSender().sendMessage("You need a Smithing level of " + barData.getLevelRequirement() + " to smelt " + barData.getName().toLowerCase() + " bars.");
            return false;
        }

        if (!barData.hasRequiredOres(player)) {
            if (barData.requiresAmmoMould() && !player.getInventory().contains(4)) {
                player.getPacketSender().sendMessage("You need an ammo mould to smith cannonballs.");
                return false;
            }
            player.getPacketSender().sendMessage("You don't have the required ores to smelt this bar.");
            return false;
        }

        if (player.getInventory().getFreeSlots() <= 0) {
            player.getPacketSender().sendMessage("You don't have enough inventory space to smelt bars.");
            return false;
        }

        return true;
    }

    private static boolean hasRingOfForging(Player player) {
        return player.getEquipment().getItems()[Equipment.RING_SLOT].getId() == RING_OF_FORGING;
    }

    private static boolean hasGoldsmithGauntlets(Player player) {
        return player.getEquipment().getItems()[Equipment.HANDS_SLOT].getId() == GOLDSMITH_GAUNTLETS;
    }

    private static int getRingCharges(Player player) {
        if (!hasRingOfForging(player)) {
            player.setAttribute(RING_CHARGES_ATTR, 0);
            return 0;
        }

        Object existing = player.getAttribute(RING_CHARGES_ATTR);
        if (existing instanceof Integer) {
            return (Integer) existing;
        }

        player.setAttribute(RING_CHARGES_ATTR, 140);
        return 140;
    }

    private static class SmeltingTask extends Task {

        private final Player player;
        private final SmeltingData barData;
        private final int maxAmount;
        private final boolean hasGoldsmithGauntlets;

        private int barsSmelted = 0;
        private int ringCharges;

        public SmeltingTask(Player player, SmeltingData barData, int maxAmount) {
            super(4, player, true);
            this.player = player;
            this.barData = barData;
            this.maxAmount = maxAmount;
            this.hasGoldsmithGauntlets = hasGoldsmithGauntlets(player);
            this.ringCharges = getRingCharges(player);
        }

        @Override
        protected void execute() {
            if (barsSmelted >= maxAmount || !canContinueSmelting()) {
                stop();
                return;
            }

            player.performAnimation(new Animation(SMELTING_ANIMATION));

            if (barData.isIronBar()) {
                boolean guaranteedByRing = hasRingOfForging(player) && ringCharges > 0;
                boolean success = guaranteedByRing || Misc.getRandom(1) == 0;

                if (guaranteedByRing) {
                    ringCharges--;
                    player.setAttribute(RING_CHARGES_ATTR, ringCharges);
                    if (ringCharges <= 0) {
                        player.getEquipment().delete(Equipment.RING_SLOT, 1);
                        player.setAttribute(RING_CHARGES_ATTR, 0);
                        player.getPacketSender().sendMessage("Your Ring of Forging has crumbled to dust.");
                    }
                }

                if (!success) {
                    player.getPacketSender().sendMessage("The iron ore was too impure and you were unable to make an iron bar.");
                    barData.removeRequiredOres(player);
                    barsSmelted++;
                    return;
                }
            }

            barData.removeRequiredOres(player);
            player.getInventory().add(new Item(barData.getBarId(), barData.getOutputAmount()));

            double exp = barData.getExperienceWithGoldsmithGauntlets(hasGoldsmithGauntlets);
            player.getSkillManager().addExperience(Skill.SMITHING, (int) Math.round(exp));

            barsSmelted++;
        }

        private boolean canContinueSmelting() {
            return player.getInventory().getFreeSlots() > 0 && barData.hasRequiredOres(player);
        }
    }
}
