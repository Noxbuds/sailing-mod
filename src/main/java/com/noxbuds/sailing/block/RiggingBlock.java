package com.noxbuds.sailing.block;

import com.noxbuds.sailing.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RiggingBlock extends Block implements EntityBlock {
    public RiggingBlock() {
        super(BlockBehaviour.Properties.of());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlocks.RIGGING_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
}
