package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public final class JetpackBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final JetpackBackModel model;

    public JetpackBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.model = new JetpackBackModel(models.bakeLayer(JetpackBackModel.LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = JetpackClientState.getEquipment(player.getId());
        if (!(stack.getItem() instanceof JetpackItem)) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return;
        ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/item/" + id.getPath() + ".png");

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        // The back face is +Z in the player model's local torso coordinates.
        poseStack.translate(0.0D, 0.0D, 0.12D);
        model.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
