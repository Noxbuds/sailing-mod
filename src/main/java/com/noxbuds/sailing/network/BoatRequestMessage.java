package com.noxbuds.sailing.network;

import com.noxbuds.sailing.boat.EntityBoat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BoatRequestMessage(int entityId) {
    public static void encode(final BoatRequestMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
    }

    public static BoatRequestMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        return new BoatRequestMessage(entityId);
    }

    public static void handle(final BoatRequestMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();

            if (player == null) {
                return;
            }

            Level level = player.level();
            EntityBoat boat = (EntityBoat) level.getEntity(message.entityId());

            if (boat == null) {
                return;
            }

            boat.syncData();
        });
        ctx.get().setPacketHandled(true);
    }
}
