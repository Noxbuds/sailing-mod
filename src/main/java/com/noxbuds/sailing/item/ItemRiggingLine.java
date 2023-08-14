package com.noxbuds.sailing.item;

import com.noxbuds.sailing.block.RiggingBlockEntity;
import com.noxbuds.sailing.boat.RiggingLine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class ItemRiggingLine extends Item {
    private BlockPos storedBlockPos;

    public ItemRiggingLine(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos blockPos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        if (!(blockEntity instanceof RiggingBlockEntity riggingBlockEntity)) {
            return InteractionResult.PASS;
        }

        if (this.storedBlockPos == null) {
            this.storedBlockPos = blockPos;
            return InteractionResult.SUCCESS;
        }

        RiggingLine line = new RiggingLine(this.storedBlockPos, blockPos);
        riggingBlockEntity.setRiggingLine(line);

        this.storedBlockPos = null;

        return InteractionResult.SUCCESS;
    }
}
