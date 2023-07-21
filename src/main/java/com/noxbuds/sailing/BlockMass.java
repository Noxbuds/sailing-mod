package com.noxbuds.sailing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class BlockMass {
    public static final float WATER_MASS = 1000f;
    private static final HashMap<Block, Float> MASSES = getMassMap();

    private static HashMap<Block, Float> getMassMap() {
        HashMap<Block, Float> masses = new HashMap<>();

        // should be the mass of 1m^3 of the material - for reference, water is ~1000
        masses.put(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "oak_planks")), 500f);
        masses.put(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "stone")), 1500f);

        return masses;
    }

    public static float get(BlockState blockState) {
        Block block = blockState.getBlock();
        return MASSES.getOrDefault(block, WATER_MASS);
    }
}
