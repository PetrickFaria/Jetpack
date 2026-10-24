package br.com.petrickfaria.jetpack.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Volumetric pack frame, tanks and twin thrusters, attached to the player's torso. */
public final class JetpackBackModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("jetpack", "jetpack_back"), "main");
    private final ModelPart root;

    public JetpackBackModel(ModelPart bakedRoot) { this.root = bakedRoot; }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The torso model's positive Y points down. Move the pack into the middle of the back,
        // keeping its upper frame below the shoulder line and its nozzles near the waist.
        PartDefinition pack = root.addOrReplaceChild("pack", CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
        // UVs sample the existing tier illustration; the geometry supplies real depth while retaining its palette.
        pack.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(64, 76)
                .addBox(-3.4F, -9.8F, 1.8F, 6.8F, 8.5F, 2.2F), PartPose.ZERO);
        pack.addOrReplaceChild("left_tank", CubeListBuilder.create().texOffs(37, 52)
                .addBox(-5.0F, -10.4F, 1.4F, 2.7F, 8.4F, 3.4F), PartPose.ZERO);
        pack.addOrReplaceChild("right_tank", CubeListBuilder.create().texOffs(92, 52)
                .addBox(2.3F, -10.4F, 1.4F, 2.7F, 8.4F, 3.4F), PartPose.ZERO);
        pack.addOrReplaceChild("top_bridge", CubeListBuilder.create().texOffs(63, 33)
                .addBox(-2.4F, -11.6F, 2.0F, 4.8F, 2.0F, 3.0F), PartPose.ZERO);
        // Raised crown and side rails give the frame a layered, machined silhouette
        // while staying inside the torso outline and using the existing tier texture.
        pack.addOrReplaceChild("crown_cap", CubeListBuilder.create().texOffs(63, 33)
                .addBox(-2.8F, -12.0F, 2.2F, 5.6F, 0.8F, 3.2F), PartPose.ZERO);
        pack.addOrReplaceChild("left_frame_rail", CubeListBuilder.create().texOffs(23, 61)
                .addBox(-5.5F, -10.6F, 1.8F, 0.7F, 8.2F, 2.8F), PartPose.ZERO);
        pack.addOrReplaceChild("right_frame_rail", CubeListBuilder.create().texOffs(124, 61)
                .addBox(4.8F, -10.6F, 1.8F, 0.7F, 8.2F, 2.8F), PartPose.ZERO);
        // A raised service panel and inset spine make the central assembly read as
        // a compact power unit instead of one broad flat plate.
        pack.addOrReplaceChild("power_housing", CubeListBuilder.create().texOffs(64, 76)
                .addBox(-2.0F, -8.9F, 4.0F, 4.0F, 5.7F, 0.8F), PartPose.ZERO);
        pack.addOrReplaceChild("power_spine", CubeListBuilder.create().texOffs(64, 76)
                .addBox(-0.55F, -8.4F, 4.7F, 1.1F, 4.7F, 0.35F), PartPose.ZERO);
        pack.addOrReplaceChild("left_bracket", CubeListBuilder.create().texOffs(23, 61)
                .addBox(-5.4F, -2.6F, 1.8F, 1.1F, 3.2F, 2.8F), PartPose.ZERO);
        pack.addOrReplaceChild("right_bracket", CubeListBuilder.create().texOffs(124, 61)
                .addBox(4.3F, -2.6F, 1.8F, 1.1F, 3.2F, 2.8F), PartPose.ZERO);
        pack.addOrReplaceChild("left_thruster", CubeListBuilder.create().texOffs(35, 132)
                .addBox(-4.8F, -0.3F, 1.2F, 2.5F, 3.0F, 3.2F), PartPose.ZERO);
        pack.addOrReplaceChild("right_thruster", CubeListBuilder.create().texOffs(97, 132)
                .addBox(2.3F, -0.3F, 1.2F, 2.5F, 3.0F, 3.2F), PartPose.ZERO);
        // Layered nozzle collars and tapered-looking outlet lips add depth to the
        // twin propulsors without changing their established location on the back.
        pack.addOrReplaceChild("left_nozzle_collar", CubeListBuilder.create().texOffs(35, 132)
                .addBox(-5.0F, 2.1F, 1.0F, 2.9F, 0.8F, 3.7F), PartPose.ZERO);
        pack.addOrReplaceChild("right_nozzle_collar", CubeListBuilder.create().texOffs(97, 132)
                .addBox(2.1F, 2.1F, 1.0F, 2.9F, 0.8F, 3.7F), PartPose.ZERO);
        pack.addOrReplaceChild("left_exhaust", CubeListBuilder.create().texOffs(35, 132)
                .addBox(-4.6F, 2.8F, 1.55F, 2.1F, 1.2F, 2.6F), PartPose.ZERO);
        pack.addOrReplaceChild("right_exhaust", CubeListBuilder.create().texOffs(97, 132)
                .addBox(2.5F, 2.8F, 1.55F, 2.1F, 1.2F, 2.6F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 150, 170);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
