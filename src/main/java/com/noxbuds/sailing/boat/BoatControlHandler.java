package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.block.BoatControlBlock;
import com.noxbuds.sailing.block.HelmBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;

public class BoatControlHandler {
    private final EntityBoat boat;

    private ArrayList<Player> controllers;
    private ArrayList<BlockPos> controllerPositions;

    private double rudderRotation;
    private Vec2 rudderLimits;

    private double engineThrottle;
    private Vec2 engineThrottleLimits;

    public BoatControlHandler(EntityBoat boat) {
        this.boat = boat;

        this.controllers = new ArrayList<>();
        this.controllerPositions = new ArrayList<>();

        this.rudderRotation = 0f;
        this.rudderLimits = new Vec2(-0.5f, 0.5f).scale((float) Math.PI);

        this.engineThrottle = 0f;
        this.engineThrottleLimits = new Vec2(-1, 1);
    }

    public void update(float dt) {
        for (int i = 0; i < this.controllers.size(); i++) {
            Player player = this.controllers.get(i);
            Vec3 input = new Vec3(player.xxa, 0, player.zza);

            BlockPos blockPos = this.controllerPositions.get(i);
            Block block = this.boat.getBlocks().get(blockPos).blockState().getBlock();

            if (block instanceof HelmBlock) {
                this.rotateRudder(input.x * dt);
                this.addEngineThrottle(input.z * dt);
            }

            // TODO: rigging/line control
        }
    }

    public void addController(Player player, BlockPos blockPos) {
        this.controllers.add(player);
        this.controllerPositions.add(blockPos);
    }

    public void removeController(Player player) {
        int index = this.controllers.indexOf(player);

        if (index >= 0) {
            this.controllers.remove(index);
            this.controllerPositions.remove(index);
        }
    }

    public void rotateRudder(double delta) {
        this.rudderRotation += delta;
        this.rudderRotation = Math.min(Math.max(this.rudderRotation, this.rudderLimits.x), this.rudderLimits.y);
    }

    public double getRudderRotation() {
        return this.rudderRotation;
    }

    public void addEngineThrottle(double delta) {
        this.engineThrottle += delta;
        this.engineThrottle = Math.min(Math.max(this.engineThrottle, this.engineThrottleLimits.x), this.engineThrottleLimits.y);
    }

    public double getEngineThrottle() {
        return this.engineThrottle;
    }
}
