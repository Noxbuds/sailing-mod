package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.network.BoatBlockMessage;
import com.noxbuds.sailing.network.BoatRequestMessage;
import com.noxbuds.sailing.network.SailingNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

public class EntityBoat extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Vector3f> MIN_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> MAX_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);

    private HashMap<BlockPos, BlockState> blockPositions;

    public EntityBoat(EntityType<? extends EntityBoat> type, Level level) {
        super(type, level);
        this.blockPositions = new HashMap<>();
    }

    private Vec3 calculateCentreOfMass(Set<BlockPos> positions) {
        //TODO: calculate mass of blocks and use it to weight position
        Vec3 centre = Vec3.ZERO;

        for (BlockPos pos : positions) {
            centre = centre.add(pos.getCenter());
        }

        centre = centre.scale(1f / positions.size());
        return centre;
    }

    public void setBlocks(HashMap<BlockPos, BlockState> blocks) {
        Vec3 centreOfMass = calculateCentreOfMass(blocks.keySet());
        this.setPos(centreOfMass);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        for (BlockPos pos : blocks.keySet()) {
            if (pos.getX() < minX)
                minX = pos.getX();

            if (pos.getY() < minY)
                minY = pos.getY();

            if (pos.getZ() < minZ)
                minZ = pos.getZ();
        }

        Vector3f minPos = new Vector3f(minX, minY, minZ);
        minPos = minPos.sub(centreOfMass.toVector3f());
        minPos = minPos.sub(new Vector3f(0.5f));

        this.entityData.set(MIN_POS, minPos);

        for (BlockPos position : blocks.keySet()) {
            Vec3i newPos = position.subtract(new Vec3i(minX, minY, minZ));
            this.blockPositions.put(new BlockPos(newPos), blocks.get(position));
        }
    }

    // Called from the server to sync the ship data with clients
    public void syncData() {
        for (BlockPos pos : this.blockPositions.keySet()) {
            BoatBlockMessage message = new BoatBlockMessage(this.getId(), pos, this.blockPositions.get(pos));
            SailingMod.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
        }
    }

    // This is called by the packet handler when a block packet is received on the client
    public void updateBlock(BlockPos position, BlockState state) {
        this.blockPositions.put(position, state);
    }


    public HashMap<BlockPos, BlockState> getBlockPositions() {
        return this.blockPositions;
    }

    public Vector3f getMinPosition() {
        return this.entityData.get(MIN_POS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MIN_POS, new Vector3f(0));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        float minX = nbt.getFloat("minX");
        float minY = nbt.getFloat("minY");
        float minZ = nbt.getFloat("minZ");
        this.entityData.set(MIN_POS, new Vector3f(minX, minY, minZ));

        this.blockPositions = new HashMap<>();

        byte[] bytes = nbt.getByteArray("blockPositions");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        while (buffer.hasRemaining()) {
            int x = buffer.getInt();
            int y = buffer.getInt();
            int z = buffer.getInt();

            BlockPos position = new BlockPos(x, y, z);

            CompoundTag stateNbt = nbt.getCompound(position.toShortString());
            HolderGetter<Block> holder = level().holderLookup(ForgeRegistries.BLOCKS.getRegistryKey());
            BlockState state = NbtUtils.readBlockState(holder, stateNbt);

            this.blockPositions.put(position, state);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        Vector3f minPos = getMinPosition();
        System.out.println("Writing min position: " + minPos);
        nbt.putFloat("minX", minPos.x);
        nbt.putFloat("minY", minPos.y);
        nbt.putFloat("minZ", minPos.z);

        ByteBuffer buffer = ByteBuffer.allocate(this.blockPositions.size() * 12);

        for (BlockPos pos : this.blockPositions.keySet()) {
            buffer.putInt(pos.getX());
            buffer.putInt(pos.getY());
            buffer.putInt(pos.getZ());
        }

        nbt.putByteArray("blockPositions", buffer.array());

        for (BlockPos pos : this.blockPositions.keySet()) {
            CompoundTag stateNbt = NbtUtils.writeBlockState(this.blockPositions.get(pos));
            nbt.put(pos.toShortString(), stateNbt);
        }
    }

    // The following code is a bit naff, but this is the only way I found to sync entity IDs and then send a request
    // to the server for the boat's data. Requesting on entity spawn results in a de-synced ID being sent.
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        if (level().isClientSide()) {
            SailingMod.CHANNEL.sendToServer(new BoatRequestMessage(getId()));
        }
    }
}
