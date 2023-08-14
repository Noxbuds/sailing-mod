package com.noxbuds.sailing.registry;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.item.ItemRiggingLine;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SailingMod.MODID);

    public static final RegistryObject<Item> RIGGING_LINE = ITEMS.register("rigging_line", () -> new ItemRiggingLine(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
