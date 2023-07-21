package com.noxbuds.sailing.client;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SailingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterRenderers {
    @SubscribeEvent
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BOAT_TYPE.get(), BoatRenderer::new);
    }
}
