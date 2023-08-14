package com.noxbuds.sailing.registry;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SailingMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SailingMod.MODID);

    public static final RegistryObject<Block> HELM_BLOCK = registerBlock("helm_block", HelmBlock::new);
    public static final RegistryObject<Block> PROPELLER_BLOCK = registerBlock("propeller_block", PropellerBlock::new);
    public static final RegistryObject<Block> AXLE_BLOCK = registerBlock("axle_block", RotatingAxleBlock::new);

    public static final RegistryObject<Block> RIGGING_BLOCK = registerBlock("rigging_block", RiggingBlock::new);
    public static final RegistryObject<Block> WINCH_BLOCK = registerBlock("winch_block", WinchBlock::new);
    public static final RegistryObject<Block> RUDDER_BLOCK = registerBlock("rudder_block", RudderBlock::new);
    public static final RegistryObject<BlockEntityType<RiggingBlockEntity>> RIGGING_BLOCK_ENTITY = BLOCK_ENTITIES.register("rigging_block_entity",
        () -> BlockEntityType.Builder.of(RiggingBlockEntity::new, ModBlocks.RIGGING_BLOCK.get()).build(null)
    );

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> supplier) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, supplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), new Item.Properties()));
        return registeredBlock;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
