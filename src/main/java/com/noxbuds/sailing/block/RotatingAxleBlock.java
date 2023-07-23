package com.noxbuds.sailing.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RotatingAxleBlock extends RotatingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public RotatingAxleBlock() {
        super(BlockBehaviour.Properties.of());
    }

    @Override
    public Vec3 getBase(BlockPos blockPos, BlockState blockState) {
        return blockPos.getCenter().add(0.5, 0.5, 0.5);
    }

    @Override
    public Vec3 getAxis(BlockPos blockPos, BlockState blockState) {
        return switch (blockState.getValue(FACING)) {
            case UP -> new Vec3(0, 1, 0);
            case DOWN -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(-1, 0, 0);
            case NORTH -> new Vec3(0, 0, 1);
            case SOUTH -> new Vec3(0, 0, -1);
        };
    }

    @Override
    public BlockPos getFirstBlock(BlockPos blockPos, BlockState blockState) {
        return switch (blockState.getValue(FACING)) {
            case UP -> blockPos.above();
            case DOWN -> blockPos.below();
            case EAST -> blockPos.east();
            case WEST -> blockPos.west();
            case NORTH -> blockPos.north();
            case SOUTH -> blockPos.south();
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
