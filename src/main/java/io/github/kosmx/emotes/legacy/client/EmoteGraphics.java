package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;

public final class EmoteGraphics extends Gui {
    private static final Map<UUID,ResourceLocation> icons=new HashMap<>();
    public static void clear() { for(ResourceLocation icon:icons.values())if(icon!=null)Minecraft.getMinecraft().getTextureManager().deleteTexture(icon);icons.clear(); }
    public static void icon(KeyframeAnimation emote,int x,int y,int size) {
        if(emote==null)return;
        Minecraft mc=Minecraft.getMinecraft();ResourceLocation icon=icons.get(emote.getUuid());
        if(!icons.containsKey(emote.getUuid())) {
            Object data=emote.extraData.get("iconData");
            if(data instanceof ByteBuffer)try {
                ByteBuffer buffer=((ByteBuffer)data).duplicate();buffer.rewind();byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);
                java.awt.image.BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));
                if(image!=null && image.getWidth()<=1024 && image.getHeight()<=1024)
                    icon=mc.getTextureManager().getDynamicTextureLocation("emote_icon",new DynamicTexture(image));
            }catch(Exception ignored) { }
            icons.put(emote.getUuid(),icon);
        }
        if(icon!=null && MenuSettings.get("icons")) {
            mc.getTextureManager().bindTexture(icon);GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();
            drawScaledCustomSizeModalRect(x,y,0,0,256,256,size,size,256,256);
        }else {
            String name=ClientProxy.name(emote);mc.fontRenderer.drawStringWithShadow(name.isEmpty()?"?":name.substring(0,1),x+size/2-3,y+size/2-4,0xFFFFFF);
        }
    }
    public static int hit(int x,int y,int size,int mx,int my) {
        double dx=mx-x-size/2d,dy=my-y-size/2d,r=Math.hypot(dx,dy);
        if(r<size*.17 || r>size*.5)return -1;
        return ((int)Math.floor((Math.PI/2-Math.atan2(dy,dx)+Math.PI*2+Math.PI/8)/(Math.PI/4)))%8;
    }
    public static int wheel(int x,int y,int size,int mx,int my) {
        Minecraft mc=Minecraft.getMinecraft();
        ResourceLocation texture=new ResourceLocation("emotecraft","textures/gui/fastchoose_"+(MenuSettings.get("dark")?"dark":"light")+"_new.png");
        mc.getTextureManager().bindTexture(texture);GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();
        drawScaledCustomSizeModalRect(x,y,0,0,512,512,size,size,1024,1024);
        int selected=hit(x,y,size,mx,my);
        // Original atlas: lower, lower-right, right, upper-right, upper, upper-left, left, lower-left.
        int[][] patches={{0,256,0,384,2,1},{256,256,384,384,1,1},{256,0,384,0,1,2},{256,0,384,256,1,1},{0,0,0,256,2,1},{0,0,256,256,1,1},{0,0,256,0,1,2},{0,256,256,384,1,1}};
        if(selected>=0) { int[] p=patches[selected];
            drawScaledCustomSizeModalRect(x+p[0]*size/512,y+p[1]*size/512,p[2]*2,p[3]*2,p[4]*256,p[5]*256,p[4]*size/2,p[5]*size/2,1024,1024);
        }
        for(int i=0;i<8;i++) {
            // Angles and slot order match the original wheel.
            double angle=i*Math.PI/4;int s=Math.max(10,size/8);
            int ix=x+size/2+(int)(Math.sin(angle)*size*.36)-s/2,iy=y+size/2+(int)(Math.cos(angle)*size*.36)-s/2;
            KeyframeAnimation emote=ClientProxy.INSTANCE.find(ClientProxy.INSTANCE.slots[i]);
            if(emote!=null)icon(emote,ix,iy,s);
            else mc.fontRenderer.drawStringWithShadow(""+(i+1),ix+s/2-3,iy+s/2-4,0xAAAAAA);
        }
        mc.fontRenderer.drawStringWithShadow(""+(ClientProxy.INSTANCE.wheelPage+1),x+size/2-3,y+size/2-4,0xFFFFFF);
        return selected;
    }
}
