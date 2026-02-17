package com.elvarg.game.content.skill.slayer;

import com.elvarg.util.Misc;

public enum SlayerUnlock {

    GARGOYLE_SMASHER(120, "Gargoyle Smasher", "Automatically smashes Gargoyles when they reach low health."),
    SLAYER_HELMET_CRAFTING(400, "Malevolent Masquerade", "Learn how to combine a Slayer helmet."),
    IMBUED_SLAYER_HELMET(500, "Imbued Slayer Helmet", "Learn how to imbue your Slayer helmet for better stats."),
    EXTENDED_ABYSSAL_DEMONS(100, "Extended Abyssal Demons", "Extend your Abyssal Demon tasks."),
    EXTENDED_BLOODVELDS(100, "Extended Bloodvelds", "Extend your Bloodveld tasks."),
    EXTENDED_GARGOYLES(100, "Extended Gargoyles", "Extend your Gargoyle tasks."),
    EXTENDED_KRAKENS(100, "Extended Krakens", "Extend your Kraken tasks."),
    EXTENDED_DARK_BEASTS(100, "Extended Dark Beasts", "Extend your Dark Beast tasks."),
    SUPERIOR_SLAYER_MONSTERS(150, "Bigger and Badder", "Unlock the ability to encounter superior slayer monsters.");

    private final int cost;
    private final String name;
    private final String description;

    SlayerUnlock(int cost, String name, String description) {
        this.cost = cost;
        this.name = name;
        this.description = description;
    }

    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return Misc.ucFirst(name().toLowerCase().replaceAll("_", " "));
    }
}
