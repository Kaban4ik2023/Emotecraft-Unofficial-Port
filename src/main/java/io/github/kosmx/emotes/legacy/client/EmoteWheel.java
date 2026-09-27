package io.github.kosmx.emotes.legacy.client;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;
import java.io.IOException;
public final class EmoteWheel extends GuiScreen {
    private int selected=-1,x,y,size;
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void drawScreen(int mx,int my,float partial){
        drawRect(0,0,width,height,0x55000000);size=Math.min(height-55,280);x=(width-size)/2;y=(height-size)/2-8;
        selected=EmoteGraphics.wheel(x,y,size,mx,my);
        KeyframeAnimation emote=selected<0?null:ClientProxy.INSTANCE.find(ClientProxy.INSTANCE.slots[selected]);
        if(emote!=null)drawCenteredString(fontRenderer,ClientProxy.name(emote),width/2,12,0xFFFFFF);
        drawCenteredString(fontRenderer,"Отпустите клавишу — играть · ПКМ — настройки",width/2,height-24,0xDDDDDD);
        drawCenteredString(fontRenderer,"Колесо мыши — страница "+(ClientProxy.INSTANCE.wheelPage+1)+" / 3",width/2,height-12,0xAAAAAA);
    }
    @Override public void updateScreen(){if(!GameSettings.isKeyDown(ClientProxy.INSTANCE.wheel))choose();}
    private void choose(){mc.displayGuiScreen(null);if(selected>=0)ClientProxy.INSTANCE.play(ClientProxy.INSTANCE.find(ClientProxy.INSTANCE.slots[selected]));}
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{
        if(button==0 && Math.hypot(mx-x-size/2d,my-y-size/2d)<size*.17){ClientProxy.INSTANCE.changePage(mx<x+size/2?-1:1);return;}
        if(button==0)choose();else if(button==1)mc.displayGuiScreen(new EmoteMenu());
    }
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int delta=Mouse.getEventDWheel();if(delta!=0)ClientProxy.INSTANCE.changePage(delta>0?-1:1);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1)mc.displayGuiScreen(null);}
}
