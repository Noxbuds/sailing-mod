package com.noxbuds.sailing.network;

import com.noxbuds.sailing.boat.RiggingLine;
import com.noxbuds.sailing.client.network.BoatRiggingPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BoatRiggingMessage(int entityId, RiggingLine line) {
    public static void encode(final BoatRiggingMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeNbt(message.line.getNBT());
    }

    public static BoatRiggingMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        CompoundTag nbt = buffer.readNbt();
        return new BoatRiggingMessage(entityId, new RiggingLine(nbt));
    }

    public static void handle(final BoatRiggingMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatRiggingPacketHandler.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
