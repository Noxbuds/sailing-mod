package com.noxbuds.sailing.network;

import com.noxbuds.sailing.client.BoatBlockPacketHandler;
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

import java.nio.charset.Charset;
import java.util.function.Supplier;

public record BoatBlockMessage(int entityId, BlockPos position, BlockState state) {

    public static void encode(final BoatBlockMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeBlockPos(message.position);

        CompoundTag nbt = NbtUtils.writeBlockState(message.state());
        buffer.writeNbt(nbt);
    }

    public static BoatBlockMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        BlockPos position = buffer.readBlockPos();

        CompoundTag nbt = buffer.readNbt();
        HolderGetter<Block> holder = Minecraft.getInstance().level.holderLookup(ForgeRegistries.BLOCKS.getRegistryKey());
        BlockState state = NbtUtils.readBlockState(holder, nbt);

        return new BoatBlockMessage(entityId, position, state);
    }

    public static void handle(final BoatBlockMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatBlockPacketHandler.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
