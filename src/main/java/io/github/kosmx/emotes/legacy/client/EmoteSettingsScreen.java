package io.github.kosmx.emotes.legacy.client;

import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.server.config.Serializer;
import net.minecraft.client.gui.*;
import java.io.IOException;
import org.lwjgl.input.Mouse;

public final class EmoteSettingsScreen extends GuiScreen {
    private final GuiScreen parent;private int offset;
    public EmoteSettingsScreen(GuiScreen parent) { this.parent=parent; }
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void initGui() {
        buttonList.clear();int rows=Math.max(1,(height-80)/24);offset=Math.max(0,Math.min(offset,12-rows));
        for(int i=offset;i<Math.min(12,offset+rows);i++) {
            String label;boolean value;
            if(i<9){label=MenuSettings.LABELS[i];value=MenuSettings.get(MenuSettings.OPTIONS[i]);}
            else if(i==9){label="Встроенные эмоции";value=EmoteInstance.config.loadBuiltinEmotes.get();}
            else if(i==10){label="Импорт формата Quark";value=EmoteInstance.config.enableQuark.get();}
            else {label="Исправлять конец анимации";value=EmoteInstance.config.autoFixEmoteStop.get();}
            addButton(new GuiButton(i,width/2-Math.min(190,width/2-12),36+(i-offset)*24,Math.min(380,width-24),20,label+": "+(value?"§aВКЛ":"§cВЫКЛ")));
        }
        addButton(new GuiButton(90,width/2-154,height-28,100,20,"По умолчанию"));
        addButton(new GuiButton(91,width/2-50,height-28,100,20,"Управление"));
        addButton(new GuiButton(92,width/2+54,height-28,100,20,"Готово"));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id<9)MenuSettings.toggle(MenuSettings.OPTIONS[b.id]);
        else if(b.id==9)EmoteInstance.config.loadBuiltinEmotes.set(!EmoteInstance.config.loadBuiltinEmotes.get());
        else if(b.id==10)EmoteInstance.config.enableQuark.set(!EmoteInstance.config.enableQuark.get());
        else if(b.id==11)EmoteInstance.config.autoFixEmoteStop.set(!EmoteInstance.config.autoFixEmoteStop.get());
        else if(b.id==90){MenuSettings.defaults();EmoteInstance.config.loadBuiltinEmotes.set(true);EmoteInstance.config.enableQuark.set(false);EmoteInstance.config.autoFixEmoteStop.set(true);}
        else if(b.id==91){mc.displayGuiScreen(new GuiControls(this,mc.gameSettings));return;}
        else if(b.id==92){mc.displayGuiScreen(parent);return;}
        Serializer.saveConfig();ClientProxy.INSTANCE.reload();initGui();
    }
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int d=Mouse.getEventDWheel();if(d!=0){offset+=d>0?-1:1;initGui();}}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1)mc.displayGuiScreen(parent);else super.keyTyped(c,key);}
    @Override public void drawScreen(int x,int y,float p){drawDefaultBackground();drawCenteredString(fontRenderer,"Настройки Emotecraft",width/2,12,0xFFFFFF);drawCenteredString(fontRenderer,"Прокрутите вниз, чтобы увидеть остальные настройки",width/2,height-43,0xAAAAAA);super.drawScreen(x,y,p);}
}
