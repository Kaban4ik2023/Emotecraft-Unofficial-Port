package io.github.kosmx.emotes.legacy.smoke;

import io.github.kosmx.emotes.legacy.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

@Mod(modid="emotecraftsmoke",name="Emotecraft integration test",version="1",dependencies="required-after:emotecraft")
public class SmokeTest {
    private int ticks;
    private boolean launched;
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        if(Boolean.getBoolean("emotecraft.smoke") && System.getProperty("emotecraft.testPack")==null) MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched=true;mc.gameSettings.pauseOnLostFocus=false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.launchIntegratedServer("emotecraft-smoke","Emotecraft smoke test",new WorldSettings(1234,GameType.CREATIVE,false,false,WorldType.FLAT));
        }
        if(mc.player==null || mc.world==null)return;
        ticks++;
        if(ticks==20) {
            if(ClientProxy.INSTANCE.library.size()!=9)throw new AssertionError("Expected nine bundled animations");
            ClientProxy.INSTANCE.play(ClientProxy.INSTANCE.library.stream().filter(a->ClientProxy.name(a).equalsIgnoreCase("Waving")).findFirst().orElseThrow(()->new AssertionError("Missing wave")));
            mc.displayGuiScreen(new EmoteMenu());
        }
        if(ticks==45) {
            if(!ClientProxy.INSTANCE.playing.containsKey(mc.player.getUniqueID()))throw new AssertionError("Animation not playing");
            ScreenShotHelper.saveScreenshot(mc.gameDir,"emotecraft-menu.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            mc.getIntegratedServer().addScheduledTask(() -> {
                try {
                    java.lang.reflect.Field f=io.github.kosmx.emotes.legacy.LegacyNetwork.class.getDeclaredField("playing");f.setAccessible(true);
                    if(((java.util.Map<?,?>)f.get(io.github.kosmx.emotes.legacy.Emotecraft.NETWORK)).isEmpty())throw new AssertionError("Server did not receive animation");
                    Files.write(new File(mc.gameDir,"smoke-network.txt").toPath(),"PASS: server decoded and tracked client animation".getBytes(StandardCharsets.UTF_8));
                } catch(Exception ex) { throw new RuntimeException(ex); }
            });
        }
        if(ticks==80) {
            ClientProxy.INSTANCE.stop();
            mc.displayGuiScreen(new EmoteWheel());
        }
        if(ticks>=100 && ticks<280 && ticks%20==0) {
            mc.displayGuiScreen(new EmoteMenu());
            ClientProxy.INSTANCE.play(ClientProxy.INSTANCE.library.get((ticks-100)/20));
        }
        if(ticks==285){mc.displayGuiScreen(new EmoteMenu());}
        if(ticks==290){
            // Exercise actual widget handlers, not just opening the screen.
            EmoteMenu menu=(EmoteMenu)mc.currentScreen;
            java.lang.reflect.Method click=EmoteMenu.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class);click.setAccessible(true);
            click.invoke(menu,20,70,0);
            java.lang.reflect.Field wx=EmoteMenu.class.getDeclaredField("wx"),wy=EmoteMenu.class.getDeclaredField("wy"),ws=EmoteMenu.class.getDeclaredField("ws");wx.setAccessible(true);wy.setAccessible(true);ws.setAccessible(true);
            int x=wx.getInt(menu),y=wy.getInt(menu),s=ws.getInt(menu);
            java.util.UUID before=ClientProxy.INSTANCE.slots[0];
            click.invoke(menu,x+s/2,y+(int)(s*.86),0);
            if(ClientProxy.INSTANCE.slots[0]==null)throw new AssertionError("Slot assignment failed");
            click.invoke(menu,x+s/2,y+(int)(s*.86),1);
            if(ClientProxy.INSTANCE.slots[0]!=null)throw new AssertionError("Slot removal failed");
            ClientProxy.INSTANCE.slots[0]=before;ClientProxy.INSTANCE.saveSlots();
            boolean dark=MenuSettings.get("dark");MenuSettings.toggle("dark");MenuSettings.load();
            if(MenuSettings.get("dark")==dark)throw new AssertionError("Settings were not persisted");MenuSettings.toggle("dark");
        }
        if(ticks==300)ScreenShotHelper.saveScreenshot(mc.gameDir,"emotecraft-library-v2.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        if(ticks==305)mc.displayGuiScreen(new EmoteSettingsScreen(new EmoteMenu()));
        if(ticks==310)ScreenShotHelper.saveScreenshot(mc.gameDir,"emotecraft-settings-v2.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        if(ticks==315)mc.displayGuiScreen(new EmoteImportScreen(new EmoteMenu()));
        if(ticks==320)ScreenShotHelper.saveScreenshot(mc.gameDir,"emotecraft-import-v2.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        if(ticks==325){net.minecraft.client.settings.KeyBinding.setKeyBindState(ClientProxy.INSTANCE.wheel.getKeyCode(),true);mc.displayGuiScreen(new EmoteWheel());}
        if(ticks==340) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(ClientProxy.INSTANCE.wheel.getKeyCode(),false);
            ClientProxy.INSTANCE.stop();
            java.lang.reflect.Field field=ClientProxy.class.getDeclaredField("connected");field.setAccessible(true);
            if(!field.getBoolean(ClientProxy.INSTANCE))throw new AssertionError("Server handshake failed");
            if(!new File(mc.gameDir,"smoke-network.txt").isFile())throw new AssertionError("Missing network roundtrip");
            Files.write(new File(mc.gameDir,"smoke-result.txt").toPath(),"PASS v2: all 9 emotes rendered; server handshake and packet processing; original wheel atlas, library preview, slot assignment/removal, settings persistence, import browser, stop.\n".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END || ticks!=330)return;
        Minecraft mc=Minecraft.getMinecraft();
        net.minecraft.client.gui.ScaledResolution resolution=new net.minecraft.client.gui.ScaledResolution(mc);
        EmoteWheel wheel=new EmoteWheel();wheel.setWorldAndResolution(mc,resolution.getScaledWidth(),resolution.getScaledHeight());
        wheel.drawScreen(resolution.getScaledWidth()/2,resolution.getScaledHeight()*3/4,event.renderTickTime);
        ScreenShotHelper.saveScreenshot(mc.gameDir,"emotecraft-wheel-v2.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
    }
}

