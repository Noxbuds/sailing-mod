package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;

public class RiggingLine {
    private BlockPos posA;
    private BlockPos posB;

    private double targetLength;

    public RiggingLine(BlockPos posA, BlockPos posB) {
        this.posA = posA;
        this.posB = posB;

        this.targetLength = calculateLength();
    }

    public RiggingLine(CompoundTag nbt) {
        int ax = nbt.getInt("ax");
        int ay = nbt.getInt("ay");
        int az = nbt.getInt("az");
        this.posA = new BlockPos(ax, ay, az);

        int bx = nbt.getInt("bx");
        int by = nbt.getInt("by");
        int bz = nbt.getInt("bz");
        this.posB = new BlockPos(bx, by, bz);
    }

    public CompoundTag getNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("ax", this.posA.getX());
        nbt.putInt("ay", this.posA.getY());
        nbt.putInt("az", this.posA.getZ());

        nbt.putInt("bx", this.posB.getX());
        nbt.putInt("by", this.posB.getY());
        nbt.putInt("bz", this.posB.getZ());

        return nbt;
    }

    public void offsetPositions(Vec3i offset) {
        this.posA = this.posA.offset(offset);
        this.posB = this.posB.offset(offset);
    }

    private double calculateLength() {
        Vec3 posA = this.posA.getCenter();
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
        RotatingComponent componentA = boat.getRotatingComponent(this.posA);
        RotatingComponent componentB = boat.getRotatingComponent(this.posB);

        if (componentA == null && componentB == null) {
            return;
        }

        // modelling almost as a spring, but only applying force on extension rather than contraction
        Vec3 posA = this.transformPosition(this.posA, boat);
        Vec3 posB = this.transformPosition(this.posB, boat);

        Vec3 direction = posB.subtract(posA);
        double length = direction.length();
        double extension = Math.max(length - targetLength, 0);
        double force = extension * 1e2; // TODO: figure out force constant
        Vec3 forceVectorA = direction.scale(force / length);

        if (componentA != null) {
            componentA.addForce(forceVectorA, posA);
        }

        if (componentB != null) {
            componentB.addForce(forceVectorA.scale(-1), posB);
        }
    }
}
