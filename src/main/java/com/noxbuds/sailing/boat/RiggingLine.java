package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RiggingLine {
    private BlockPos winchPos;
    private BlockPos posB;

    private double targetLength;

    public RiggingLine(BlockPos winchPos, BlockPos posB) {
        this.winchPos = winchPos;
        this.posB = posB;

        this.targetLength = calculateLength();
    }

    public RiggingLine(CompoundTag nbt) {
        int ax = nbt.getInt("ax");
        int ay = nbt.getInt("ay");
        int az = nbt.getInt("az");
        this.winchPos = new BlockPos(ax, ay, az);

        int bx = nbt.getInt("bx");
        int by = nbt.getInt("by");
        int bz = nbt.getInt("bz");
        this.posB = new BlockPos(bx, by, bz);
    }

    public CompoundTag getNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("ax", this.winchPos.getX());
        nbt.putInt("ay", this.winchPos.getY());
        nbt.putInt("az", this.winchPos.getZ());

        nbt.putInt("bx", this.posB.getX());
        nbt.putInt("by", this.posB.getY());
        nbt.putInt("bz", this.posB.getZ());

        return nbt;
    }

    public BlockPos getWinchPos() {
        return this.winchPos;
    }

    public double getTargetLength() {
        return this.targetLength;
    }

    public void setTargetLength(double targetLength) {
        this.targetLength = targetLength;
    }

    public void offsetPositions(Vec3i offset) {
        this.winchPos = this.winchPos.offset(offset);
        this.posB = this.posB.offset(offset);
    }

    private double calculateLength() {
        Vec3 posA = this.winchPos.getCenter();
        Vec3 posB = this.posB.getCenter();

        return posA.subtract(posB).length();
    }

    private Vec3 transformPosition(BlockPos blockPos, EntityBoat boat) {
        Vector3f position = blockPos.getCenter()
            .toVector3f()
            .add(boat.getMinPosition());
        Vector4f transformed = new Vector4f(position, 1);

        RotatingComponent component = boat.getRotatingComponent(blockPos);
        if (component != null) {
            Matrix4f matrix = component.getTransformationMatrix();
            transformed = transformed.mul(matrix);
        }

        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    public void applyForces(EntityBoat boat) {
        RotatingComponent componentB = boat.getRotatingComponent(this.posB);

        if (componentB == null) {
            return;
        }

        // modelling almost as a spring, but only applying force on extension rather than contraction
        Vec3 posA = this.transformPosition(this.winchPos, boat);
        Vec3 posB = this.transformPosition(this.posB, boat);

        Vec3 direction = posB.subtract(posA);
        double length = direction.length();
        double extension = Math.max(length - targetLength, 0);
        double force = extension * 1e2; // TODO: figure out force constant
        Vec3 forceVectorA = direction.scale(force / length);

        componentB.addForce(forceVectorA.scale(-1), posB);
    }
}
