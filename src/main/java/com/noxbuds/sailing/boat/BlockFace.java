package com.noxbuds.sailing.boat;

import net.minecraft.world.phys.Vec3;

public class BlockFace {
    private final Vec3 position;
    private final Vec3 normal;
    private final float airDragFactor;
    private final float waterDragFactor;

    public BlockFace(Vec3 position, Vec3 normal, float airDragFactor, float waterDragFactor) {
        this.position = position;
        this.normal = normal;
        this.airDragFactor = airDragFactor;
        this.waterDragFactor = waterDragFactor;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public Vec3 getDrag(Vec3 velocity, Vec3 angularVelocity, boolean isSubmerged) {
        // the linear motion caused by the angular velocity is perpendicular to both the position vector and the
        // angular velocity vector, so it can be found by taking their cross product
        Vec3 velocityFromAngular = new Vec3(angularVelocity.toVector3f()).cross(this.position);
        Vec3 totalVelocity = velocityFromAngular.add(velocity);
        Vec3 velocityDirection = totalVelocity.normalize();

        float dragFactor = isSubmerged ? this.waterDragFactor : this.airDragFactor;
        double dragAngle = Math.max(velocityDirection.dot(this.normal), 0);
        double dragMagnitude = -1 * dragAngle * dragFactor * totalVelocity.length();

        return this.normal.scale(dragMagnitude);
    }
}
