package com.noxbuds.sailing.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;

public class RotatingComponent {
    private final Vec3 base;
    private final Vec3 axis;
    private final HashMap<BlockPos, BoatBlockContainer> blocks;

    private float rotation;
    private float angularMomentum;

    private final ArrayList<Vec3> forces;
    private final ArrayList<Vec3> forcePositions;

    public RotatingComponent(Vec3 base, Vec3 axis) {
        this.base = base;
        this.axis = axis;
        this.blocks = new HashMap<>();

        this.rotation = 0f;
        this.angularMomentum = 0f;

        this.forces = new ArrayList<>();
        this.forcePositions = new ArrayList<>();
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        this.angularMomentum = 0;
    }

    public float getRotation() {
        return this.rotation;
    }

    public Matrix4f getTransformationMatrix() {
        Matrix4f matrix = new Matrix4f();

        matrix = matrix.translate(this.base.scale(1).toVector3f());
        matrix = matrix.rotate(this.rotation, this.axis.toVector3f());
        matrix = matrix.translate(this.base.scale(-1).toVector3f());

        return matrix;
    }

    public Matrix4f getRotationMatrix() {
        Matrix4f matrix = new Matrix4f();
        matrix = matrix.rotate(this.rotation, this.axis.toVector3f());
        return matrix;
    }

    public void addForce(Vec3 force, Vec3 position) {
        this.forces.add(force);
        this.forcePositions.add(position.subtract(this.base));
    }

    public void update(float dt) {
        Vec3 torque = Vec3.ZERO;

        for (int i = 0; i < this.forces.size(); i++) {
            Vec3 localTorque = this.forcePositions.get(i).cross(this.forces.get(i));
            torque = torque.add(localTorque);
        }

        // TODO: calculate inverse inertia - is it really necessary?
        this.angularMomentum += torque.dot(this.axis);
        float angularVelocity = this.angularMomentum / 100f;

        this.rotation += angularVelocity * dt;

        this.forces.clear();
        this.forcePositions.clear();
    }

    // TODO: load/save state
}
