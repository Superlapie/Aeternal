package com.elvarg.game.entity.impl.npc.impl;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.npc.NPC;
import com.elvarg.game.entity.impl.npc.NPCInteraction;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Animation;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.Skill;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.util.ItemIdentifiers;
import com.elvarg.util.NpcIdentifiers;
import com.elvarg.game.model.Ids;

import java.util.Map;

@Ids({
        NpcIdentifiers.BABY_IMPLING,
        NpcIdentifiers.YOUNG_IMPLING,
        NpcIdentifiers.GOURMET_IMPLING,
        NpcIdentifiers.EARTH_IMPLING,
        NpcIdentifiers.ESSENCE_IMPLING,
        NpcIdentifiers.ECLECTIC_IMPLING,
        NpcIdentifiers.NATURE_IMPLING,
        NpcIdentifiers.MAGPIE_IMPLING,
        NpcIdentifiers.NINJA_IMPLING,
        NpcIdentifiers.DRAGON_IMPLING,
        NpcIdentifiers.BABY_IMPLING_2,
        NpcIdentifiers.YOUNG_IMPLING_2,
        NpcIdentifiers.GOURMET_IMPLING_2,
        NpcIdentifiers.EARTH_IMPLING_2,
        NpcIdentifiers.ESSENCE_IMPLING_2,
        NpcIdentifiers.ECLECTIC_IMPLING_2,
        NpcIdentifiers.NATURE_IMPLING_2,
        NpcIdentifiers.MAGPIE_IMPLING_2,
        NpcIdentifiers.NINJA_IMPLING_2,
        NpcIdentifiers.DRAGON_IMPLING_2,
        NpcIdentifiers.WANDERING_IMPLING,
        NpcIdentifiers.LUCKY_IMPLING,
        NpcIdentifiers.LUCKY_IMPLING_2
})
public class Impling extends NPC implements NPCInteraction {

    private static final Animation CATCH_ANIM = new Animation(827);
    private static final int[] BUTTERFLY_NETS = {
            ItemIdentifiers.BUTTERFLY_NET,
            ItemIdentifiers.BUTTERFLY_NET_2,
            ItemIdentifiers.MAGIC_BUTTERFLY_NET
    };

    private static final Map<Integer, CatchData> CATCH_DATA = Map.ofEntries(
            Map.entry(NpcIdentifiers.BABY_IMPLING, new CatchData(17, ItemIdentifiers.BABY_IMPLING_JAR, 10)),
            Map.entry(NpcIdentifiers.BABY_IMPLING_2, new CatchData(17, ItemIdentifiers.BABY_IMPLING_JAR, 10)),
            Map.entry(NpcIdentifiers.YOUNG_IMPLING, new CatchData(22, ItemIdentifiers.YOUNG_IMPLING_JAR, 20)),
            Map.entry(NpcIdentifiers.YOUNG_IMPLING_2, new CatchData(22, ItemIdentifiers.YOUNG_IMPLING_JAR, 20)),
            Map.entry(NpcIdentifiers.GOURMET_IMPLING, new CatchData(28, ItemIdentifiers.GOURMET_IMPLING_JAR, 30)),
            Map.entry(NpcIdentifiers.GOURMET_IMPLING_2, new CatchData(28, ItemIdentifiers.GOURMET_IMPLING_JAR, 30)),
            Map.entry(NpcIdentifiers.EARTH_IMPLING, new CatchData(36, ItemIdentifiers.EARTH_IMPLING_JAR, 45)),
            Map.entry(NpcIdentifiers.EARTH_IMPLING_2, new CatchData(36, ItemIdentifiers.EARTH_IMPLING_JAR, 45)),
            Map.entry(NpcIdentifiers.ESSENCE_IMPLING, new CatchData(42, ItemIdentifiers.ESSENCE_IMPLING_JAR, 50)),
            Map.entry(NpcIdentifiers.ESSENCE_IMPLING_2, new CatchData(42, ItemIdentifiers.ESSENCE_IMPLING_JAR, 50)),
            Map.entry(NpcIdentifiers.ECLECTIC_IMPLING, new CatchData(50, ItemIdentifiers.ECLECTIC_IMPLING_JAR, 75)),
            Map.entry(NpcIdentifiers.ECLECTIC_IMPLING_2, new CatchData(50, ItemIdentifiers.ECLECTIC_IMPLING_JAR, 75)),
            Map.entry(NpcIdentifiers.NATURE_IMPLING, new CatchData(58, ItemIdentifiers.NATURE_IMPLING_JAR, 95)),
            Map.entry(NpcIdentifiers.NATURE_IMPLING_2, new CatchData(58, ItemIdentifiers.NATURE_IMPLING_JAR, 95)),
            Map.entry(NpcIdentifiers.MAGPIE_IMPLING, new CatchData(65, ItemIdentifiers.MAGPIE_IMPLING_JAR, 115)),
            Map.entry(NpcIdentifiers.MAGPIE_IMPLING_2, new CatchData(65, ItemIdentifiers.MAGPIE_IMPLING_JAR, 115)),
            Map.entry(NpcIdentifiers.NINJA_IMPLING, new CatchData(74, ItemIdentifiers.NINJA_IMPLING_JAR, 135)),
            Map.entry(NpcIdentifiers.NINJA_IMPLING_2, new CatchData(74, ItemIdentifiers.NINJA_IMPLING_JAR, 135)),
            Map.entry(NpcIdentifiers.DRAGON_IMPLING, new CatchData(83, ItemIdentifiers.DRAGON_IMPLING_JAR, 180)),
            Map.entry(NpcIdentifiers.DRAGON_IMPLING_2, new CatchData(83, ItemIdentifiers.DRAGON_IMPLING_JAR, 180)),
            Map.entry(NpcIdentifiers.WANDERING_IMPLING, new CatchData(17, ItemIdentifiers.IMPLING_JAR, 10)),
            Map.entry(NpcIdentifiers.LUCKY_IMPLING, new CatchData(89, ItemIdentifiers.LUCKY_IMPLING_JAR, 380)),
            Map.entry(NpcIdentifiers.LUCKY_IMPLING_2, new CatchData(89, ItemIdentifiers.LUCKY_IMPLING_JAR, 380))
    );

    public Impling(int id, Location position) {
        super(id, position);
    }

    @Override
    public void firstOptionClick(Player player, NPC npc) {
        catchImpling(player, npc);
    }

    @Override
    public void secondOptionClick(Player player, NPC npc) {
    }

    @Override
    public void thirdOptionClick(Player player, NPC npc) {
    }

    @Override
    public void forthOptionClick(Player player, NPC npc) {
    }

    @Override
    public void useItemOnNpc(Player player, NPC npc, int itemId, int slot) {
    }

    private void catchImpling(Player player, NPC npc) {
        CatchData data = CATCH_DATA.get(npc.getId());
        if (data == null) {
            return;
        }

        if (npc.getId() == NpcIdentifiers.WANDERING_IMPLING) {
            player.getPacketSender().sendMessage("You can't catch this impling.");
            return;
        }

        if (player.getSkillManager().getCurrentLevel(Skill.HUNTER) < data.level) {
            player.getPacketSender().sendMessage("You need a Hunter level of " + data.level + " to catch this impling.");
            return;
        }

        if (!hasButterflyNet(player) && player.getSkillManager().getCurrentLevel(Skill.HUNTER) < data.level + 10) {
            player.getPacketSender().sendMessage("You need a butterfly net to catch this impling, unless you can barehand it at level " + (data.level + 10) + ".");
            return;
        }

        if (player.getInventory().isFull()) {
            player.getPacketSender().sendMessage("You need an empty inventory slot to catch this impling.");
            return;
        }

        player.performAnimation(CATCH_ANIM);
        player.getSkillManager().addExperience(Skill.HUNTER, data.xp);
        player.getInventory().add(data.jarItemId, 1);
        player.getPacketSender().sendMessage("You catch the impling.");
        World.getRemoveNPCQueue().add(npc);
    }

    private boolean hasButterflyNet(Player player) {
        for (int net : BUTTERFLY_NETS) {
            if (player.getInventory().contains(net) || player.getEquipment().getItems()[Equipment.WEAPON_SLOT].getId() == net) {
                return true;
            }
        }
        return false;
    }

    private static final class CatchData {
        private final int level;
        private final int jarItemId;
        private final int xp;

        private CatchData(int level, int jarItemId, int xp) {
            this.level = level;
            this.jarItemId = jarItemId;
            this.xp = xp;
        }
    }
}
