package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.util.Misc;

/**
 * Represents all of the possible Slayer tasks a player can be assigned.
 * 
 * @author Professor Oak
 */
public enum SlayerTask {
    BANSHEES("in the Slayer Tower", 15, 50, 15, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "banshee", "twisted banshee" }),
    BATS("in the Taverly Dungeon", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "bat", "giant bat" }),
    CHICKENS("in Lumbridge", 15, 50, 1, 6, new SlayerMaster[] { SlayerMaster.TURAEL },
            new String[] { "chicken", "mounted terrorbird gnome", "terrorbird", "rooster", }),
    BEARS("outside Varrock", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "black bear", "grizzly bear", "grizzly bear cub", "bear cub", "callisto" }),
    CAVE_BUGS("Lumbridge dungeon", 10, 20, 7, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "cave bug" }),
    CAVE_CRAWLERS("Lumbridge dungeon", 15, 50, 10, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "cave crawler" }),    
    CAVE_SLIME("Lumbridge dungeon", 10, 20, 17, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "cave slime" }),    
    COWS("Lumbridge", 15, 50, 1, 8, new SlayerMaster[] { SlayerMaster.TURAEL },
            new String[] { "cow", "cow calf" }),
    CRAWLING_HANDS("in the Slayer Tower", 15, 50, 5, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "crawling hand" }),
    DESERT_LIZARDS("in the desert", 15, 50, 22, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "lizard", "small lizard", "desert lizard" }),
    DOGS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "dog", "jackal", "guard dog", "wild dog" }),
    DWARVES("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.VANNAKA }, new String[] {"dwarf", "dwarf gang member", "chaos dwarf"}),
    GHOSTS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR },
            new String[] { "ghost", "tortured soul" }),
    GOBLINS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL },
            new String[] { "goblin", "cave goblin guard" }),
    ICEFIENDS("", 15, 50, 1, 8, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.VANNAKA }, new String[] { "icefiend" }),
    KALPHITES("", 15, 50, 1, 6, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE },
            new String[] { "kalphite worker", "kalphite soldier", "kalphite guardian", "kalphite queen" }),
    MINOTAURS("", 10, 20, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL }, new String[] { "minotaur" }),
    MONKEYS("", 10, 20, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL },
            new String[] { "monkey", "karmjan monkey", "monkey guard", "monkey archer", "zombie monkey" }),
    RATS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL },
            new String[] { "rat", "giant rat", "dungeon rat", "brine rat" }),
    SCORPIONS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "scorpion", "king scorpion", "poison scorpion", "pit scorpion", "scorpia" }),
    SKELETONS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR },
            new String[] { "skeleton", "skeleton mage", "vet'ion" }),
    SPIDERS("", 15, 50, 1, 6, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.KRYSTILIA },
            new String[] { "spider", "giant spider", "shadow spider", "giant crypt spider", "venenatis" }),
    WOLVES("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA },
            new String[] { "wolf", "white wolf", "big wolf" }),
    ZOMBIES("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.TURAEL, SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR }, new String[] { "zombie", "undead one" }),
    HILL_GIANTS("", 15, 50, 1, 7, new SlayerMaster[] { SlayerMaster.MAZCHNA, SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KRYSTILIA }, new String[] { "hill giant" }),
    BLOODVELDS("", 40, 100, 50, 9, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "bloodveld", "mutated bloodveld" }),
    DAGANNOTHS("", 40, 100, 1, 9, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "dagannoth", "dagannoth prime", "dagannoth rex", "dagannoth supreme" }),
    FIRE_GIANTS("", 40, 100, 1, 8, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "fire giant" }),
    GREATER_DEMONS("", 40, 100, 1, 8, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL, SlayerMaster.KRYSTILIA }, new String[] { "greater demon" }),
    HELLHOUNDS("", 40, 100, 1, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL, SlayerMaster.KRYSTILIA }, new String[] { "hellhound", "cerberus" }),
    BLACK_DEMONS("", 40, 100, 1, 7, new SlayerMaster[] { SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL, SlayerMaster.KRYSTILIA }, new String[] { "black demon" }),
    ABYSSAL_DEMONS("", 50, 150, 85, 9, new SlayerMaster[] { SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "abyssal demon" }),
    GARGOYLES("", 50, 150, 75, 8, new SlayerMaster[] { SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "gargoyle" }),
    NECHRYAELS("", 50, 150, 80, 8, new SlayerMaster[] { SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "nechryael", "greater nechryael" }),
    DARK_BEASTS("", 50, 120, 90, 7, new SlayerMaster[] { SlayerMaster.KURADAL }, new String[] { "dark beast" }),
    BLUE_DRAGONS("", 40, 100, 1, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "blue dragon", "baby blue dragon", "vorkath" }),
    BLACK_DRAGONS("", 10, 50, 1, 6, new SlayerMaster[] { SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "black dragon", "baby black dragon", "king black dragon" }),
    IRON_DRAGONS("", 30, 60, 1, 5, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "iron dragon" }),
    STEEL_DRAGONS("", 30, 60, 1, 5, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "steel dragon" }),
    MITHRIL_DRAGONS("", 5, 20, 1, 4, new SlayerMaster[] { SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "mithril dragon" }),
    ADAMANT_DRAGONS("", 5, 20, 1, 4, new SlayerMaster[] { SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "adamant dragon" }),
    RUNE_DRAGONS("", 5, 20, 1, 4, new SlayerMaster[] { SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "rune dragon" }),
    KRAKEN("", 30, 100, 87, 8, new SlayerMaster[] { SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "cave kraken", "kraken" }),
    SMOKE_DEVILS("", 30, 100, 93, 8, new SlayerMaster[] { SlayerMaster.KURADAL }, new String[] { "smoke devil", "thermonuclear smoke devil" }),
    AVIANSIES("", 50, 150, 1, 7, new SlayerMaster[] { SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "aviansie" }),
    BASILISKS("", 40, 100, 40, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR }, new String[] { "basilisk", "basilisk knight" }),
    TUROTHS("", 40, 100, 55, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR }, new String[] { "turoth" }),
    KURASKS("", 40, 100, 70, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "kurask" }),
    SPECTRES("", 40, 100, 60, 8, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "aberrant spectre", "deviant spectre" }),
    DUST_DEVILS("", 40, 100, 65, 8, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "dust devil" }),
    WYVERNS("", 30, 100, 72, 7, new SlayerMaster[] { SlayerMaster.VANNAKA, SlayerMaster.CHAELDAR, SlayerMaster.KONAR, SlayerMaster.STEVE, SlayerMaster.KURADAL }, new String[] { "skeletal wyvern" }),
    
    // Wilderness Exclusives
    LAVA_DRAGONS("", 30, 60, 1, 8, new SlayerMaster[] { SlayerMaster.KRYSTILIA }, new String[] { "lava dragon" }),
    ENT_TASKS("", 20, 50, 1, 8, new SlayerMaster[] { SlayerMaster.KRYSTILIA }, new String[] { "ent" }),
    MAMMOTHS("", 50, 100, 1, 8, new SlayerMaster[] { SlayerMaster.KRYSTILIA }, new String[] { "mammoth" }),
    ROBBERS("", 50, 100, 1, 8, new SlayerMaster[] { SlayerMaster.KRYSTILIA }, new String[] { "rogue", "bandit" }),
    CHAOS_ELEMENTAL("", 5, 15, 1, 5, new SlayerMaster[] { SlayerMaster.KRYSTILIA }, new String[] { "chaos elemental" }),
    ;

    private final String hint;
    private final int minimumAmount;
    private final int maximumAmount;
    private final int slayerLevel;
    private final int weight;
    private final SlayerMaster[] masters;
    private final String[] npcNames;

    SlayerTask(String hint, int minimumAmount, int maximumAmount, int slayerLevel, int weight,
            SlayerMaster[] masters, String[] npcNames) {
        this.hint = hint;
        this.minimumAmount = minimumAmount;
        this.maximumAmount = maximumAmount;
        this.slayerLevel = slayerLevel;
        this.weight = weight;
        this.masters = masters;
        this.npcNames = npcNames;
    }

    public String getHint() {
        return hint;
    }

    public int getMinimumAmount() {
        return minimumAmount;
    }

    public int getMaximumAmount() {
        return maximumAmount;
    }

    public int getSlayerLevel() {
        return slayerLevel;
    }

    public int getWeight() {
        return weight;
    }

    public SlayerMaster[] getMasters() {
        return masters;
    }

    public String[] getNpcNames() {
        return npcNames;
    }
    
    @Override
    public String toString() {
        return Misc.ucFirst(name().toLowerCase().replaceAll("_", ""));
    }

    public boolean isUnlocked(Player player) {
        return true;
    }

    public static final SlayerTask[] VALUES = values();
}
