package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class BlockFace {
    private final BlockPos blockPos;
    private final RotatingComponent component;
    private final Vec3 position;
    private final Vec3 normal;
    private final float airDragFactor;
    private final float waterDragFactor;

    public BlockFace(BlockPos blockPos, RotatingComponent component, Vec3 position, Vec3 normal, float airDragFactor, float waterDragFactor) {
        this.blockPos = blockPos;
        this.component = component;
        this.position = position;
        this.normal = normal;
        this.airDragFactor = airDragFactor;
        this.waterDragFactor = waterDragFactor;
    }

    public BlockFace(BlockPos blockPos, Vec3 position, Vec3 normal, float airDragFactor, float waterDragFactor) {
        this.blockPos = blockPos;
        this.component = null;
        this.position = position;
        this.normal = normal;
        this.airDragFactor = airDragFactor;
        this.waterDragFactor = waterDragFactor;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Vec3 getDrag(Vec3 velocity, Vec3 angularVelocity, boolean isSubmerged) {
        // the linear motion caused by the angular velocity is perpendicular to both the position vector and the
        // angular velocity vector, so it can be found by taking their cross product
        // TODO: reposition according to rotating component
        Vec3 velocityFromAngular = new Vec3(angularVelocity.toVector3f()).cross(this.position);
        Vec3 totalVelocity = velocityFromAngular.add(velocity);
        Vec3 velocityDirection = totalVelocity.normalize();

        Vec3 normal = new Vec3(this.normal.toVector3f());
        if (this.component != null) {
            Vector4f transformed = new Vector4f((float) normal.x, (float) normal.y, (float) normal.z, 1f);
            Matrix4f matrix = this.component.getRotationMatrix();
            transformed = matrix.transform(transformed);

            normal = new Vec3(transformed.x, transformed.y, transformed.z);
        }

        float dragFactor = isSubmerged ? this.waterDragFactor : this.airDragFactor;
        double dragAngle = Math.max(velocityDirection.dot(normal), 0);
        double dragMagnitude = -1 * dragAngle * dragFactor * totalVelocity.length();

        if (this.component != null && Math.abs(this.normal.x) > 0.02) {
            System.out.println("");
            System.out.println(this.normal);
            System.out.println(normal);
        }

        return normal.scale(dragMagnitude);
    }
}
