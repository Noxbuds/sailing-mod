package com.noxbuds.sailing.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class RotatingBlock extends Block {
    public RotatingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public abstract Vec3 getBase(BlockPos blockPos, BlockState blockState);

    public abstract Vec3 getAxis(BlockPos blockPos, BlockState blockState);

    public abstract BlockPos getFirstBlock(BlockPos blockPos, BlockState blockState);
}
