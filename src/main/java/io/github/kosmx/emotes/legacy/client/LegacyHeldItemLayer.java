package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

final class LegacyHeldItemLayer extends LayerHeldItem {
    LegacyHeldItemLayer(RenderLivingBase<?> renderer){super(renderer);}
    @Override public void doRenderLayer(EntityLivingBase entity,float limb,float amount,float partial,float age,float yaw,float pitch,float scale) {
        boolean right=entity.getPrimaryHand()==EnumHandSide.RIGHT;
        render(entity,right?entity.getHeldItemMainhand():entity.getHeldItemOffhand(),EnumHandSide.RIGHT,partial);
        render(entity,right?entity.getHeldItemOffhand():entity.getHeldItemMainhand(),EnumHandSide.LEFT,partial);
    }
    private void render(EntityLivingBase entity,ItemStack item,EnumHandSide side,float partial) {
        if(item.isEmpty())return;
        boolean left=side==EnumHandSide.LEFT;
        ClientProxy.Playback state=ClientProxy.INSTANCE.playing.get(entity.getUniqueID());
        KeyframeAnimationPlayer animation=state==null?null:state.animation;
        if(animation!=null)animation.setupAnim(partial);
        GlStateManager.pushMatrix();
        try {
            if(entity.isSneaking())GlStateManager.translate(0,.2f,0);
            LegacyPlayerModel.bodyBend(entity);translateToHand(side);
            ModelBiped model=(ModelBiped)livingEntityRenderer.getMainModel();
            DeformedPart arm=(DeformedPart)(left?model.bipedLeftArm:model.bipedRightArm);
            GlStateManager.scale(arm.partScale.getX(),arm.partScale.getY(),arm.partScale.getZ());
            if(animation!=null)LegacyPlayerModel.bendMatrix(animation.get3DTransform(left?"leftArm":"rightArm",TransformType.BEND,0,Vec3f.ZERO),.25f);
            GlStateManager.rotate(-90,1,0,0);GlStateManager.rotate(180,0,1,0);
            GlStateManager.translate((left?-1:1)/16f,.125f,-.625f);
            if(animation!=null) {
                String name=left?"leftItem":"rightItem";
                Vec3f s=animation.get3DTransform(name,TransformType.SCALE,0,new Vec3f(1,1,1));
                Vec3f p=animation.get3DTransform(name,TransformType.POSITION,0,Vec3f.ZERO);
                GlStateManager.scale(s.getX(),s.getY(),s.getZ());GlStateManager.translate(p.getX()/16,p.getY()/16,p.getZ()/16);
                LegacyPlayerRenderer.rotate(animation.get3DTransform(name,TransformType.ROTATION,0,Vec3f.ZERO));
            }
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity,item,left?ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND:ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,left);
        }finally{GlStateManager.popMatrix();}
    }
}
