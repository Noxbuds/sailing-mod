package com.noxbuds.sailing.network;

import com.noxbuds.sailing.SailingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class SailingNetworking {
    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel getChannel() {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SailingMod.MODID, "blocks"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        channel.messageBuilder(BoatBlockMessage.class, 1)
            .decoder(BoatBlockMessage::decode)
            .encoder(BoatBlockMessage::encode)
            .consumerMainThread(BoatBlockMessage::handle)
            .add();

        channel.messageBuilder(BoatRequestMessage.class, 2)
            .decoder(BoatRequestMessage::decode)
            .encoder(BoatRequestMessage::encode)
            .consumerMainThread(BoatRequestMessage::handle)
            .add();

        return channel;
    }

}
