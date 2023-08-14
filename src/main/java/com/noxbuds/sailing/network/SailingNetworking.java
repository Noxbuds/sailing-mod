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

        channel.messageBuilder(BoatRequestMessage.class, 1)
            .decoder(BoatRequestMessage::decode)
            .encoder(BoatRequestMessage::encode)
            .consumerMainThread(BoatRequestMessage::handle)
            .add();

        channel.messageBuilder(BoatBlockMessage.class, 2)
            .decoder(BoatBlockMessage::decode)
            .encoder(BoatBlockMessage::encode)
            .consumerMainThread(BoatBlockMessage::handle)
            .add();

        channel.messageBuilder(BoatRiggingMessage.class, 3)
            .decoder(BoatRiggingMessage::decode)
            .encoder(BoatRiggingMessage::encode)
            .consumerMainThread(BoatRiggingMessage::handle)
            .add();

        channel.messageBuilder(BoatComponentMessage.class, 4)
            .decoder(BoatComponentMessage::decode)
            .encoder(BoatComponentMessage::encode)
            .consumerMainThread(BoatComponentMessage::handle)
            .add();

        return channel;
    }

}
