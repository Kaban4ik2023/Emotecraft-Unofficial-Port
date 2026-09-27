package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.*;
import org.lwjgl.opengl.GL11;

/** Uses vanilla UVs, including slim arms, mirrored skins and armor. */
final class DeformedPart extends ModelRenderer {
    private static final Field QUADS=ReflectionHelper.findField(ModelBox.class,"quadList","field_78254_i");
    float bend,axis;
    Vec3f partScale=new Vec3f(1,1,1);
    private final float joint;
    private final boolean upper;
    private final List<Face> mesh=new ArrayList<>();
    DeformedPart(ModelBase owner,ModelRenderer source,float joint,boolean upper) {
        super(owner);this.joint=joint;this.upper=upper;
        cubeList.addAll(source.cubeList);childModels=source.childModels;mirror=source.mirror;
        ModelBase.copyModelAngles(source,this);
        for(ModelBox cube:cubeList)try {
            TexturedQuad[] quads=(TexturedQuad[])QUADS.get(cube);
            float top=Float.POSITIVE_INFINITY,bottom=Float.NEGATIVE_INFINITY;
            for(TexturedQuad quad:quads)for(PositionTextureVertex v:quad.vertexPositions){top=Math.min(top,(float)v.vector3D.y);bottom=Math.max(bottom,(float)v.vector3D.y);}
            for(TexturedQuad quad:quads) {
                List<Vertex> vertices=new ArrayList<>();
                for(PositionTextureVertex v:quad.vertexPositions)vertices.add(new Vertex((float)v.vector3D.x,(float)v.vector3D.y,(float)v.vector3D.z,v.texturePositionX,v.texturePositionY));
                if(top<joint && bottom>joint){add(clip(vertices,true),cube,top,bottom);add(clip(vertices,false),cube,top,bottom);}
                else add(vertices,cube,top,bottom);
            }
        }catch(IllegalAccessException ex){throw new IllegalStateException("Cannot read skin geometry",ex);}
    }
    private void add(List<Vertex> vertices,ModelBox cube,float top,float bottom) {
        if(vertices.size()>=3)mesh.add(new Face(vertices,(cube.posX1+cube.posX2)/2,(cube.posZ1+cube.posZ2)/2,top,bottom));
    }
    private List<Vertex> clip(List<Vertex> input,boolean above) {
        List<Vertex> output=new ArrayList<>();Vertex previous=input.get(input.size()-1);
        boolean previousInside=above?previous.y<=joint:previous.y>=joint;
        for(Vertex current:input) {
            boolean inside=above?current.y<=joint:current.y>=joint;
            if(inside!=previousInside){float t=(joint-previous.y)/(current.y-previous.y);output.add(previous.lerp(current,t));}
            if(inside)output.add(current);
            previous=current;previousInside=inside;
        }
        return output;
    }
    @Override public void render(float scale) {
        if(isHidden||!showModel)return;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(offsetX,offsetY,offsetZ);
            GlStateManager.translate(rotationPointX*scale,rotationPointY*scale,rotationPointZ*scale);
            LegacyPlayerRenderer.rotate(new Vec3f(rotateAngleX,rotateAngleY,rotateAngleZ));
            GlStateManager.scale(partScale.getX(),partScale.getY(),partScale.getZ());
            BufferBuilder buffer=Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_TRIANGLES,DefaultVertexFormats.POSITION_TEX_NORMAL);
            for(Face face:mesh) {
                Vec3d[] points=new Vec3d[face.vertices.size()];
                for(int i=0;i<points.length;i++) {
                    Vertex v=face.vertices.get(i);
                    if(Math.abs(bend)<.0001f)points[i]=new Vec3d(v.x,v.y,v.z);
                    else {float[] p=BendGeometry.transform(v.x-face.cx,v.y,v.z-face.cz,joint,face.top,face.bottom,axis,bend,upper);points[i]=new Vec3d(p[0]+face.cx,p[1],p[2]+face.cz);}
                }
                for(int i=1;i<points.length-1;i++) {
                    Vec3d normal=points[i].subtract(points[0]).crossProduct(points[i+1].subtract(points[0])).normalize();
                    for(int index:new int[]{0,i,i+1}) {
                        Vec3d p=points[index];Vertex v=face.vertices.get(index);
                        buffer.pos(p.x*scale,p.y*scale,p.z*scale).tex(v.u,v.v).normal((float)normal.x,(float)normal.y,(float)normal.z).endVertex();
                    }
                }
            }
            Tessellator.getInstance().draw();
            if(childModels!=null)for(ModelRenderer child:childModels)child.render(scale);
        }finally{GlStateManager.popMatrix();}
    }
    private static final class Face {
        final List<Vertex> vertices;final float cx,cz,top,bottom;
        Face(List<Vertex> vertices,float cx,float cz,float top,float bottom){this.vertices=vertices;this.cx=cx;this.cz=cz;this.top=top;this.bottom=bottom;}
    }
    private static final class Vertex {
        final float x,y,z,u,v;
        Vertex(float x,float y,float z,float u,float v){this.x=x;this.y=y;this.z=z;this.u=u;this.v=v;}
        Vertex lerp(Vertex b,float t){return new Vertex(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t,u+(b.u-u)*t,v+(b.v-v)*t);}
    }
}
