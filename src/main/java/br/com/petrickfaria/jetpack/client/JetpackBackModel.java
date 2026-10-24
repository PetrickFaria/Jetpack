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
        // Compact twin tanks, layered core and stepped nozzle assemblies.
        pack.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(2, 2)
                .addBox(-3.00F, -7.00F, 0.10F, 6.00F, 7.00F, 1.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_tank", CubeListBuilder.create().texOffs(23, 2)
                .addBox(-5.00F, -8.00F, 0.70F, 3.00F, 8.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_tank", CubeListBuilder.create().texOffs(39, 2)
                .addBox(2.00F, -8.00F, 0.70F, 3.00F, 8.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("top_bridge", CubeListBuilder.create().texOffs(55, 2)
                .addBox(-2.00F, -8.00F, 0.40F, 4.00F, 2.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("crown_cap", CubeListBuilder.create().texOffs(74, 2)
                .addBox(-3.00F, -9.00F, 0.70F, 6.00F, 1.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_frame_rail", CubeListBuilder.create().texOffs(95, 2)
                .addBox(-5.00F, -8.00F, 0.20F, 1.00F, 8.00F, 2.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_frame_rail", CubeListBuilder.create().texOffs(106, 2)
                .addBox(4.00F, -8.00F, 0.20F, 1.00F, 8.00F, 2.00F), PartPose.ZERO);
        pack.addOrReplaceChild("power_housing", CubeListBuilder.create().texOffs(117, 2)
                .addBox(-2.00F, -6.00F, 1.20F, 4.00F, 5.00F, 1.00F), PartPose.ZERO);
        pack.addOrReplaceChild("power_spine", CubeListBuilder.create().texOffs(130, 2)
                .addBox(-0.50F, -5.00F, 2.20F, 1.00F, 4.00F, 1.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_bracket", CubeListBuilder.create().texOffs(135, 2)
                .addBox(-5.00F, -1.00F, 0.70F, 1.00F, 3.00F, 2.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_bracket", CubeListBuilder.create().texOffs(2, 21)
                .addBox(4.00F, -1.00F, 0.70F, 1.00F, 3.00F, 2.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_thruster", CubeListBuilder.create().texOffs(12, 21)
                .addBox(-5.00F, 0.00F, 0.70F, 3.00F, 3.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_thruster", CubeListBuilder.create().texOffs(26, 21)
                .addBox(2.00F, 0.00F, 0.70F, 3.00F, 3.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_nozzle_collar", CubeListBuilder.create().texOffs(40, 21)
                .addBox(-5.00F, 2.00F, 0.70F, 3.00F, 1.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_nozzle_collar", CubeListBuilder.create().texOffs(56, 21)
                .addBox(2.00F, 2.00F, 0.70F, 3.00F, 1.00F, 3.00F), PartPose.ZERO);
        pack.addOrReplaceChild("left_exhaust", CubeListBuilder.create().texOffs(72, 21)
                .addBox(-4.50F, 3.00F, 1.20F, 2.00F, 1.00F, 2.00F), PartPose.ZERO);
        pack.addOrReplaceChild("right_exhaust", CubeListBuilder.create().texOffs(84, 21)
                .addBox(2.50F, 3.00F, 1.20F, 2.00F, 1.00F, 2.00F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 150, 170);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
