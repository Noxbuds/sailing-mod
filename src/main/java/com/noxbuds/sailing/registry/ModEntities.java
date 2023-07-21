package com.noxbuds.sailing.registry;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.boat.EntityBoat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SailingMod.MODID);

    public static final RegistryObject<EntityType<EntityBoat>> BOAT_TYPE = ENTITIES.register("boat", () -> EntityType.Builder.of(EntityBoat::new, MobCategory.MISC)
        .updateInterval(1)
        .build("boat"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
