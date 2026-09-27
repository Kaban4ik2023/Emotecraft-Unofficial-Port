package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

final class LegacyPlayerModel extends ModelPlayer {
    LegacyPlayerModel(boolean slim) {
        super(0,slim);wrap(this);
        bipedBodyWear=new DeformedPart(this,bipedBodyWear,6,true);
        bipedRightArmwear=new DeformedPart(this,bipedRightArmwear,4,false);
        bipedLeftArmwear=new DeformedPart(this,bipedLeftArmwear,4,false);
        bipedRightLegwear=new DeformedPart(this,bipedRightLegwear,6,false);
        bipedLeftLegwear=new DeformedPart(this,bipedLeftLegwear,6,false);
    }
    static void wrap(ModelBiped model) {
        model.bipedHead=new DeformedPart(model,model.bipedHead,0,false);
        model.bipedHeadwear=new DeformedPart(model,model.bipedHeadwear,0,false);
        model.bipedBody=new DeformedPart(model,model.bipedBody,6,true);
        model.bipedRightArm=new DeformedPart(model,model.bipedRightArm,4,false);
        model.bipedLeftArm=new DeformedPart(model,model.bipedLeftArm,4,false);
        model.bipedRightLeg=new DeformedPart(model,model.bipedRightLeg,6,false);
        model.bipedLeftLeg=new DeformedPart(model,model.bipedLeftLeg,6,false);
    }
    static void reset(ModelBiped model) {
        model.bipedHead.setRotationPoint(0,0,0);model.bipedHead.rotateAngleZ=0;
        model.bipedBody.setRotationPoint(0,0,0);
        model.bipedBody.rotateAngleX=model.bipedBody.rotateAngleY=model.bipedBody.rotateAngleZ=0;
        model.bipedRightArm.setRotationPoint(-5,2,0);model.bipedLeftArm.setRotationPoint(5,2,0);
        model.bipedRightLeg.setRotationPoint(-1.9f,12,0);model.bipedLeftLeg.setRotationPoint(1.9f,12,0);
        model.bipedLeftLeg.rotateAngleY=model.bipedLeftLeg.rotateAngleZ=0;
        model.bipedRightLeg.rotateAngleY=model.bipedRightLeg.rotateAngleZ=0;
        for(ModelRenderer part:new ModelRenderer[]{model.bipedHead,model.bipedHeadwear,model.bipedBody,model.bipedRightArm,model.bipedLeftArm,model.bipedRightLeg,model.bipedLeftLeg}) {
            DeformedPart p=(DeformedPart)part;p.bend=p.axis=0;p.partScale=new Vec3f(1,1,1);
        }
    }
    static void pose(ModelBiped model,Entity entity,float partial) {
        ClientProxy.Playback state=ClientProxy.INSTANCE.playing.get(entity.getUniqueID());
        if(state==null)return;
        KeyframeAnimationPlayer animation=state.animation;animation.setupAnim(Math.max(0,Math.min(1,partial)));
        apply(animation,"head",model.bipedHead);apply(animation,"torso",model.bipedBody);
        apply(animation,"rightArm",model.bipedRightArm);apply(animation,"leftArm",model.bipedLeftArm);
        apply(animation,"rightLeg",model.bipedRightLeg);apply(animation,"leftLeg",model.bipedLeftLeg);
        Vec3f bodyBend=animation.get3DTransform("body",TransformType.BEND,0,Vec3f.ZERO);
        DeformedPart torso=(DeformedPart)model.bipedBody;torso.axis+=bodyBend.getX();torso.bend+=bodyBend.getY();
        copy(model.bipedHead,model.bipedHeadwear);
    }
    private static void apply(KeyframeAnimationPlayer animation,String name,ModelRenderer part) {
        Vec3f p=animation.get3DTransform(name,TransformType.POSITION,0,new Vec3f(part.rotationPointX,part.rotationPointY,part.rotationPointZ));
        Vec3f r=animation.get3DTransform(name,TransformType.ROTATION,0,new Vec3f(part.rotateAngleX,part.rotateAngleY,part.rotateAngleZ));
        part.setRotationPoint(p.getX(),p.getY(),p.getZ());part.rotateAngleX=r.getX();part.rotateAngleY=r.getY();part.rotateAngleZ=r.getZ();
        DeformedPart b=(DeformedPart)part;
        Vec3f bend=animation.get3DTransform(name,TransformType.BEND,0,Vec3f.ZERO);
        if(!name.equals("head")){b.axis=bend.getX();b.bend=bend.getY();}
        b.partScale=animation.get3DTransform(name,TransformType.SCALE,0,new Vec3f(1,1,1));
    }
    @Override public void setRotationAngles(float limb,float amount,float age,float yaw,float pitch,float scale,Entity entity) {
        reset(this);super.setRotationAngles(limb,amount,age,yaw,pitch,scale,entity);pose(this,entity,age-entity.ticksExisted);
        copy(bipedRightArm,bipedRightArmwear);copy(bipedLeftArm,bipedLeftArmwear);
        copy(bipedRightLeg,bipedRightLegwear);copy(bipedLeftLeg,bipedLeftLegwear);copy(bipedBody,bipedBodyWear);
    }
    private static void copy(ModelRenderer from,ModelRenderer to) {
        copyModelAngles(from,to);DeformedPart a=(DeformedPart)from,b=(DeformedPart)to;
        b.bend=a.bend;b.axis=a.axis;b.partScale=a.partScale;
    }
    static void bodyBend(Entity entity) {
        ClientProxy.Playback state=ClientProxy.INSTANCE.playing.get(entity.getUniqueID());
        if(state!=null)bendMatrix(state.animation.get3DTransform("body",TransformType.BEND,0,Vec3f.ZERO),.375f);
    }
    static void bendMatrix(Vec3f bend,float pivot) {
        GlStateManager.translate(0,pivot,0);
        GlStateManager.rotate(bend.getY()*57.29578f,(float)Math.cos(-bend.getX()),0,(float)Math.sin(-bend.getX()));
        GlStateManager.translate(0,-pivot,0);
    }
    static void renderParts(ModelBiped model,Entity entity,float scale) {
        GlStateManager.pushMatrix();
        if(model.isSneak)GlStateManager.translate(0,.2f,0);
        model.bipedBody.render(scale);model.bipedRightLeg.render(scale);model.bipedLeftLeg.render(scale);
        if(model instanceof ModelPlayer) {
            ModelPlayer p=(ModelPlayer)model;p.bipedBodyWear.render(scale);p.bipedRightLegwear.render(scale);p.bipedLeftLegwear.render(scale);
        }
        bodyBend(entity);
        model.bipedHead.render(scale);model.bipedHeadwear.render(scale);model.bipedRightArm.render(scale);model.bipedLeftArm.render(scale);
        if(model instanceof ModelPlayer){ModelPlayer p=(ModelPlayer)model;p.bipedRightArmwear.render(scale);p.bipedLeftArmwear.render(scale);}
        GlStateManager.popMatrix();
    }
    @Override public void render(Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(limb,amount,age,yaw,pitch,scale,entity);renderParts(this,entity,scale);
    }
}
