package com.noxbuds.sailing.network;

import com.noxbuds.sailing.client.BoatBlockPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BoatBlockMessage(int entityId, BlockPos position) {

    public static void encode(final BoatBlockMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeBlockPos(message.position);
    }

    public static BoatBlockMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        BlockPos position = buffer.readBlockPos();

        return new BoatBlockMessage(entityId, position);
    }

    public static void handle(final BoatBlockMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatBlockPacketHandler.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
