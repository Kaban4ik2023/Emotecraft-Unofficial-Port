package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;

public final class LegacyPlayerRenderer extends RenderPlayer {
    public LegacyPlayerRenderer(RenderManager manager, boolean slim) {
        super(manager, slim);
        mainModel = new LegacyPlayerModel(slim);
        layerRenderers.removeIf(layer -> ((Object) layer) instanceof LayerBipedArmor || ((Object) layer) instanceof LayerHeldItem || ((Object) layer) instanceof LayerCustomHead);
        addLayer(new LegacyHeldItemLayer(this));
        addLayer(new LayerCustomHead(getMainModel().bipedHead) {
            @Override public void doRenderLayer(EntityLivingBase entity,float limb,float amount,float partial,float age,float yaw,float pitch,float scale) {
                GlStateManager.pushMatrix();
                try { LegacyPlayerModel.bodyBend(entity);super.doRenderLayer(entity,limb,amount,partial,age,yaw,pitch,scale); }
                finally { GlStateManager.popMatrix(); }
            }
        });
        addLayer(new LayerBipedArmor(this) {
            @Override protected void initArmor() {
                modelLeggings = armor(0.5f); modelArmor = armor(1f);
            }
        });
    }
    private static ModelBiped armor(float size) {
        return new ModelBiped(size) {
            { LegacyPlayerModel.wrap(this); }
            @Override public void render(Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale) {
                setRotationAngles(limb,amount,age,yaw,pitch,scale,entity);
                LegacyPlayerModel.renderParts(this,entity,scale);
            }
            @Override public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
                LegacyPlayerModel.reset(this);
                super.setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
                LegacyPlayerModel.pose(this, entity, age - entity.ticksExisted);
            }
        };
    }
    @Override protected void applyRotations(AbstractClientPlayer player, float age, float yaw, float partial) {
        super.applyRotations(player, age, yaw, partial);
        ClientProxy.Playback state = ClientProxy.INSTANCE.playing.get(player.getUniqueID());
        if (state == null) return;
        KeyframeAnimationPlayer animation = state.animation; animation.setupAnim(partial);
        Vec3f position = animation.get3DTransform("body", TransformType.POSITION,  0f, Vec3f.ZERO);
        Vec3f rotation = animation.get3DTransform("body", TransformType.ROTATION,  0f, Vec3f.ZERO);
        Vec3f scale = animation.get3DTransform("body", TransformType.SCALE,  0f, new Vec3f(1, 1, 1));
        GlStateManager.scale(scale.getX(), scale.getY(), scale.getZ());
        GlStateManager.translate(position.getX(), position.getY() + .7f, position.getZ());
        rotate(rotation);
        GlStateManager.translate(0, -.7f, 0);
    }
    static void rotate(Vec3f rotation) {
        GlStateManager.rotate(rotation.getZ() * 57.29578f, 0, 0, 1);
        GlStateManager.rotate(rotation.getY() * 57.29578f, 0, 1, 0);
        GlStateManager.rotate(rotation.getX() * 57.29578f, 1, 0, 0);
    }
}
