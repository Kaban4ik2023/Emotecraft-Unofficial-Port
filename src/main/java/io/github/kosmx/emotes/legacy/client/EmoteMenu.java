package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import io.github.kosmx.emotes.executor.EmoteInstance;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class EmoteMenu extends GuiScreen {
    private GuiTextField search;
    private KeyframeAnimation selected;
    private ClientProxy.Playback preview;
    private final List<KeyframeAnimation> filtered=new ArrayList<>();
    private int offset,listWidth,rows,wx,wy,ws,ticks;
    private String query="",status="";
    private boolean binding;
    private long lastClick,folderStamp;
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void initGui(){
        Keyboard.enableRepeatEvents(true);listWidth=Math.max(118,width*2/5);
        search=new GuiTextField(0,fontRenderer,12,32,listWidth-22,20);search.setMaxStringLength(100);search.setText(query);
        rows=Math.max(1,(height-112)/30);
        int available=width-listWidth-24;ws=Math.max(72,Math.min(height-150,available-62));wx=listWidth+12;wy=74;
        refresh();if(selected==null&&!filtered.isEmpty())select(filtered.get(0));folderStamp=stamp();
    }
    private void refresh(){
        UUID id=selected==null?null:selected.getUuid();filtered.clear();
        for(KeyframeAnimation emote:ClientProxy.INSTANCE.library)
            if((ClientProxy.name(emote)+" "+text(emote,"author")).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))filtered.add(emote);
        if(id!=null)selected=ClientProxy.INSTANCE.find(id);
        offset=Math.max(0,Math.min(offset,Math.max(0,filtered.size()-rows)));buttonList.clear();
        int rx=listWidth+12,rw=width-rx-12;
        int key=selected==null?0:MenuSettings.keys.getOrDefault(selected.getUuid(),0);
        addButton(new GuiButton(10,rx,32,rw-65,20,binding?"Нажмите клавишу…":("Клавиша: "+(key==0?"не задана":Keyboard.getKeyName(key))))).enabled=selected!=null;
        addButton(new GuiButton(11,rx+rw-61,32,61,20,"Сброс")).enabled=selected!=null;
        addButton(new GuiButton(12,rx,height-59,(rw-4)/2,20,"Играть")).enabled=selected!=null;
        addButton(new GuiButton(13,rx+(rw-4)/2+4,height-59,(rw-4)/2,20,"Стоп"));
        addButton(new GuiButton(14,12,height-59,60,20,"Экспорт")).enabled=selected!=null;
        int count=5,bw=(width-24-4*(count-1))/count;
        String[] labels={"Папка","Загрузить","Обновить","Настройки","Готово"};
        for(int i=0;i<count;i++)addButton(new GuiButton(i,12+i*(bw+4),height-26,bw,20,labels[i]));
    }
    private static String text(KeyframeAnimation animation,String key){
        String raw=String.valueOf(animation.extraData.getOrDefault(key,""));
        try{net.minecraft.util.text.ITextComponent t=net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(raw);if(t!=null)return t.getUnformattedText();}catch(RuntimeException ignored){}return raw;
    }
    private void select(KeyframeAnimation emote){selected=emote;preview=mc.player==null?null:new ClientProxy.Playback(emote,0,mc.player.getUniqueID());binding=false;refresh();}
    @Override protected void actionPerformed(GuiButton b)throws IOException{
        ClientProxy client=ClientProxy.INSTANCE;
        switch(b.id){
            case 0:try{java.awt.Desktop.getDesktop().open(EmoteInstance.instance.getExternalEmoteDir());}catch(Exception ex){status=EmoteInstance.instance.getExternalEmoteDir().toString();}break;
            case 1:mc.displayGuiScreen(new EmoteImportScreen(this));break;
            case 2:client.reload();refresh();status="Библиотека обновлена";break;
            case 3:mc.displayGuiScreen(new EmoteSettingsScreen(this));break;
            case 4:mc.displayGuiScreen(null);break;
            case 10:binding=true;refresh();break;
            case 11:if(selected!=null)MenuSettings.bind(selected.getUuid(),0);binding=false;refresh();break;
            case 12:client.play(selected);break;
            case 13:client.stop();break;
            case 14:if(selected!=null){
                Path folder=EmoteInstance.instance.getExternalEmoteDir().toPath().resolve("_export");Files.createDirectories(folder);
                try(Writer writer=Files.newBufferedWriter(folder.resolve(selected.getUuid()+".json"),StandardCharsets.UTF_8)){AnimationSerializing.writeAnimation(selected,writer);status="Сохранено в emotes/_export";}
            }break;
        }
    }
    @Override public void drawScreen(int mx,int my,float partial){
        drawDefaultBackground();drawCenteredString(fontRenderer,"Настройка эмоций",width/2,11,0xFFFFFF);search.drawTextBox();
        if(query.isEmpty()&&!search.isFocused())fontRenderer.drawStringWithShadow("Поиск эмоции или автора…",16,38,0x777777);
        drawRect(10,58,listWidth-8,58+rows*30,0x88000000);
        int hover=-1;
        for(int row=0;row<rows && offset+row<filtered.size();row++){
            KeyframeAnimation emote=filtered.get(offset+row);int y=58+row*30;
            boolean over=mx>=12&&mx<listWidth-10&&my>=y&&my<y+30;if(over)hover=offset+row;
            if(emote==selected||over)drawRect(11,y,listWidth-9,y+29,emote==selected?0xAA42623B:0x66555555);
            EmoteGraphics.icon(emote,15,y+3,26);
            fontRenderer.drawStringWithShadow(fontRenderer.trimStringToWidth(ClientProxy.name(emote),listWidth-65),47,y+5,0xFFFFFF);
            fontRenderer.drawStringWithShadow(fontRenderer.trimStringToWidth(text(emote,"author"),listWidth-65),47,y+18,0xAAAAAA);
        }
        if(filtered.size()>rows){int h=rows*30;int thumb=Math.max(12,h*rows/filtered.size());int sy=58+(h-thumb)*offset/(filtered.size()-rows);drawRect(listWidth-10,sy,listWidth-7,sy+thumb,0xFFAAAAAA);}
        if(filtered.isEmpty())drawCenteredString(fontRenderer,"Ничего не найдено",listWidth/2,80,0xAAAAAA);
        fontRenderer.drawStringWithShadow(filtered.size()+" эмоций",78,height-53,0xAAAAAA);
        drawCenteredString(fontRenderer,"ЛКМ: назначить • ПКМ: убрать",(listWidth+width)/2,62,0xBBBBBB);
        int part=EmoteGraphics.wheel(wx,wy,ws,mx,my);
        int px=wx+ws+Math.max(22,(width-wx-ws)/2)-6,py=wy+ws-4;
        if(mc.player!=null && preview!=null && MenuSettings.get("preview")){
            UUID owner=mc.player.getUniqueID();ClientProxy.Playback original=ClientProxy.INSTANCE.playing.put(owner,preview);
            try{net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,240,240);GuiInventory.drawEntityOnScreen(px,py,Math.max(14,Math.min(32,ws/3)),px-mx,py-ws/2-my,mc.player);}
            finally{if(original==null)ClientProxy.INSTANCE.playing.remove(owner);else ClientProxy.INSTANCE.playing.put(owner,original);}
        }
        drawCenteredString(fontRenderer,"<  Страница "+(ClientProxy.INSTANCE.wheelPage+1)+" / 3  >",wx+ws/2,wy+ws+6,0xAAAAAA);
        super.drawScreen(mx,my,partial);
        if(!status.isEmpty())drawCenteredString(fontRenderer,fontRenderer.trimStringToWidth(status,width-24),width/2,height-37,0xAACC99);
        if(hover>=0){KeyframeAnimation emote=filtered.get(hover);List<String> tip=new ArrayList<>();tip.add(ClientProxy.name(emote));String desc=text(emote,"description");if(!desc.isEmpty())tip.addAll(fontRenderer.listFormattedStringToWidth(desc,200));tip.add("§7Двойной щелчок — играть");drawHoveringText(tip,mx,my);}
        else if(part>=0){KeyframeAnimation emote=ClientProxy.INSTANCE.find(ClientProxy.INSTANCE.slots[part]);drawHoveringText(emote==null?"Пустой слот":ClientProxy.name(emote),mx,my);}
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{
        search.mouseClicked(x,y,button);
        if(x>=12&&x<listWidth-10&&y>=58&&y<58+rows*30){int index=offset+(y-58)/30;if(index<filtered.size()){
            KeyframeAnimation emote=filtered.get(index);long now=System.currentTimeMillis();boolean twice=emote==selected&&now-lastClick<300;select(emote);lastClick=now;if(twice)ClientProxy.INSTANCE.play(emote);return;
        }}
        int slot=EmoteGraphics.hit(wx,wy,ws,x,y);
        if(slot>=0){if(button==1)ClientProxy.INSTANCE.slots[slot]=null;else if(button==0&&selected!=null)ClientProxy.INSTANCE.slots[slot]=selected.getUuid();ClientProxy.INSTANCE.saveSlots();return;}
        if(x>wx+ws*.33&&x<wx+ws*.67&&y>wy+ws*.33&&y<wy+ws*.67){ClientProxy.INSTANCE.changePage(x<wx+ws/2?-1:1);return;}
        super.mouseClicked(x,y,button);
    }
    @Override public void handleMouseInput()throws IOException{
        super.handleMouseInput();int delta=Mouse.getEventDWheel();if(delta==0)return;int x=Mouse.getEventX()*width/mc.displayWidth;
        if(x<listWidth){offset+=delta>0?-2:2;refresh();}else ClientProxy.INSTANCE.changePage(delta>0?-1:1);
    }
    @Override protected void keyTyped(char c,int key)throws IOException{
        if(binding&&selected!=null){MenuSettings.bind(selected.getUuid(),key==1||key==14||key==211?0:key);binding=false;refresh();return;}
        if(search.textboxKeyTyped(c,key)){query=search.getText();offset=0;refresh();}
        else if(key==28&&selected!=null)ClientProxy.INSTANCE.play(selected);else super.keyTyped(c,key);
    }
    private long stamp(){
        try(java.util.stream.Stream<Path> files=Files.walk(EmoteInstance.instance.getExternalEmoteDir().toPath())){
            return files.filter(Files::isRegularFile).mapToLong(p->{try{return Files.getLastModifiedTime(p).toMillis()+Files.size(p);}catch(IOException ex){return 0;}}).sum();
        }catch(IOException ex){return 0;}
    }
    @Override public void updateScreen(){search.updateCursorCounter();if(preview!=null){preview.animation.tick();if(!preview.animation.isActive()&&selected!=null)preview=new ClientProxy.Playback(selected,0,mc.player.getUniqueID());}
        if(++ticks%40==0&&MenuSettings.get("autoReload")){long current=stamp();if(current!=folderStamp){folderStamp=current;ClientProxy.INSTANCE.reload();refresh();}}
    }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
}
