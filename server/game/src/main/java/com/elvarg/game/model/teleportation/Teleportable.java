package com.elvarg.game.model.teleportation;

import com.elvarg.game.content.minigames.impl.CastleWars;
import com.elvarg.game.content.minigames.impl.pestcontrol.PestControl;
import com.elvarg.game.model.Location;
import java.util.Locale;

public enum Teleportable {
	
	EDGEVILLE_DITCH(TeleportButton.WILDERNESS, 0, 0, new Location(3088, 3520)),
	WEST_DRAGONS(TeleportButton.WILDERNESS, 0, 1, new Location(2979, 3592)),
	EAST_DRAGONS(TeleportButton.WILDERNESS, 0, 2, new Location(3356, 3675)),

	FISHING_GUILD(TeleportButton.SKILLS, 3, 0, new Location(2611, 3391)),
	CATHERBY(TeleportButton.SKILLS, 3, 1, new Location(2836, 3433)),
	BARBARIAN_OUTPOST(TeleportButton.SKILLS, 3, 2, new Location(2542, 3569)),
	SEERS_VILLAGE(TeleportButton.SKILLS, 3, 3, new Location(2725, 3492)),
	CRAFTING_GUILD(TeleportButton.SKILLS, 3, 4, new Location(2933, 3287)),
	RIMMINGTON_MINE(TeleportButton.SKILLS, 3, 5, new Location(2977, 3238)),
	AL_KHARID_MINE(TeleportButton.SKILLS, 3, 6, new Location(3297, 3317)),
	
	GODWARS_DUNGEON(TeleportButton.BOSSES, 2, 0, new Location(2831, 5299, 2)),
	KING_BLACK_DRAGON(TeleportButton.BOSSES, 2, 1, new Location(3005, 3850)),
	CHAOS_ELEMENTAL(TeleportButton.BOSSES, 2, 2, new Location(3267, 3916)),
	ELDER_CHAOS_DRUID(TeleportButton.BOSSES, 2, 3, new Location(3236, 3636)),
	CRAZY_ARCHAEOLOGIST(TeleportButton.BOSSES, 2, 4, new Location(2980, 3708)),
	CHAOS_FANATIC(TeleportButton.BOSSES, 2, 5, new Location(2986, 3838)),
	VENENATIS(TeleportButton.BOSSES, 2, 6, new Location(3346, 3727)),
	VET_ION(TeleportButton.BOSSES, 2, 7, new Location(3187, 3787)),
	CALLISTO(TeleportButton.BOSSES, 2, 8, new Location(3312, 3830)),
	ZULRAH(TeleportButton.BOSSES, 2, 9, new Location(2202, 3056)),
	VORKATH(TeleportButton.BOSSES, 2, 10, new Location(2272, 4052)),
	NIGHTMARE_LAIR(TeleportButton.BOSSES, 2, 11, new Location(3808, 9756, 1)),
	ARAXXOR(TeleportButton.BOSSES, 2, 12, new Location(3682, 9752, 2)),
	DEMONIC_GORILLAS(TeleportButton.BOSSES, 2, 13, new Location(2140, 5651)),
	
	DUEL_ARENA(TeleportButton.MINIGAME, 1, 0, new Location(3370, 3270)),
	BARROWS(TeleportButton.MINIGAME, 1, 1, new Location(3565, 3313)),
	FIGHT_CAVES(TeleportButton.MINIGAME, 1, 2, new Location(2439, 5171)),
	CASTLE_WARS(TeleportButton.MINIGAME, 1, 3, CastleWars.LOBBY_TELEPORT),
	PEST_CONTROL(TeleportButton.MINIGAME, 1, 4, PestControl.GANG_PLANK_START),

	VARROCK(TeleportButton.CITY, 4, 0, new Location(3210, 3424)),
	FALADOR(TeleportButton.CITY, 4, 1, new Location(2964, 3378)),
	LUMBRIDGE(TeleportButton.CITY, 4, 2, new Location(3222, 3218)),
	SEERS_VILLAGE_CITY(TeleportButton.CITY, 4, 3, new Location(2725, 3492)),
	ARDOUGNE(TeleportButton.CITY, 4, 4, new Location(2661, 3302)),
	CAMELOT(TeleportButton.CITY, 4, 5, new Location(2757, 3479)),
	YANILLE(TeleportButton.CITY, 4, 6, new Location(2606, 3093)),
	GRAND_EXCHANGE(TeleportButton.CITY, 4, 7, new Location(3163, 3479))
	
	;
	
	private final TeleportButton teleportButton;
	private final int type;
	private final int index;
	private final Location position;
	
	private Teleportable(TeleportButton teleportButton, int type, int index, Location position) {
		this.teleportButton = teleportButton;
		this.type = type;
		this.index = index;
		this.position = position;
	}

	public TeleportButton getTeleportButton() {
		return teleportButton;
	}

	public int getType() {
		return type;
	}

	public int getIndex() {
		return index;
	}

	public Location getPosition() {
		return position;
	}

	public String getDisplayName() {
		if (this == VET_ION) {
			return "Vet'ion";
		}
		String[] parts = name().toLowerCase(Locale.ENGLISH).split("_");
		StringBuilder nameBuilder = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (nameBuilder.length() > 0) {
				nameBuilder.append(' ');
			}
			nameBuilder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return nameBuilder.toString();
	}
}
