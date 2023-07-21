package com.noxbuds.sailing.block;

import com.mojang.logging.LogUtils;
import com.noxbuds.sailing.boat.EntityBoat;
import com.noxbuds.sailing.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class HelmBlock extends Block {
    // TODO: make this configurable
    private static final int MAX_CONNECTED_BLOCKS = 1000;
    private Logger logger = LogUtils.getLogger();

    public HelmBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    }

    private HashMap<BlockPos, BlockState> getConnectedBlocks(Level level, BlockPos pos) {
        HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        ArrayList<BlockPos> queue = new ArrayList<>();

        queue.add(pos);

        while (!queue.isEmpty() && blocks.size() < MAX_CONNECTED_BLOCKS) {
            BlockPos next = queue.remove(0);
            BlockPos[] neighbours = { next.above(), next.below(), next.north(), next.south(), next.east(), next.west() };

            for (BlockPos neighbour: neighbours) {
                BlockState state = level.getBlockState(neighbour);

                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    continue;
                }

                if (!blocks.containsKey(neighbour)) {
                    blocks.put(neighbour, state);
                    queue.add(neighbour);
                }
            }
        }

        return blocks;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (!level.isClientSide) {
            HashMap<BlockPos, BlockState> connected = getConnectedBlocks(level, pos);

            EntityBoat boat = new EntityBoat(ModEntities.BOAT_TYPE.get(), level);
            boat.setBlocks(connected);

            level.addFreshEntity(boat);

            for (BlockPos position : connected.keySet()) {
                level.removeBlock(position, false);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
