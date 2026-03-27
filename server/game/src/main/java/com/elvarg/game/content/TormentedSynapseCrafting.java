package com.elvarg.game.content;

import com.elvarg.game.content.skill.impl.smithing.Smelting;
import com.elvarg.game.content.skill.skillable.impl.DefaultSkillable;
import com.elvarg.game.content.skill.skillable.impl.ItemCreationSkillable;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.AnimationLoop;
import com.elvarg.game.model.Item;
import com.elvarg.game.model.RequiredItem;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.menu.CreationMenu;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.ObjectIdentifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class TormentedSynapseCrafting extends ItemIdentifiers {

    private static final int TORMENTED_SYNAPSE = 29580;
    private static final int EMBERLIGHT = 29589;
    private static final int SCORCHING_BOW = 29591;
    private static final int PURGING_STAFF = 29594;

    private static final int FIRST_UPGRADE_XP = 730;
    private static final int SUBSEQUENT_UPGRADE_XP = 73;
    private static final int PURGING_STAFF_SMITHING_XP = 13;

    private static final AnimationLoop ANVIL_ANIMATION = new AnimationLoop(new Animation(898), 3);
    private static final AnimationLoop FURNACE_ANIMATION = new AnimationLoop(new Animation(Smelting.SMELTING_ANIMATION), 3);

    private TormentedSynapseCrafting() {
    }

    public static boolean handleItemOnItem(Player player, int itemUsed, int itemUsedWith) {
        if (isBallOfWoolAmuletPair(itemUsed, itemUsedWith)) {
            openZenyteAmuletStringMenu(player);
            return true;
        }

        if (isScorchingBowPair(itemUsed, itemUsedWith)) {
            startScorchingBowCraft(player);
            return true;
        }

        return false;
    }

    public static boolean handleItemOnObject(Player player, Item item, int objectId) {
        if (isFurnace(objectId) && item.getId() == GOLD_BAR) {
            return handleZenyteJewelleryOnFurnace(player);
        }

        if (!isAnvil(objectId)) {
            return false;
        }

        if (isEmberlightRelevantItem(item.getId()) && hasEmberlightIngredients(player)) {
            startEmberlightCraft(player);
            return true;
        }

        if (isPurgingStaffRelevantItem(item.getId()) && hasPurgingStaffIngredients(player)) {
            startPurgingStaffCraft(player);
            return true;
        }

        return false;
    }

    private static boolean handleZenyteJewelleryOnFurnace(Player player) {
        List<JewelleryRecipe> recipes = new ArrayList<>();
        if (hasZenyte(player) && player.getInventory().contains(RING_MOULD) && hasCraftingLevel(player, 89)) {
            recipes.add(JewelleryRecipe.RING);
        }
        if (hasZenyte(player) && player.getInventory().contains(NECKLACE_MOULD) && hasCraftingLevel(player, 92)) {
            recipes.add(JewelleryRecipe.NECKLACE);
        }
        if (hasZenyte(player) && player.getInventory().contains(BRACELET_MOULD) && hasCraftingLevel(player, 95)) {
            recipes.add(JewelleryRecipe.BRACELET);
        }
        if (hasZenyte(player) && player.getInventory().contains(AMULET_MOULD) && hasCraftingLevel(player, 98)) {
            recipes.add(JewelleryRecipe.AMULET_UNSTRUNG);
        }

        if (recipes.isEmpty()) {
            return false;
        }

        List<Integer> products = new ArrayList<>();
        for (JewelleryRecipe recipe : recipes) {
            products.add(recipe.productId);
        }

        player.getPacketSender().sendCreationMenu(new CreationMenu("What would you like to make?", products, (productId, amount) -> {
            JewelleryRecipe recipe = JewelleryRecipe.forProduct(productId);
            if (recipe == null) {
                return;
            }

            player.getSkillManager().startSkillable(new ItemCreationSkillable(
                    Arrays.asList(
                            new RequiredItem(new Item(GOLD_BAR), true),
                            new RequiredItem(new Item(ZENYTE), true),
                            new RequiredItem(new Item(recipe.mouldId), false)
                    ),
                    new Item(recipe.productId),
                    amount,
                    Optional.of(FURNACE_ANIMATION),
                    recipe.levelRequirement,
                    recipe.experience,
                    Skill.CRAFTING
            ));
        }));
        return true;
    }

    private static void openZenyteAmuletStringMenu(Player player) {
        if (!player.getInventory().contains(ZENYTE_AMULET_U_) || !player.getInventory().contains(BALL_OF_WOOL)) {
            return;
        }

        player.getPacketSender().sendCreationMenu(new CreationMenu("How many would you like to string?", Arrays.asList(ZENYTE_AMULET), (productId, amount) -> {
            player.getSkillManager().startSkillable(new ItemCreationSkillable(
                    Arrays.asList(
                            new RequiredItem(new Item(ZENYTE_AMULET_U_), true),
                            new RequiredItem(new Item(BALL_OF_WOOL), true)
                    ),
                    new Item(ZENYTE_AMULET),
                    amount,
                    Optional.empty(),
                    1,
                    4,
                    Skill.CRAFTING
            ));
        }));
    }

    private static void startScorchingBowCraft(Player player) {
        if (!hasScorchingBowRequirements(player)) {
            return;
        }

        player.getSkillManager().startSkillable(new SynapseUpgradeSkillable(
                player,
                Skill.FLETCHING,
                74,
                FIRST_UPGRADE_XP,
                SUBSEQUENT_UPGRADE_XP,
                0,
                SCORCHING_BOW,
                Arrays.asList(
                        new RequiredItem(new Item(TORMENTED_SYNAPSE), true),
                        new RequiredItem(new Item(MAGIC_LONGBOW_U_), true)
                ),
                Optional.empty()
        ));
    }

    private static void startEmberlightCraft(Player player) {
        if (!hasEmberlightRequirements(player)) {
            return;
        }

        player.getSkillManager().startSkillable(new SynapseUpgradeSkillable(
                player,
                Skill.SMITHING,
                74,
                FIRST_UPGRADE_XP,
                SUBSEQUENT_UPGRADE_XP,
                0,
                EMBERLIGHT,
                Arrays.asList(
                        new RequiredItem(new Item(TORMENTED_SYNAPSE), true),
                        new RequiredItem(new Item(ARCLIGHT), true),
                        new RequiredItem(new Item(HAMMER), false)
                ),
                Optional.of(ANVIL_ANIMATION)
        ));
    }

    private static void startPurgingStaffCraft(Player player) {
        if (!hasPurgingStaffRequirements(player)) {
            return;
        }

        player.getSkillManager().startSkillable(new SynapseUpgradeSkillable(
                player,
                Skill.CRAFTING,
                74,
                FIRST_UPGRADE_XP,
                SUBSEQUENT_UPGRADE_XP,
                PURGING_STAFF_SMITHING_XP,
                PURGING_STAFF,
                Arrays.asList(
                        new RequiredItem(new Item(TORMENTED_SYNAPSE), true),
                        new RequiredItem(new Item(BATTLESTAFF), true),
                        new RequiredItem(new Item(IRON_BAR), true),
                        new RequiredItem(new Item(HAMMER), false)
                ),
                Optional.of(ANVIL_ANIMATION)
        ));
    }

    private static boolean hasEmberlightRequirements(Player player) {
        if (player.getSkillManager().getCurrentLevel(Skill.SMITHING) < 74) {
            player.getPacketSender().sendMessage("You need a Smithing level of 74 to make that.");
            return false;
        }

        if (!hasEmberlightIngredients(player)) {
            player.getPacketSender().sendMessage("You need an Arclight, a tormented synapse, and a hammer to make that.");
            return false;
        }

        return true;
    }

    private static boolean hasScorchingBowRequirements(Player player) {
        if (player.getSkillManager().getCurrentLevel(Skill.FLETCHING) < 74) {
            player.getPacketSender().sendMessage("You need a Fletching level of 74 to make that.");
            return false;
        }

        if (!player.getInventory().contains(TORMENTED_SYNAPSE) || !player.getInventory().contains(MAGIC_LONGBOW_U_)) {
            player.getPacketSender().sendMessage("You need a tormented synapse and a magic longbow (u) to make that.");
            return false;
        }

        return true;
    }

    private static boolean hasPurgingStaffRequirements(Player player) {
        if (player.getSkillManager().getCurrentLevel(Skill.CRAFTING) < 74) {
            player.getPacketSender().sendMessage("You need a Crafting level of 74 to make that.");
            return false;
        }
        if (player.getSkillManager().getCurrentLevel(Skill.SMITHING) < 55) {
            player.getPacketSender().sendMessage("You need a Smithing level of 55 to make that.");
            return false;
        }
        if (!hasPurgingStaffIngredients(player)) {
            player.getPacketSender().sendMessage("You need a battlestaff, an iron bar, a tormented synapse, and a hammer to make that.");
            return false;
        }
        return true;
    }

    private static boolean hasEmberlightIngredients(Player player) {
        return player.getInventory().contains(TORMENTED_SYNAPSE)
                && player.getInventory().contains(ARCLIGHT)
                && player.getInventory().contains(HAMMER);
    }

    private static boolean hasPurgingStaffIngredients(Player player) {
        return player.getInventory().contains(TORMENTED_SYNAPSE)
                && player.getInventory().contains(BATTLESTAFF)
                && player.getInventory().contains(IRON_BAR)
                && player.getInventory().contains(HAMMER);
    }

    private static boolean hasZenyte(Player player) {
        return player.getInventory().contains(ZENYTE);
    }

    private static boolean isBallOfWoolAmuletPair(int itemUsed, int itemUsedWith) {
        return (itemUsed == BALL_OF_WOOL && itemUsedWith == ZENYTE_AMULET_U_)
                || (itemUsedWith == BALL_OF_WOOL && itemUsed == ZENYTE_AMULET_U_);
    }

    private static boolean isScorchingBowPair(int itemUsed, int itemUsedWith) {
        return (itemUsed == TORMENTED_SYNAPSE && itemUsedWith == MAGIC_LONGBOW_U_)
                || (itemUsedWith == TORMENTED_SYNAPSE && itemUsed == MAGIC_LONGBOW_U_);
    }

    private static boolean isEmberlightRelevantItem(int itemId) {
        return itemId == TORMENTED_SYNAPSE
                || itemId == ARCLIGHT
                || itemId == HAMMER;
    }

    private static boolean isPurgingStaffRelevantItem(int itemId) {
        return itemId == TORMENTED_SYNAPSE
                || itemId == BATTLESTAFF
                || itemId == IRON_BAR
                || itemId == HAMMER;
    }

    private static boolean hasCraftingLevel(Player player, int level) {
        return player.getSkillManager().getCurrentLevel(Skill.CRAFTING) >= level;
    }

    private static boolean isFurnace(int objectId) {
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

    private static boolean isAnvil(int objectId) {
        return objectId == ObjectIdentifiers.ANVIL
                || objectId == ObjectIdentifiers.ANVIL_2
                || objectId == ObjectIdentifiers.ANVIL_3
                || objectId == ObjectIdentifiers.ANVIL_4
                || objectId == ObjectIdentifiers.ANVIL_5
                || objectId == ObjectIdentifiers.ANVIL_6;
    }

    private static final class SynapseUpgradeSkillable extends DefaultSkillable {
        private final Player player;
        private final Skill primarySkill;
        private final int requiredLevel;
        private final int firstTimeXp;
        private final int repeatXp;
        private final int secondaryXp;
        private final int productId;
        private final List<RequiredItem> requiredItems;
        private final Optional<AnimationLoop> animationLoop;

        private SynapseUpgradeSkillable(Player player, Skill primarySkill, int requiredLevel,
                                        int firstTimeXp, int repeatXp, int secondaryXp, int productId,
                                        List<RequiredItem> requiredItems, Optional<AnimationLoop> animationLoop) {
            this.player = player;
            this.primarySkill = primarySkill;
            this.requiredLevel = requiredLevel;
            this.firstTimeXp = firstTimeXp;
            this.repeatXp = repeatXp;
            this.secondaryXp = secondaryXp;
            this.productId = productId;
            this.requiredItems = requiredItems;
            this.animationLoop = animationLoop;
        }

        @Override
        public void startAnimationLoop(Player player) {
            if (!animationLoop.isPresent()) {
                return;
            }

            AnimationLoop loop = animationLoop.get();
            com.elvarg.game.task.Task task = new com.elvarg.game.task.Task(loop.getLoopDelay(), player, true) {
                @Override
                protected void execute() {
                    player.performAnimation(loop.getAnim());
                }
            };
            com.elvarg.game.task.TaskManager.submit(task);
            getTasks().add(task);
        }

        @Override
        public int cyclesRequired(Player player) {
            return 2;
        }

        @Override
        public void onCycle(Player player) {
        }

        @Override
        public void finishedCycle(Player player) {
            for (RequiredItem requiredItem : requiredItems) {
                if (requiredItem.isDelete()) {
                    player.getInventory().delete(requiredItem.getItem());
                }
            }

            player.getInventory().add(new Item(productId));
            player.getSkillManager().addExperience(primarySkill, getPrimaryXp(player));
            if (secondaryXp > 0) {
                player.getSkillManager().addExperience(Skill.SMITHING, secondaryXp);
            }

            player.getPacketSender().sendMessage("You make " + new Item(productId).getDefinition().getName().toLowerCase() + ".");
            markCrafted(player);
        }

        @Override
        public boolean hasRequirements(Player player) {
            if (player.getSkillManager().getCurrentLevel(primarySkill) < requiredLevel) {
                player.getPacketSender().sendMessage("You need a " + primarySkill.getName() + " level of at least " + requiredLevel + " to do this.");
                return false;
            }

            if (productId == PURGING_STAFF && player.getSkillManager().getCurrentLevel(Skill.SMITHING) < 55) {
                player.getPacketSender().sendMessage("You need a Smithing level of 55 to make that.");
                return false;
            }

            for (RequiredItem item : requiredItems) {
                if (!player.getInventory().contains(item.getItem())) {
                    player.getPacketSender().sendMessage("You need " + item.getItem().getDefinition().getName().toLowerCase() + " to make that.");
                    return false;
                }
            }

            return super.hasRequirements(player);
        }

        @Override
        public boolean loopRequirements() {
            return true;
        }

        @Override
        public boolean allowFullInventory() {
            return true;
        }

        private int getPrimaryXp(Player player) {
            if (isCrafted(player)) {
                return repeatXp;
            }
            return firstTimeXp;
        }

        private boolean isCrafted(Player player) {
            if (productId == EMBERLIGHT) {
                return player.isEmberlightCrafted();
            }
            if (productId == SCORCHING_BOW) {
                return player.isScorchingBowCrafted();
            }
            return player.isPurgingStaffCrafted();
        }

        private void markCrafted(Player player) {
            if (productId == EMBERLIGHT) {
                player.setEmberlightCrafted(true);
                return;
            }
            if (productId == SCORCHING_BOW) {
                player.setScorchingBowCrafted(true);
                return;
            }
            player.setPurgingStaffCrafted(true);
        }
    }

    private enum JewelleryRecipe {
        RING(ZENYTE_RING, RING_MOULD, 89, 150),
        NECKLACE(ZENYTE_NECKLACE, NECKLACE_MOULD, 92, 165),
        BRACELET(ZENYTE_BRACELET, BRACELET_MOULD, 95, 180),
        AMULET_UNSTRUNG(ZENYTE_AMULET_U_, AMULET_MOULD, 98, 200);

        private final int productId;
        private final int mouldId;
        private final int levelRequirement;
        private final int experience;

        JewelleryRecipe(int productId, int mouldId, int levelRequirement, int experience) {
            this.productId = productId;
            this.mouldId = mouldId;
            this.levelRequirement = levelRequirement;
            this.experience = experience;
        }

        private static JewelleryRecipe forProduct(int productId) {
            for (JewelleryRecipe recipe : values()) {
                if (recipe.productId == productId) {
                    return recipe;
                }
            }
            return null;
        }
    }
}
