package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.BlockMass;
import com.noxbuds.sailing.block.PropellerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;

public class BoatPhysicsHandler {
    private final Vec3 gravity = new Vec3(0, -9.8, 0);
    private final EntityBoat boat;
    private float waterHeight;

    private final ArrayList<Vec3> forces;
    private final ArrayList<Vec3> forcePositions;

    private Vec3 position;
    private Vec3 oldPosition;

    private Quaternionf rotation;
    private Vector3f angularMomentum;
    private Vector3f angularVelocity;

    private final Matrix3f inverseInertia;

    public BoatPhysicsHandler(EntityBoat boat) {
        this.boat = boat;

        this.forces = new ArrayList<>();
        this.forcePositions = new ArrayList<>();

        this.position = new Vec3(boat.position().toVector3f());
        this.oldPosition = new Vec3(this.position.toVector3f());

        this.rotation = new Quaternionf(boat.getRotation());

        this.angularMomentum = Vec3.ZERO.toVector3f();
        this.angularVelocity = Vec3.ZERO.toVector3f();

        this.inverseInertia = getInertia().invert();
    }

    // Treating the boat as a set of particles where each particle is the centre of the block
    // See http://www.cs.cmu.edu/~baraff/sigcourse/notesd1.pdf
    private Matrix3f getInertia() {
        Matrix3f tensor = new Matrix3f();

        HashMap<BlockPos, BoatBlockContainer> blocks = boat.getBlocks();
        for (BlockPos blockPos : blocks.keySet()) {
            Vector3f dir = blockPos.getCenter().toVector3f().sub(boat.getMinPosition());

            // (r0^T r0)I
            Matrix3f leftSide = new Matrix3f().identity().scale(dir.dot(dir));

            // r0 r0^T
            Matrix3f rightSide = new Matrix3f(
                dir.x * dir.x, dir.x * dir.y, dir.x * dir.z,
                dir.y * dir.x, dir.y * dir.y, dir.y * dir.z,
                dir.z * dir.x, dir.z * dir.y, dir.z * dir.z
            ).transpose();

            float mass = BlockMass.get(blocks.get(blockPos).blockState());
            Matrix3f sum = leftSide.add(rightSide).scale(mass);
            tensor.add(sum);
        }

        return tensor;
    }

    private void calculateWaterHeight() {
        Vec3 minPos = boat.boatToWorld(new Vec3(boat.getMinPosition()));
        Vec3 maxPos = boat.boatToWorld(new Vec3(boat.getMaxPosition()));

        BlockPos minBlockPos = new BlockPos((int)minPos.x, (int)minPos.y, (int)minPos.z);
        BlockPos maxBlockPos = new BlockPos((int)maxPos.x, (int)maxPos.y, (int)maxPos.z);

        this.waterHeight = 0f;

        for (int x = minBlockPos.getX(); x <= maxBlockPos.getX(); x++) {
            for (int y = minBlockPos.getY(); y <= maxBlockPos.getY(); y++) {
                for (int z = minBlockPos.getZ(); z <= maxBlockPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = boat.level().getBlockState(pos);

                    if (!state.getFluidState().isEmpty() && y > this.waterHeight) {
                        this.waterHeight = y;
                    }
                }
            }
        }

        // we want the water height to be the top of the block, so add 1
        this.waterHeight += 1;
    }

    public Vec3 getVelocity() {
        return this.position.subtract(oldPosition);
    }

    public Vec3 getAngularMomentum() {
        return new Vec3(this.angularMomentum);
    }

    public Vec3 getAngularVelocity() {
        return new Vec3(this.angularVelocity);
    }

    public void addForce(Vec3 force, Vec3 position) {
        this.forces.add(force);
        this.forcePositions.add(position);
    }

    private void addForce(Vec3 force, BlockPos blockPos) {
        Vec3 position = blockPos.getCenter().add(new Vec3(this.boat.getMinPosition()));

        // TODO: apply to rotating component to simulate rudder effect, and add rotating component's angular velocity to drag
        this.forces.add(force);
        this.forcePositions.add(position);
    }

    private void calculateBuoyancy() {
        for (BlockPos blockPos : this.boat.getBlocks().keySet()) {
            Vec3 worldPosition = this.boat.boatToWorld(blockPos);

            // volume will be 1m x 1m x (y - waterHeight), so we can simplify to just use depth (capped to block height)
            double depth = -Math.max(Math.min(worldPosition.y - this.waterHeight, 0), -1f);
            double buoyancy = depth * BlockMass.WATER_MASS * Math.abs(this.gravity.y);
            Vec3 force = new Vec3(0, 1, 0).scale(buoyancy);

            this.addForce(force, blockPos);
        }
    }

    private void calculateDrag() {
        Vector3f velocity = this.position.toVector3f().sub(this.oldPosition.toVector3f());

        for (BlockFace face : this.boat.getBlockFaces()) {
            Vec3 boatPosition = face.getPosition();
            Vec3 worldPosition = this.boat.boatToWorld(boatPosition);
            boolean isSubmerged = worldPosition.y < this.waterHeight;

            Vec3 drag = face.getDrag(new Vec3(velocity), new Vec3(this.angularVelocity), isSubmerged);
            // TODO: the position is not quite correct
            this.addForce(drag, face.getBlockPos());
        }
    }

    private void calculateEngineForces() {
        // TODO: handle engine direction and force
        float engineForce = 10000f;
        BoatControlHandler controlHandler = this.boat.getControlHandler();
        if (controlHandler == null)
            return;

        HashMap<BlockPos, BoatBlockContainer> blocks = this.boat.getBlocks();
        for (BlockPos blockPos : blocks.keySet()) {
            Block block = blocks.get(blockPos).blockState().getBlock();
            if (block instanceof PropellerBlock) {
                Vec3 force = new Vec3(0, 0, engineForce * controlHandler.getEngineThrottle());
                this.addForce(force, blockPos);
            }
        }
    }

    private void resolveAcceleration(float dt) {
        Vec3 acceleration = this.gravity;

        for (Vec3 force : this.forces) {
            Vec3 deltaAcceleration = force.scale(1 / this.boat.getTotalMass());
            acceleration = acceleration.add(deltaAcceleration);
        }

        Vec3 newPosition = this.position.scale(2f)
            .subtract(this.oldPosition)
            .add(acceleration.scale(dt * dt));

        this.oldPosition = new Vec3(this.position.toVector3f());
        this.position = newPosition;
    }

    private void resolveAngularMomentum() {
        Vector3f torque = Vec3.ZERO.toVector3f(); // add small value to prevent divide by zero

        for (int i = 0; i < this.forces.size(); i++) {
            Vec3 localTorque = this.forcePositions.get(i).cross(this.forces.get(i));
            torque = torque.add(localTorque.toVector3f());
        }

        // This doesn't quite seem accurate, but it looks good enough
        // Rotation/angular velocity code based on https://gafferongames.com/post/physics_in_3d/
        this.angularMomentum = this.angularMomentum.add(torque);

        // JOML modifies vectors - we need to do a new vector3f here because transform modifies the vector you pass in
        this.angularVelocity = this.inverseInertia.transform(new Vector3f(this.angularMomentum));

        Quaternionf angularVelocityQuaternion = new Quaternionf(this.angularVelocity.x, this.angularVelocity.y, this.angularVelocity.z, 0);
        Quaternionf spin = angularVelocityQuaternion.mul(0.5f).mul(this.rotation);

        this.rotation = new Quaternionf(this.rotation).add(spin);
        this.rotation.normalize();
    }

    public void update(float dt) {
        this.calculateWaterHeight();
        calculateBuoyancy();
        calculateDrag();
        calculateEngineForces();

        resolveAcceleration(dt);
        resolveAngularMomentum();

        boat.moveTo(this.position);
        boat.setRotation(this.rotation);

        this.forces.clear();
        this.forcePositions.clear();
    }

    public void readData(CompoundTag nbt) {
        float velocityX = nbt.getFloat("velocityX");
        float velocityY = nbt.getFloat("velocityY");
        float velocityZ = nbt.getFloat("velocityZ");
        this.oldPosition = this.oldPosition.subtract(velocityX, velocityY, velocityZ);

        float angularMomentumX = nbt.getFloat("angularMomentumX");
        float angularMomentumY = nbt.getFloat("angularMomentumY");
        float angularMomentumZ = nbt.getFloat("angularMomentumZ");
        this.angularMomentum = new Vector3f(angularMomentumX, angularMomentumY, angularMomentumZ);
    }

    public void writeData(CompoundTag nbt) {
        Vector3f velocity = this.getVelocity().toVector3f();
        nbt.putFloat("velocityX", velocity.x);
        nbt.putFloat("velocityY", velocity.y);
        nbt.putFloat("velocityZ", velocity.z);

        nbt.putFloat("angularMomentumX", angularMomentum.x);
        nbt.putFloat("angularMomentumY", angularMomentum.y);
        nbt.putFloat("angularMomentumZ", angularMomentum.z);
    }
}
