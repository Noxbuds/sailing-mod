package com.noxbuds.sailing.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RudderBlock extends RotatingBlock {
    public RudderBlock() {
        super(BlockBehaviour.Properties.of());
    }

    @Override
    public Vec3 getBase(BlockPos blockPos, BlockState blockState) {
        return blockPos.getCenter().add(0.5, 0.5, 0.5);
    }

    @Override
    public Vec3 getAxis(BlockPos blockPos, BlockState blockState) {
        return new Vec3(0, -1, 0);
    }

    @Override
    public BlockPos getFirstBlock(BlockPos blockPos, BlockState blockState) {
        return blockPos.below();
    }
}
