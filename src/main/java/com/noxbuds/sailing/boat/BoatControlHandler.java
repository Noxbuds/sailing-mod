package com.noxbuds.sailing.boat;

import com.noxbuds.sailing.block.*;
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
    private ArrayList<RotatingComponent> rudderComponents;

    private double rudderRotation;
    private Vec2 rudderLimits;

    private double engineThrottle;
    private Vec2 engineThrottleLimits;

    public BoatControlHandler(EntityBoat boat) {
        this.boat = boat;

        this.controllers = new ArrayList<>();
        this.controllerPositions = new ArrayList<>();
        this.findRudderComponents();

        this.rudderRotation = 0f;
        this.rudderLimits = new Vec2(-0.3f, 0.3f).scale((float) Math.PI);

        this.engineThrottle = 0f;
        this.engineThrottleLimits = new Vec2(-1, 1);
    }

    private void findRudderComponents() {
        this.rudderComponents = new ArrayList<>();
        HashMap<BlockPos, BoatBlockContainer> blocks = this.boat.getBlocks();

        for (BlockPos blockPos : blocks.keySet()) {
            BoatBlockContainer container = blocks.get(blockPos);
            Block block = container.blockState().getBlock();

            if (!(block instanceof RudderBlock)) {
                continue;
            }

            RotatingBlock rotatingBlock = (RotatingBlock) block;
            BlockPos firstConnectedBlock = rotatingBlock.getFirstBlock(blockPos, container.blockState());
            RotatingComponent component = this.boat.getRotatingComponent(firstConnectedBlock);

            if (container.blockState().getBlock() instanceof RudderBlock && component != null) {
                this.rudderComponents.add(component);
            }
        }
    }

    public void update(float dt) {
        for (int i = 0; i < this.controllers.size(); i++) {
            Player player = this.controllers.get(i);
            Vec3 input = new Vec3(player.xxa, 0, player.zza);

            BlockPos blockPos = this.controllerPositions.get(i);
            Block block = this.boat.getBlocks().get(blockPos).blockState().getBlock();

            for (RotatingComponent component : this.rudderComponents) {
                component.setRotation((float) this.rudderRotation);
            }

            if (block instanceof HelmBlock) {
                this.rotateRudder(input.x * dt);
                this.addEngineThrottle(input.z * dt);
                continue;
            }

            RiggingLine line = this.boat.getRiggingLine(blockPos);
            if (block instanceof WinchBlock && line != null) {
                double targetLength = line.getTargetLength();
                targetLength += input.z * dt;
                line.setTargetLength(targetLength);
            }
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
