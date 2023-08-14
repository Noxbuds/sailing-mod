package com.noxbuds.sailing.client.network;

import com.noxbuds.sailing.boat.EntityBoat;
import com.noxbuds.sailing.network.BoatRiggingMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BoatRiggingPacketHandler {
    public static void handlePacket(final BoatRiggingMessage message, final Supplier<NetworkEvent.Context> ctx) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        Level level = player.level();

        EntityBoat boat = (EntityBoat)level.getEntity(message.entityId());

        if (boat == null || !level.isClientSide()) {
            return;
        }

        boat.addRiggingLine(message.line());
    }
}
