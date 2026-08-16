package com.andrewbristowx.chainacobblemon.hologram;

import java.util.ArrayList;
import java.util.List;

public final class HologramDefinition {
    public String id;
    public String world;
    public double x;
    public double y;
    public double z;
    public List<String> lines = new ArrayList<>();
    public String entityUuid;

    public HologramDefinition() {}

    public HologramDefinition(String id, String world, double x, double y, double z) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lines.add("&f" + id);
    }
}
