package com.elvarg.game.content.skill.skillable.impl;

import com.elvarg.game.entity.impl.player.Player;

public class Agility extends DefaultSkillable {
    @Override
    public void startAnimationLoop(Player player) {}

    @Override
    public void onCycle(Player player) {}

    @Override
    public void finishedCycle(Player player) {}

    @Override
    public int cyclesRequired(Player player) {
        return 0;
    }

    @Override
    public boolean loopRequirements() {
        return false;
    }

    @Override
    public boolean allowFullInventory() {
        return false;
    }
}
