package com.noxbuds.sailing.block;

import com.noxbuds.sailing.boat.RiggingLine;
import com.noxbuds.sailing.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class RiggingBlockEntity extends BlockEntity {
    private RiggingLine riggingLine;

    public RiggingBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlocks.RIGGING_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public void setRiggingLine(RiggingLine riggingLine) {
        this.riggingLine = riggingLine;
    }

    public Optional<RiggingLine> getRiggingLine() {
        return Optional.ofNullable(this.riggingLine);
    }
}
