package com.noxbuds.sailing.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

public abstract class BoatControlBlock extends Block {
    public enum ControlType {
        HELM,
        LINE
    }

    public BoatControlBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @NotNull
    public abstract ControlType getControlType();

    public abstract Vec2 getControlLimits();
}
