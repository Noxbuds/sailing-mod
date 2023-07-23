package com.noxbuds.sailing.boat;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record BoatBlockContainer(BlockState blockState, Optional<Integer> componentId) {

}
