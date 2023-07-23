package com.noxbuds.sailing.network;

import com.noxbuds.sailing.boat.BoatBlockContainer;
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
import java.util.Optional;
import java.util.function.Supplier;

public record BoatBlockMessage(int entityId, BlockPos position, BoatBlockContainer container) {

    public static void encode(final BoatBlockMessage message, final FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeBlockPos(message.position);

        Optional<Integer> componentId = message.container.componentId();
        if (componentId.isPresent()) {
            buffer.writeInt(componentId.get());
        } else {
            buffer.writeInt(-1);
        }

        CompoundTag nbt = NbtUtils.writeBlockState(message.container().blockState());
        buffer.writeNbt(nbt);
    }

    public static BoatBlockMessage decode(final FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        BlockPos position = buffer.readBlockPos();

        int componentId = buffer.readInt();
        Optional<Integer> componentIdOption = Optional.empty();
        if (componentId >= 0) {
            componentIdOption = Optional.of(componentId);
        }

        CompoundTag nbt = buffer.readNbt();
        HolderGetter<Block> holder = Minecraft.getInstance().level.holderLookup(ForgeRegistries.BLOCKS.getRegistryKey());
        BlockState state = NbtUtils.readBlockState(holder, nbt);

        BoatBlockContainer container = new BoatBlockContainer(state, componentIdOption);
        return new BoatBlockMessage(entityId, position, container);
    }

    public static void handle(final BoatBlockMessage message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoatBlockPacketHandler.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
