package com.noxbuds.sailing.block;

import com.mojang.logging.LogUtils;
import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.boat.EntityBoat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;

public class HelmBlock extends Block {
    private static final int MAX_CONNECTED_BLOCKS = 64;
    private Logger logger = LogUtils.getLogger();

    public HelmBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    }

    private ArrayList<BlockPos> getConnectedBlocks(Level level, BlockPos pos) {
        ArrayList<BlockPos> connected = new ArrayList<>();
        ArrayList<BlockPos> queue = new ArrayList<>();

        queue.add(pos);

        while (!queue.isEmpty() && connected.size() < MAX_CONNECTED_BLOCKS) {
            BlockPos next = queue.remove(0);
            BlockPos[] neighbours = { next.above(), next.below(), next.north(), next.south(), next.east(), next.west() };

            for (BlockPos neighbour: neighbours) {
                BlockState state = level.getBlockState(neighbour);
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    continue;
                }

                if (!connected.contains(neighbour)) {
                    connected.add(neighbour);
                    queue.add(neighbour);
                }
            }
        }

        logger.info("Found " + connected.size() + " connected blocks");
        return connected;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (!level.isClientSide) {
            ArrayList<BlockPos> connected = getConnectedBlocks(level, pos);

            EntityBoat boat = new EntityBoat(SailingMod.BOAT_TYPE.get(), level);
            boat.setBlocks(connected);
            boat.setPos(pos.above().above().getCenter());

            level.addFreshEntity(boat);

            // this has to be called after adding to get the correct entity ID
//            boat.syncData();
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
