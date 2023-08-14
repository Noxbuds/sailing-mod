package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.BlockMass;
import com.noxbuds.sailing.SailingMod;
import com.noxbuds.sailing.block.BoatControlBlock;
import com.noxbuds.sailing.block.HelmBlock;
import com.noxbuds.sailing.block.RiggingBlockEntity;
import com.noxbuds.sailing.block.RotatingBlock;
import com.noxbuds.sailing.network.BoatBlockMessage;
import com.noxbuds.sailing.network.BoatComponentMessage;
import com.noxbuds.sailing.network.BoatRequestMessage;
import com.noxbuds.sailing.network.BoatRiggingMessage;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.*;

public class EntityBoat extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Vector3f> MIN_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> MAX_POS = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(EntityBoat.class, EntityDataSerializers.QUATERNION);

    private HashMap<BlockPos, BoatBlockContainer> blocks;
    private float totalMass;
    private ArrayList<BlockFace> blockFaces;
    private BoatPhysicsHandler physicsHandler;
    private ArrayList<RotatingComponent> rotatingComponents;
    private HashMap<BlockPos, RiggingLine> riggingLines; // block position is the winch block
    private BoatCollisionHandler collisionHandler;
    private BoatControlHandler controlHandler;

    public EntityBoat(EntityType<? extends EntityBoat> type, Level level) {
        super(type, level);
        this.blocks = new HashMap<>();
        this.rotatingComponents = new ArrayList<>();
        this.riggingLines = new HashMap<>();
        this.totalMass = 0f;
        this.blockFaces = new ArrayList<>();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 minPos = this.boatToWorld(new Vec3(this.getMinPosition()));
        Vec3 maxPos = this.boatToWorld(new Vec3(this.getMaxPosition()));

        return new AABB(minPos.x, minPos.y, minPos.z, maxPos.x, maxPos.y, maxPos.z);
    }

    private void updateBoundingBox() {
        Vec3 minPos = this.boatToWorld(new Vec3(this.getMinPosition()));
        Vec3 maxPos = this.boatToWorld(new Vec3(this.getMaxPosition()));

        AABB aabb = new AABB(minPos.x, minPos.y, minPos.z, maxPos.x, maxPos.y, maxPos.z);
        aabb = aabb.inflate(1);

        this.setBoundingBox(aabb);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    private BlockPos raycast(Player player) {
        // TODO: this algorithm is quite slow - fine for now but maybe worth improving later
        float maxDistance = 5f;
        Vec3 minPosition = new Vec3(this.getMinPosition());
        Vec3 direction = player.getLookAngle();
        Vec3 position = player.getPosition(0);

        float playerHeight = player.getEyeHeight();
        position = position.add(0, playerHeight, 0);
        position = this.worldToBoat(position);
        position = position.subtract(minPosition);

        float distance = 0;
        Vec3 gridPosition = new Vec3(Math.floor(position.x), Math.floor(position.y), Math.floor(position.z));

        while (distance < maxDistance) {

            BlockPos blockPos = new BlockPos((int) gridPosition.x, (int) gridPosition.y, (int) gridPosition.z);
            BoatBlockContainer container = this.blocks.get(blockPos);
            if (container != null) {
                return blockPos;
            }

            distance += 0.05f;
            gridPosition = position.add(direction.scale(distance));
            gridPosition = gridPosition.subtract(0.5, 0.5, 0.5);
            gridPosition = new Vec3(Math.floor(gridPosition.x), Math.floor(gridPosition.y), Math.floor(gridPosition.z));
        }

        return null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            BlockPos blockPos = raycast(player);

            if (blockPos == null) {
                return InteractionResult.FAIL;
            }

            BlockState blockState = this.blocks.get(blockPos).blockState();
            System.out.println(blockState);
            if (blockState.getBlock() instanceof BoatControlBlock && this.controlHandler != null) {
                player.startRiding(this);
                this.controlHandler.addController(player, blockPos);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void removePassenger(Entity entity) {
        super.removePassenger(entity);
        if (entity instanceof Player) {
            this.controlHandler.removeController((Player) entity);
        }
    }

    @Override
    public void tick() {
        // tick rate is 20/s
        float dt = 1 / 20f;

        if (this.totalMass == 0f) {
            this.calculateTotalMass();
        }

        if (!level().isClientSide() && this.physicsHandler != null) {
            for (RiggingLine line : this.riggingLines.values()) {
                line.applyForces(this);
            }

            for (RotatingComponent component : this.rotatingComponents) {
                component.update(dt);
            }

            this.physicsHandler.update(dt);
            this.syncPhysicsState();
        }

        this.updateBoundingBox();

        if (this.collisionHandler != null) {
            Vec3 minPos = this.boatToWorld(new Vec3(this.getMinPosition()));
            Vec3 maxPos = this.boatToWorld(new Vec3(this.getMaxPosition()));
            AABB boundingBox = new AABB(minPos.x, minPos.y, minPos.z, maxPos.x, maxPos.y, maxPos.z);
            boundingBox = boundingBox.inflate(5);

            List<Entity> entities = level().getEntities(this, boundingBox);
            for (Entity entity : entities) {
                entity.setNoGravity(false);
                this.collisionHandler.handleCollision(entity);
            }
        }

        if (!level().isClientSide() && this.controlHandler != null) {
            this.controlHandler.update(dt);
        }
    }

    private Vec3 calculateCentreOfMass(HashMap<BlockPos, BoatBlockContainer> blocks) {
        Vec3 centre = Vec3.ZERO;
        float totalMass = 0;

        for (BlockPos blockPos : blocks.keySet()) {
            float mass = BlockMass.get(blocks.get(blockPos).blockState());
            centre = centre.add(blockPos.getCenter().scale(mass));
            totalMass += mass;
        }

        return centre.scale(1f / totalMass);
    }

    private void calculateTotalMass() {
        this.totalMass = 0f;

        for (BlockPos blockPos : this.blocks.keySet()) {
            this.totalMass += BlockMass.get(this.blocks.get(blockPos).blockState());
        }
    }

    // TODO: mark faces as exposed or not
    private void setBlockFaces() {
        ArrayList<BlockFace> faces = new ArrayList<>();

        HashMap<BlockPos, BoatBlockContainer> blocks = this.getBlocks();
        for (BlockPos blockPos : this.getBlocks().keySet()) {
            Vec3 position = blockPos.getCenter()
                .add(new Vec3(this.getMinPosition()));

            BlockPos[] neighbours = {
                blockPos.above(),
                blockPos.below(),
                blockPos.west(),
                blockPos.east(),
                blockPos.south(),
                blockPos.north(),
            };

            Vec3[] normals = {
                new Vec3(0, 1, 0),
                new Vec3(0, -1, 0),
                new Vec3(1, 0, 0),
                new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1),
                new Vec3(0, 0, -1),
            };

            for (int i = 0; i < neighbours.length; i++) {
                if (!blocks.containsKey(neighbours[i])) {
                    float airDragFactor = 1e2f;
                    float waterDragFactor = 1e4f;

                    RotatingComponent component = this.getRotatingComponent(blockPos);

                    Vec3 edgePosition = position.add(normals[i].scale(0.5f));
                    faces.add(new BlockFace(blockPos, component, edgePosition, normals[i], airDragFactor, waterDragFactor));
                }
            }
        }

        this.blockFaces = faces;
    }

    // Takes a set of block world positions and block states, and puts them into the block positions map with
    // positions relative to the boat. Should only be called when server is spawning the boat.
    public void setBlocks(HashMap<BlockPos, BoatBlockContainer> blocks) {
        Vec3 centreOfMass = calculateCentreOfMass(blocks);
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
            this.blocks.put(new BlockPos(newPos), blocks.get(position));
        }

        this.attachHandlers();

        for (BlockPos blockPos : blocks.keySet()) {
            BlockEntity blockEntity = level().getBlockEntity(blockPos);
            if (blockEntity instanceof RiggingBlockEntity riggingBlockEntity) {
                Optional<RiggingLine> riggingLine = riggingBlockEntity.getRiggingLine();
                final Vec3i min = new Vec3i(-minX, -minY, -minZ);

                riggingLine.ifPresent(line -> {
                    line.offsetPositions(min);
                    this.riggingLines.put(line.getWinchPos(), line);
                });
            }
        }
    }

    // Called from the server to sync the ship data with clients
    public void syncData() {
        // TODO: only send to client requesting
        for (BlockPos pos : this.blocks.keySet()) {
            BoatBlockMessage message = new BoatBlockMessage(this.getId(), pos, this.blocks.get(pos));
            SailingMod.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
        }

        for (RiggingLine line : this.riggingLines.values()) {
            BoatRiggingMessage message = new BoatRiggingMessage(this.getId(), line);
            SailingMod.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
        }
    }

    private void syncPhysicsState() {
        int[] componentIds = new int[this.rotatingComponents.size()];
        float[] rotations = new float[this.rotatingComponents.size()];

        for (int i = 0; i < componentIds.length; i++) {
            RotatingComponent component = this.rotatingComponents.get(i);
            componentIds[i] = i;
            rotations[i] = component.getRotation();
        }

        BoatComponentMessage message = new BoatComponentMessage(this.getId(), componentIds, rotations);
        SailingMod.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    // This is called by the packet handler when a block packet is received on the client
    public void updateBlock(BlockPos position, BoatBlockContainer state) {
        this.blocks.put(position, state);
        this.attachHandlers();
    }

    public void updateComponents(int[] componentIds, float[] rotations) {
        for (int i = 0; i < componentIds.length; i++) {
            RotatingComponent component = this.getRotatingComponent(componentIds[i]);
            component.setRotation(rotations[i]);
        }
    }

    public void addRiggingLine(RiggingLine line) {
        this.riggingLines.put(line.getWinchPos(), line);
    }

    private void findRotatingComponents() {
        ArrayList<BlockPos> basePositions = new ArrayList<>();
        this.rotatingComponents = new ArrayList<>();

        for (BlockPos blockPos : this.blocks.keySet()) {
            BlockState blockState = this.blocks.get(blockPos).blockState();
            if (blockState.getBlock() instanceof RotatingBlock) {
                RotatingBlock block = (RotatingBlock) blockState.getBlock();
                Vec3 base = block.getBase(blockPos, blockState).add(new Vec3(this.getMinPosition()));
                Vec3 axis = block.getAxis(blockPos, blockState);

                basePositions.add(block.getFirstBlock(blockPos, blockState));
                this.rotatingComponents.add(new RotatingComponent(base, axis));
            }
        }

        for (int i = 0; i < this.rotatingComponents.size(); i++) {
            ArrayList<BlockPos> queue = new ArrayList<>();
            queue.add(basePositions.get(i));

            while (!queue.isEmpty()) {
                BlockPos next = queue.remove(0);
                BlockPos[] neighbours = { next.above(), next.below(), next.north(), next.south(), next.east(), next.west() };

                for (BlockPos neighbour: neighbours) {
                    BoatBlockContainer container = this.blocks.get(neighbour);

                    if (container == null || container.blockState().getBlock() instanceof RotatingBlock || container.componentId().isPresent()) {
                        continue;
                    }

                    BoatBlockContainer newContainer = new BoatBlockContainer(container.blockState(), Optional.of(i));
                    this.blocks.put(neighbour, newContainer);
                    queue.add(neighbour);
                }
            }
        }
    }

    public RotatingComponent getRotatingComponent(int index) {
        if (index < this.rotatingComponents.size())
            return this.rotatingComponents.get(index);

        return null;
    }

    public RotatingComponent getRotatingComponent(BlockPos blockPos) {
        BoatBlockContainer container = this.blocks.get(blockPos);
        if (container == null || container.componentId().isEmpty()) {
            return null;
        }

        int index = container.componentId().get();
        if (index < this.rotatingComponents.size())
            return this.rotatingComponents.get(index);

        return null;
    }

    private void attachHandlers() {
        this.findRotatingComponents();
        this.setBlockFaces();
        this.calculateTotalMass();

        this.physicsHandler = new BoatPhysicsHandler(this);

        this.collisionHandler = new BoatCollisionHandler(this);
        this.updateBoundingBox();

        this.controlHandler = new BoatControlHandler(this);
    }

    public Vec3 boatToWorld(Vec3 position) {
        Vector4f rotated = new Vector4f((float) position.x, (float) position.y, (float) position.z, 1f);
        Quaternionf rotation = this.getRotation();

        rotated = rotation.transform(rotated);

        Vec3 newPosition = new Vec3(rotated.x, rotated.y, rotated.z);
        return newPosition.add(position());
    }

    public Vec3 boatToWorld(BlockPos blockPos) {
        Vec3 position = blockPos.getCenter().add(new Vec3(getMinPosition()));
        Vector4f transformed = new Vector4f((float) position.x, (float) position.y, (float) position.z, 1f);

        Quaternionf rotation = this.getRotation();
        transformed = rotation.transform(transformed);

        RotatingComponent component = this.getRotatingComponent(blockPos);
        if (component != null) {
            Matrix4f matrix = component.getTransformationMatrix();
            transformed = matrix.transform(transformed);
        }

        Vec3 newPosition = new Vec3(transformed.x, transformed.y, transformed.z);
        return newPosition.add(position());
    }

    public Vec3 worldToBoat(Vec3 position) {
        Vec3 relative = position.subtract(position());
        Quaternionf rotation = this.getRotation();

        Vector3f rotated = rotation.transformInverse(relative.toVector3f());
        return new Vec3(rotated);
    }

    public HashMap<BlockPos, BoatBlockContainer> getBlocks() {
        return this.blocks;
    }

    public BoatBlockContainer getBlock(BlockPos blockPos) {
        return this.blocks.get(blockPos);
    }

    public RiggingLine getRiggingLine(BlockPos blockPos) {
        return this.riggingLines.get(blockPos);
    }

    public float getTotalMass() {
        return this.totalMass;
    }

    public ArrayList<BlockFace> getBlockFaces() {
        return this.blockFaces;
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

    public BoatPhysicsHandler getPhysicsHandler() {
        return this.physicsHandler;
    }

    public BoatControlHandler getControlHandler() {
        return this.controlHandler;
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

        this.blocks = new HashMap<>();

        byte[] bytes = nbt.getByteArray("blockPositions");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        while (buffer.hasRemaining()) {
            int x = buffer.getInt();
            int y = buffer.getInt();
            int z = buffer.getInt();

            BlockPos position = new BlockPos(x, y, z);

            CompoundTag stateNbt = nbt.getCompound(position.toShortString() + ":state");
            HolderGetter<Block> holder = level().holderLookup(ForgeRegistries.BLOCKS.getRegistryKey());
            BlockState state = NbtUtils.readBlockState(holder, stateNbt);

            Optional<Integer> componentId = Optional.empty();
            if (nbt.contains(position.toShortString() + ":componentId")) {
                componentId = Optional.of(nbt.getInt(position.toShortString() + ":componentId"));
            }

            this.blocks.put(position, new BoatBlockContainer(state, componentId));
        }

        int numLines = nbt.getInt("lineCount");
        for (int i = 0; i < numLines; i++) {
            CompoundTag lineNbt = nbt.getCompound("line" + i);
            RiggingLine line = new RiggingLine(lineNbt);
            this.riggingLines.put(line.getWinchPos(), line);
        }

        this.attachHandlers();
        this.physicsHandler.readData(nbt);
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

        ByteBuffer buffer = ByteBuffer.allocate(this.blocks.size() * 12);

        for (BlockPos pos : this.blocks.keySet()) {
            buffer.putInt(pos.getX());
            buffer.putInt(pos.getY());
            buffer.putInt(pos.getZ());
        }

        nbt.putByteArray("blockPositions", buffer.array());

        for (BlockPos pos : this.blocks.keySet()) {
            BoatBlockContainer container = this.blocks.get(pos);

            CompoundTag stateNbt = NbtUtils.writeBlockState(container.blockState());
            nbt.put(pos.toShortString() + ":state", stateNbt);

            if (container.componentId().isPresent()) {
                nbt.putInt(pos.toShortString() + ":componentId", container.componentId().get());
            }
        }

        nbt.putInt("lineCount", this.riggingLines.size());
        int lineId = 0;
        for (RiggingLine line : this.riggingLines.values()) {
            CompoundTag lineNbt = line.getNBT();
            nbt.put("line" + lineId, lineNbt);
            lineId++;
        }

        this.physicsHandler.writeData(nbt);
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
