package com.noxbuds.sailing.block;

import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

public interface BoatControlBlock {
    enum ControlType {
        HELM,
        LINE
    }

    @NotNull
    ControlType getControlType();

    Vec2 getControlLimits();
}
