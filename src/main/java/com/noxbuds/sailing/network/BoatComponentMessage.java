package com.noxbuds.sailing.network;

import com.noxbuds.sailing.boat.BoatBlockContainer;
import com.noxbuds.sailing.boat.RotatingComponent;
import com.noxbuds.sailing.client.network.BoatBlockPacketHandler;
import com.noxbuds.sailing.client.network.BoatComponentPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

public record BoatComponentMessage(int entityId, int[] componentIds, float[] rotations) {
    public static void encode(final BoatComponentMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeInt(message.componentIds.length);

        for (int i = 0; i < message.componentIds.length; i++) {
            buffer.writeInt(message.componentIds[i]);
            buffer.writeFloat(message.rotations[i]);
        }
    }

    public static BoatComponentMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        int numComponents = buffer.readInt();

        int[] componentIds = new int[numComponents];
        float[] rotations = new float[numComponents];

        for (int i = 0; i < numComponents; i++) {
            componentIds[i] = buffer.readInt();
            rotations[i] = buffer.readFloat();
        }

        return new BoatComponentMessage(entityId, componentIds, rotations);
    }

    public static void handle(final BoatComponentMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatComponentPacketHandler.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
