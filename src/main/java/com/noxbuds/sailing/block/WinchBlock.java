package com.noxbuds.sailing.block;

import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

public class WinchBlock extends RiggingBlock implements BoatControlBlock {

    @Override
    public @NotNull ControlType getControlType() {
        return ControlType.LINE;
    }

    @Override
    public Vec2 getControlLimits() {
        return new Vec2(-1, 1);
    }
}
