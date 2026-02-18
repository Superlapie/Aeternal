package com.elvarg.game.model.areas.impl;

import com.elvarg.game.model.Boundary;
import com.elvarg.game.model.areas.Area;

import java.util.List;

public class AraxxorArea extends Area {

    public static final Boundary BOUNDARY = new Boundary(3666, 3696, 9734, 9770, 2);

    public AraxxorArea() {
        super(List.of(BOUNDARY));
    }

    @Override
    public boolean isMulti(com.elvarg.game.entity.impl.Mobile character) {
        return true;
    }
}
