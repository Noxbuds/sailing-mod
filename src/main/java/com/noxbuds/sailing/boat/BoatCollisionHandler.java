package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.renderable.IRenderable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;

public class BoatCollisionHandler {
    private final EntityBoat boat;

    public BoatCollisionHandler(EntityBoat boat) {
        this.boat = boat;
    }

    public void handleCollision(Entity entity) {
        AABB originalBB = entity.getBoundingBox();
        Vec3 minPos = new Vec3(originalBB.minX, originalBB.minY, originalBB.minZ);
        Vec3 maxPos = new Vec3(originalBB.maxX, originalBB.maxY, originalBB.maxZ);

        minPos = this.boat.worldToBoat(minPos);
        maxPos = this.boat.worldToBoat(maxPos);

        HashMap<BlockPos, BoatBlockContainer> blocks = this.boat.getBlocks();

        BoatPhysicsHandler physicsHandler = this.boat.getPhysicsHandler();
        Vec3 velocity = physicsHandler.getVelocity();
        Vec3 angularVelocity = physicsHandler.getAngularVelocity();
        Quaternionf rotation = this.boat.getRotation();

        for (BlockPos blockPos : blocks.keySet()) {
            Vec3 blockMin = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            blockMin = blockMin.add(new Vec3(this.boat.getMinPosition()));

            AABB relativeBB = new AABB(minPos.x, minPos.y, minPos.z, maxPos.x, maxPos.y, maxPos.z);
            BoatBlockContainer container = blocks.get(blockPos);
            if (container.componentId().isPresent()) {
                RotatingComponent component = boat.getRotatingComponent(container.componentId().get());
                if (component != null) {
                    Matrix4f transformation = boat.getRotatingComponent(container.componentId().get()).getTransformationMatrix();
                    transformation.invert();

                    Vector4f localMin = new Vector4f((float) minPos.x, (float) minPos.y, (float) minPos.z, 1f);
                    Vector4f localMax = new Vector4f((float) maxPos.x, (float) maxPos.y, (float) maxPos.z, 1f);

                    transformation.transform(localMin);
                    transformation.transform(localMax);

                    relativeBB = new AABB(localMin.x, localMin.y, localMin.z, localMax.x, localMax.y, localMax.z);
                }
            }

            AABB blockBB = new AABB(blockMin.x, blockMin.y, blockMin.z, blockMin.x + 1, blockMin.y + 1.5f, blockMin.z + 1);

            if (relativeBB.intersects(blockBB)) {
                double deltaY = blockBB.maxY - relativeBB.minY;
                Vec3 newPosition = entity.position();
                Vec3 movement = entity.getDeltaMovement();
                if (movement.y < 0) {
                    newPosition = newPosition.add(0, deltaY * 0.99f, 0);
                }
                movement = new Vec3(movement.x, 0, movement.z);

                Vector3f blockVelocity = angularVelocity.cross(blockBB.getCenter()).toVector3f();
                rotation.transform(blockVelocity);
                blockVelocity = blockVelocity.add(velocity.toVector3f());

                newPosition = newPosition.add(new Vec3(blockVelocity));

                entity.setPos(newPosition);
                entity.setDeltaMovement(movement.x, movement.y, movement.z);

                entity.setOnGround(true);
            }
        }
    }
}
