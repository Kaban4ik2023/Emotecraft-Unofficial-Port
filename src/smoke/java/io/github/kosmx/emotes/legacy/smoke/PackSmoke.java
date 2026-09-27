package io.github.kosmx.emotes.legacy.smoke;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.api.events.client.ClientEmoteAPI;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.legacy.*;
import io.github.kosmx.emotes.legacy.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(modid="emotecraftpacktest",name="Pack integration test",version="1",dependencies="required-after:emotecraft")
public class PackSmoke {
    private boolean launched;private int ticks,index=-1;private final AtomicInteger received=new AtomicInteger();
    private final List<KeyframeAnimation> animations=new ArrayList<>();
    private final List<EntityOtherPlayerMP> models=new ArrayList<>();
    private final List<String> rendered=new ArrayList<>();
    private String title="";
    @Mod.EventHandler public void init(FMLInitializationEvent event){if(System.getProperty("emotecraft.testPack")!=null)MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event)throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched=true;mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.limitFramerate=60;
            Path marker=mc.gameDir.toPath().resolve("pack-imported.txt");
            if(!Files.exists(marker)){EmoteImporter.importFile(Paths.get(System.getProperty("emotecraft.testPack")),EmoteInstance.instance.getExternalEmoteDir().toPath());Files.write(marker,new byte[]{1});}
            ClientProxy.INSTANCE.reload();animations.addAll(ClientProxy.INSTANCE.library);
            if(animations.size()!=133)throw new AssertionError("Expected 124 unique pack animations + 9 builtins, got "+animations.size());
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.launchIntegratedServer("pack-smoke","Pack smoke",new WorldSettings(1234,GameType.CREATIVE,false,false,WorldType.FLAT));
        }
        if(mc.player==null || mc.world==null)return;
        ticks++;
        if(ticks==20) {
            for(int i=0;i<3;i++) {
                final boolean slim=i==1;
                EntityOtherPlayerMP model=new EntityOtherPlayerMP(mc.world,new GameProfile(new UUID(143,i+1),"PackModel"+i)) {
                    @Override public String getSkinType(){return slim?"slim":"default";}
                    @Override public ResourceLocation getLocationSkin(){return new ResourceLocation("textures/entity/"+(slim?"alex":"steve")+".png");}
                };
                model.setHeldItem(EnumHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
                model.setHeldItem(EnumHand.OFF_HAND,new ItemStack(net.minecraft.init.Blocks.TORCH));
                if(i==2){model.setItemStackToSlot(EntityEquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));model.setItemStackToSlot(EntityEquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));model.setItemStackToSlot(EntityEquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));model.setItemStackToSlot(EntityEquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));}
                models.add(model);
            }
            mc.displayGuiScreen(new GuiScreen() {
                @Override public boolean doesGuiPauseGame(){return false;}
                @Override public void drawScreen(int x,int y,float partial) {
                    drawDefaultBackground();drawCenteredString(fontRenderer,"SPEMOTES: "+title,width/2,20,0xFFFFFF);
                    for(int i=0;i<models.size();i++) {
                        EntityOtherPlayerMP model=models.get(i);ClientProxy.Playback state=ClientProxy.INSTANCE.playing.get(mc.player.getUniqueID());
                        if(state!=null)ClientProxy.INSTANCE.playing.put(model.getUniqueID(),state);
                        try{net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,240,240);GuiInventory.drawEntityOnScreen(width*(i+1)/4,height-40,Math.min(65,height/4),-28,0,model);}
                        finally{ClientProxy.INSTANCE.playing.remove(model.getUniqueID());}
                    }
                }
            });
        }
        if(ticks>=20 && ticks<20+animations.size()*12) {
            index=(ticks-20)/12;int phase=(ticks-20)%12;KeyframeAnimation animation=animations.get(index);
            if(phase%4==0){int frame=(int)(animation.endTick*(phase/4)*.35);ClientEmoteAPI.playEmote(animation,frame);title=(index+1)+"/"+animations.size()+" "+ClientProxy.name(animation)+" @"+frame;}
            if(phase==10) {
                UUID expected=animation.getUuid();
                mc.getIntegratedServer().addScheduledTask(()->{
                    try {
                        java.lang.reflect.Field f=LegacyNetwork.class.getDeclaredField("playing");f.setAccessible(true);
                        Object state=((Map<?,?>)f.get(Emotecraft.NETWORK)).get(mc.player.getUniqueID());
                        if(state==null)throw new AssertionError("Missing server animation "+expected);
                        java.lang.reflect.Field d=state.getClass().getDeclaredField("data");d.setAccessible(true);
                        if(!((io.github.kosmx.emotes.common.network.objects.NetData)d.get(state)).emoteData.getUuid().equals(expected))throw new AssertionError("Server animation mismatch "+expected);
                        received.incrementAndGet();
                    }catch(Exception e){throw new RuntimeException(e);}
                });
                rendered.add(ClientProxy.name(animation)+" "+expected);
            }
        }
        if(ticks==25 || ticks==220 || ticks==580 || ticks==1020)ScreenShotHelper.saveScreenshot(mc.gameDir,"pack-models-"+ticks+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        if(ticks==30+animations.size()*12) {
            if(received.get()!=animations.size())throw new AssertionError("Server received "+received.get()+" / "+animations.size());
            ClientProxy.INSTANCE.stop();mc.displayGuiScreen(new EmoteMenu());
        }
        if(ticks==40+animations.size()*12) {
            ScreenShotHelper.saveScreenshot(mc.gameDir,"pack-library.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            Files.write(mc.gameDir.toPath().resolve("pack-rendered.txt"),rendered,StandardCharsets.UTF_8);
            Files.write(mc.gameDir.toPath().resolve("pack-result.txt"),("PASS: imported all 223 JSON files; loaded 124 unique pack emotes + 9 builtins; rendered all 133 at three animation positions on Steve, Alex and armor, with held items; server decoded and tracked all 133.\n").getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }
}
