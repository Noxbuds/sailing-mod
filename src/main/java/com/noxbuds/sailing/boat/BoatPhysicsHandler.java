package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;

public class BoatPhysicsHandler {
    private final Vec3 gravity = new Vec3(0, -9.8, 0);
    private final EntityBoat boat;
    private float waterHeight;

    private Vec3 position;
    private Vec3 oldPosition;

    private Quaternionf rotation;
    private Quaternionf oldRotation;
    private Vector3f angularMomentum;
    private Vector3f angularVelocity;

    private final Matrix3f inverseInertia;

    public BoatPhysicsHandler(EntityBoat boat) {
        this.boat = boat;
        this.position = new Vec3(boat.position().toVector3f());
        this.oldPosition = new Vec3(this.position.toVector3f());

        this.rotation = new Quaternionf(boat.getRotation());
        this.oldRotation= new Quaternionf(boat.getRotation());

        this.angularMomentum = Vec3.ZERO.toVector3f();
        this.angularVelocity = Vec3.ZERO.toVector3f();

        this.inverseInertia = getInertia().invert();
    }

    // Treating the boat as a set of particles where each particle is the centre of the block
    // See http://www.cs.cmu.edu/~baraff/sigcourse/notesd1.pdf
    private Matrix3f getInertia() {
        Matrix3f tensor = new Matrix3f();

        HashMap<BlockPos, BlockState> blocks = boat.getBlocks();
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

            // TODO: get actual block mass
            float mass = 1f;
            Matrix3f sum = leftSide.add(rightSide).scale(mass);
            tensor.add(sum);
        }

        return tensor;
    }

    private void calculateWaterHeight() {
        Vec3 minPos = new Vec3(boat.getMinPosition()).add(boat.position());
        Vec3 maxPos = new Vec3(boat.getMaxPosition()).add(boat.position());

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

    public Vec3 getAngularVelocity() {
        return new Vec3(this.angularVelocity);
    }

    public void update(float dt) {
        this.calculateWaterHeight();

        // TODO: figure out realistic values for this per block
        float waterWeight = 1f;

        ArrayList<Vec3> forces = new ArrayList<>();
        ArrayList<Vec3> forcePositions = new ArrayList<>();

        // Calculate bouyancy
        HashMap<BlockPos, BlockState> blocks = this.boat.getBlocks();
        for (BlockPos blockPos : blocks.keySet()) {
            Vec3 position = this.boat.boatToWorld(blockPos);

            // volume will be 1m x 1m x (y - waterHeight), so we can simplify
            // depth does not affect buoyancy, so we cap it to [0, 1] x waterWeight
            double depth = -Math.max(Math.min(position.y - this.waterHeight, 0), -1f);
            double mag = depth * waterWeight;
            Vec3 force = new Vec3(0, 1, 0).scale(mag);

            forces.add(force);
            forcePositions.add(position.subtract(boat.position()));
        }

        // Calculate drag
        Vector3f velocity = this.position.toVector3f().sub(this.oldPosition.toVector3f());
        for (BlockFace face : this.boat.getBlockFaces()) {
            Vec3 facePosition = face.getPosition();
            Vec3 facePositionWorld = this.boat.boatToWorld(facePosition);
            boolean isSubmerged = facePositionWorld.y < this.waterHeight;

            Vec3 drag = face.getDrag(new Vec3(velocity), new Vec3(this.angularVelocity), isSubmerged);
            forces.add(drag);
            forcePositions.add(face.getPosition());
        }

        Vec3 acceleration = gravity;
        Vec3 rotationAcceleration = new Vec3(0f, 0f, 0f); // add small value to prevent divide by zero

        for (int i = 0; i < forces.size(); i++) {
            // TODO: handle block weight (assuming weight of 1kg each)
            acceleration = acceleration.add(forces.get(i));

            Vec3 torque = forcePositions.get(i).cross(forces.get(i));
            rotationAcceleration = rotationAcceleration.add(torque);
        }

        Vec3 newPosition = this.position.scale(2f)
                .subtract(this.oldPosition)
                .add(acceleration.scale(dt * dt));

        this.oldPosition = new Vec3(this.position.toVector3f());
        this.position = newPosition;
        boat.moveTo(this.position);

        Vector3f torque = rotationAcceleration.toVector3f();

        // This doesn't quite seem accurate, but it looks good enough
        // Rotation/angular velocity code based on https://gafferongames.com/post/physics_in_3d/
        this.angularMomentum = this.angularMomentum.add(torque);

        // JOML modifies vectors - we need to do a new vector3f here because transform modifies the vector you pass in
        this.angularVelocity = this.inverseInertia.transform(new Vector3f(this.angularMomentum));

        Quaternionf deltaRotation = new Quaternionf(this.angularVelocity.x, this.angularVelocity.y, this.angularVelocity.z, 0);
        Quaternionf spin = deltaRotation.mul(0.5f).mul(this.rotation);

        Quaternionf newRotation = new Quaternionf();
        newRotation.x = this.rotation.x + spin.x;
        newRotation.y = this.rotation.y + spin.y;
        newRotation.z = this.rotation.z + spin.z;
        newRotation.w = this.rotation.w + spin.w;
        newRotation.normalize();

        this.oldRotation = new Quaternionf(this.rotation);
        this.rotation = newRotation;

        boat.setRotation(this.rotation);
    }
}
