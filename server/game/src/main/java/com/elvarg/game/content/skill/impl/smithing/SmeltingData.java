package com.elvarg.game.content.skill.impl.smithing;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;

public enum SmeltingData {

    BRONZE_BAR(2349, 438, 436, 0, 1, 6.25, "Bronze", 2405, 3987, 3986, 2807, 2414),
    IRON_BAR(2351, 440, -1, 0, 15, 12.5, "Iron", 2406, 3991, 3990, 3989, 3988),
    SILVER_BAR(2355, 442, -1, 0, 20, 13.6, "Silver", 2407, 3995, 3994, 3993, 3992),
    STEEL_BAR(2353, 440, -1, 2, 30, 17.5, "Steel", 2409, 3999, 3998, 3997, 3996),
    GOLD_BAR(2357, 444, -1, 0, 40, 22.5, "Gold", 2410, 4003, 4002, 4001, 4000),
    MITHRIL_BAR(2359, 447, -1, 4, 50, 30.0, "Mithril", 2411, 7441, 7440, 6397, 4158),
    ADAMANTITE_BAR(2361, 449, -1, 6, 70, 37.5, "Adamantite", 2412, 7446, 7444, 7443, 7442),
    RUNITE_BAR(2363, 451, -1, 8, 85, 50.0, "Runite", 2413, 7450, 7449, 7448, 7447),
    CANNONBALL(2, 2353, -1, 0, 35, 25.6, "Cannonball", -1, -1, -1, -1, -1, 4, true);

    private final int barId;
    private final int primaryOre;
    private final int secondaryOre;
    private final int coalAmount;
    private final int levelReq;
    private final double experience;
    private final String name;
    private final int frameId;
    private final int[] buttons;
    private final int outputAmount;
    private final boolean requiresAmmoMould;

    SmeltingData(int barId, int primaryOre, int secondaryOre, int coalAmount, int levelReq, double experience, String name, int frameId,
                 int make1Button, int make5Button, int make10Button, int makeXButton) {
        this(barId, primaryOre, secondaryOre, coalAmount, levelReq, experience, name, frameId,
                make1Button, make5Button, make10Button, makeXButton, 1, false);
    }

    SmeltingData(int barId, int primaryOre, int secondaryOre, int coalAmount, int levelReq, double experience, String name, int frameId,
                 int make1Button, int make5Button, int make10Button, int makeXButton, int outputAmount, boolean requiresAmmoMould) {
        this.barId = barId;
        this.primaryOre = primaryOre;
        this.secondaryOre = secondaryOre;
        this.coalAmount = coalAmount;
        this.levelReq = levelReq;
        this.experience = experience;
        this.name = name;
        this.frameId = frameId;
        this.buttons = new int[]{make1Button, make5Button, make10Button, makeXButton};
        this.outputAmount = outputAmount;
        this.requiresAmmoMould = requiresAmmoMould;
    }

    public int getBarId() {
        return barId;
    }

    public int getPrimaryOre() {
        return primaryOre;
    }

    public int getSecondaryOre() {
        return secondaryOre;
    }

    public int getCoalAmount() {
        return coalAmount;
    }

    public int getLevelRequirement() {
        return levelReq;
    }

    public double getExperienceGained() {
        return experience;
    }

    public String getName() {
        return name;
    }

    public int getFrameId() {
        return frameId;
    }

    public int[] getButtons() {
        return buttons;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public boolean requiresAmmoMould() {
        return requiresAmmoMould;
    }

    public boolean hasRequiredOres(Player player) {
        if (requiresAmmoMould && !player.getInventory().contains(4)) {
            return false;
        }
        if (player.getInventory().getAmount(primaryOre) < 1) {
            return false;
        }
        if (secondaryOre != -1 && player.getInventory().getAmount(secondaryOre) < 1) {
            return false;
        }
        if (coalAmount > 0 && player.getInventory().getAmount(453) < coalAmount) {
            return false;
        }
        return true;
    }

    public void removeRequiredOres(Player player) {
        player.getInventory().delete(new Item(primaryOre, 1));

        if (secondaryOre != -1) {
            player.getInventory().delete(new Item(secondaryOre, 1));
        }

        if (coalAmount > 0) {
            player.getInventory().delete(new Item(453, coalAmount));
        }
    }

    public int getMaxBars(Player player) {
        int maxBars = player.getInventory().getAmount(primaryOre);

        if (secondaryOre != -1) {
            maxBars = Math.min(maxBars, player.getInventory().getAmount(secondaryOre));
        }

        if (coalAmount > 0) {
            maxBars = Math.min(maxBars, player.getInventory().getAmount(453) / coalAmount);
        }

        return maxBars;
    }

    public boolean isIronBar() {
        return this == IRON_BAR;
    }

    public static SmeltingData forBarId(int barId) {
        for (SmeltingData data : values()) {
            if (data.barId == barId) {
                return data;
            }
        }
        return null;
    }

    public static SmeltingData forName(String barName) {
        for (SmeltingData data : values()) {
            if (data.name.equalsIgnoreCase(barName)) {
                return data;
            }
        }
        return null;
    }

    public double getExperienceWithGoldsmithGauntlets(boolean hasGoldsmithGauntlets) {
        if (this == GOLD_BAR && hasGoldsmithGauntlets) {
            return 56.2;
        }
        return experience;
    }
}
