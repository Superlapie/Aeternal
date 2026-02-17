package com.elvarg.game.model.areas.impl;

import java.util.List;

import com.elvarg.game.model.Boundary;
import com.elvarg.game.model.areas.Area;

public class YamaArea extends Area {

    public static final Boundary BOUNDARY = new Boundary(3790, 3830, 9750, 9795, 1);

    public YamaArea() {
        super(List.of(BOUNDARY));
    }

    @Override
    public boolean isMulti(com.elvarg.game.entity.impl.Mobile character) {
        return true;
    }
}

