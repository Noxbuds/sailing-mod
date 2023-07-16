package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.network.BoatBlockMessage;
import com.noxbuds.sailing.network.BoatRequestMessage;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

public class EntityBoat extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Vector3f> MIN_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> MAX_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.QUATERNION);

    private HashMap<BlockPos, BlockState> blockPositions;
    private BoatPhysicsHandler physicsHandler;

    public EntityBoat(EntityType<? extends EntityBoat> type, Level level) {
        super(type, level);
        this.blockPositions = new HashMap<>();
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && this.physicsHandler != null) {
            // tick rate is 20/s
            float dt = 1 / 20f;
            this.physicsHandler.update(dt);
        }
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

    // Takes a set of block world positions and block states, and puts them into the block positions map with
    // positions relative to the boat. Should only be called when server is spawning the boat.
    public void setBlocks(HashMap<BlockPos, BlockState> blocks) {
        Vec3 centreOfMass = calculateCentreOfMass(blocks.keySet());
        this.setPos(centreOfMass.subtract(0.5, 0.5, 0.5));

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks.keySet()) {
            if (pos.getX() < minX)
                minX = pos.getX();
            if (pos.getX() > maxX)
                maxX = pos.getX();

            if (pos.getY() < minY)
                minY = pos.getY();
            if (pos.getY() > maxY)
                maxY = pos.getY();

            if (pos.getZ() < minZ)
                minZ = pos.getZ();
            if (pos.getZ() > maxZ)
                maxZ = pos.getZ();
        }

        Vector3f minPos = new Vector3f(minX, minY, minZ);
        minPos = minPos.sub(centreOfMass.toVector3f());

        Vector3f maxPos = new Vector3f(maxX, maxY, maxZ);
        maxPos = maxPos.sub(centreOfMass.toVector3f());

        this.entityData.set(MIN_POS, minPos);
        this.entityData.set(MAX_POS, maxPos);

        for (BlockPos position : blocks.keySet()) {
            Vec3i newPos = position.subtract(new Vec3i(minX, minY, minZ));
            this.blockPositions.put(new BlockPos(newPos), blocks.get(position));
        }

        this.physicsHandler = new BoatPhysicsHandler(this);
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

    public Vector3f getMaxPosition() {
        return this.entityData.get(MAX_POS);
    }

    public Quaternionf getRotation() {
        return this.entityData.get(ROTATION);
    }

    public void setRotation(Quaternionf rotation) {
        this.entityData.set(ROTATION, rotation, true);
    }

    @Override
    public boolean shouldRender(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MIN_POS, new Vector3f(0));
        this.entityData.define(MAX_POS, new Vector3f(0));
        this.entityData.define(ROTATION, new Quaternionf().identity());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        float minX = nbt.getFloat("minX");
        float minY = nbt.getFloat("minY");
        float minZ = nbt.getFloat("minZ");
        this.entityData.set(MIN_POS, new Vector3f(minX, minY, minZ));

        float maxX = nbt.getFloat("maxX");
        float maxY = nbt.getFloat("maxY");
        float maxZ = nbt.getFloat("maxZ");
        this.entityData.set(MAX_POS, new Vector3f(maxX, maxY, maxZ));

        float quaternionX = nbt.getFloat("quaternionX");
        float quaternionY = nbt.getFloat("quaternionY");
        float quaternionZ = nbt.getFloat("quaternionZ");
        float quaternionW = nbt.getFloat("quaternionW");
        this.entityData.set(ROTATION, new Quaternionf(quaternionX, quaternionY, quaternionZ, quaternionW), true);

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

        this.physicsHandler = new BoatPhysicsHandler(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        Vector3f minPos = this.entityData.get(MIN_POS);
        nbt.putFloat("minX", minPos.x);
        nbt.putFloat("minY", minPos.y);
        nbt.putFloat("minZ", minPos.z);

        Vector3f maxPos = this.entityData.get(MAX_POS);
        nbt.putFloat("maxX", maxPos.x);
        nbt.putFloat("maxY", maxPos.y);
        nbt.putFloat("maxZ", maxPos.z);

        Quaternionf quaternion = this.entityData.get(ROTATION);
        nbt.putFloat("quaternionX", quaternion.x);
        nbt.putFloat("quaternionY", quaternion.y);
        nbt.putFloat("quaternionZ", quaternion.z);
        nbt.putFloat("quaternionW", quaternion.w);

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
