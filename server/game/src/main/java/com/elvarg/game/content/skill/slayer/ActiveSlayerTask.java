package com.elvarg.game.content.skill.slayer;

import com.elvarg.game.model.areas.Area;

public class ActiveSlayerTask {

    private final SlayerMaster master;
    private final SlayerTask task;
    private int remaining;
    private Area location;
    private String locationName;
    
    ActiveSlayerTask(SlayerMaster master, SlayerTask task, int amount) {
        this(master, task, amount, null, null);
    }

    ActiveSlayerTask(SlayerMaster master, SlayerTask task, int amount, Area location, String locationName) {
        this.master = master;
        this.task = task;
        this.remaining = amount;
        this.location = location;
        this.locationName = locationName;
    }
    
    public SlayerMaster getMaster() {
        return master;
    }
    
    public SlayerTask getTask() {
        return task;
    }
    
    public void setRemaining(int amount) {
        this.remaining = amount;
    }

    public int getRemaining() {
        return remaining;
    }

    public Area getLocation() {
        return location;
    }

    public void setLocation(Area location) {
        this.location = location;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
