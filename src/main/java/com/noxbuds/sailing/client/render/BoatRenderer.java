package com.noxbuds.sailing.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.noxbuds.sailing.boat.BoatBlockContainer;
import com.noxbuds.sailing.boat.EntityBoat;
import com.noxbuds.sailing.boat.RotatingComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.joml.*;
import org.joml.Math;

import java.util.HashMap;

public class BoatRenderer extends EntityRenderer {
    protected BoatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return new ResourceLocation("minecraft", "textures/block/stone.png");
    }

    @Override
    public void render(Entity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        EntityBoat boat = (EntityBoat)entity;
        HashMap<BlockPos, BoatBlockContainer> blocks = boat.getBlocks();
        Vector3f minPos = boat.getMinPosition();

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        for (BlockPos blockPos : blocks.keySet()) {
            BoatBlockContainer container = blocks.get(blockPos);

            poseStack.pushPose();
            Vector3f position = blockPos.getCenter().toVector3f();

            Quaternionf rotation = new Quaternionf(boat.getRotation());
            poseStack.mulPose(rotation);

            if (container.componentId().isPresent()) {
                int componentId = container.componentId().get();
                RotatingComponent component = boat.getRotatingComponent(componentId);
                if (component != null) {
                    Matrix4f componentMatrix = component.getTransformationMatrix();
                    poseStack.mulPoseMatrix(componentMatrix);
                }
            }

            poseStack.translate(minPos.x, minPos.y, minPos.z);
            poseStack.translate(position.x, position.y, position.z);


            blockRenderer.renderSingleBlock(container.blockState(), poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private Vector3f[] getQuad(Vector3f normal) {
        float rotX = Math.atan2(normal.y, normal.z);
        float rotY = Math.atan2(normal.x, normal.z);
        float rotZ = Math.atan2(normal.y, normal.x);

        Matrix4f rotation = new Matrix4f().rotationXYZ(-rotZ, -rotY, -rotX);

        Vector3f[] vertices = {
                new Vector3f(-0.5f, -0.5f, 0.0f),
                new Vector3f(-0.5f,  0.5f, 0.0f),
                new Vector3f( 0.5f,  0.5f, 0.0f),
                new Vector3f( 0.5f, -0.5f, 0.0f),
        };

        for (int i = 0; i < vertices.length; i++) {
            Vector4f newPos = rotation.transform(new Vector4f(vertices[i].x, vertices[i].y, vertices[i].z, 1f));
            vertices[i] = new Vector3f(newPos.x, newPos.y, newPos.z);
            vertices[i].add(new Vector3f(normal).mul(0.5f));
        }

        return vertices;
    }

    private void renderCube(Vector3f pos, VertexConsumer consumer, PoseStack poseStack, int light) {
        Vector3f[] normals = {
                new Vector3f(1f, 0f, 0f),
                new Vector3f(-1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                new Vector3f(0f, -1f, 0f),
                new Vector3f(0f, 0f, 1f),
                new Vector3f(0f, 0f, -1f)
        };

        Vec2[] uvs = {
                new Vec2(0f, 0f),
                new Vec2(0f, 1f),
                new Vec2(1f, 1f),
                new Vec2(1f, 0f)
        };

        Matrix3f normalMatrix = poseStack.last().normal();
        Matrix4f positionMatrix = poseStack.last().pose();

        for (Vector3f normal : normals) {
            Vector3f[] vertices = getQuad(normal);

            float r = 1, g = 1, b = 1, a = 1;

            for (int i = 0; i < vertices.length; i++) {
                Vector3f position = vertices[i].add(pos);
                Vector4f newPos = positionMatrix.transform(new Vector4f(position.x, position.y, position.z, 1f));

                Vector3f newNormal = normalMatrix.transform(new Vector3f(normal));

                consumer.vertex(newPos.x, newPos.y, newPos.z, r, g, b, a, uvs[i].x, uvs[i].y, OverlayTexture.NO_OVERLAY, light, newNormal.x, newNormal.y, newNormal.z);
            }
        }
    }
}
