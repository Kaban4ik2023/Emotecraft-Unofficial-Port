package io.github.kosmx.emotes.legacy.client;

import io.github.kosmx.emotes.executor.EmoteInstance;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import java.io.*;
import java.util.*;

public final class EmoteImportScreen extends GuiScreen {
    private final GuiScreen parent;private File directory;private File selected;
    private List<File> entries=new ArrayList<>();private int offset;private String status="Выберите эмоцию или ZIP-пакет";
    private GuiTextField path;private boolean busy;
    public EmoteImportScreen(GuiScreen parent) {this.parent=parent;directory=new File(System.getProperty("user.home"),"Downloads");if(!directory.isDirectory())directory=new File(System.getProperty("user.home"));}
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void initGui(){path=new GuiTextField(0,fontRenderer,12,30,width-78,20);path.setMaxStringLength(2048);path.setText(directory.getAbsolutePath());refresh();}
    private void refresh(){
        File[] list=directory.listFiles(EmoteImporter::selectable);entries.clear();if(list!=null)entries.addAll(Arrays.asList(list));
        entries.sort(Comparator.comparing((File f)->!f.isDirectory()).thenComparing(File::getName,String.CASE_INSENSITIVE_ORDER));
        int rows=Math.max(1,(height-124)/22);offset=Math.max(0,Math.min(offset,Math.max(0,entries.size()-rows)));buttonList.clear();
        addButton(new GuiButton(1,width-62,30,50,20,"Открыть"));
        addButton(new GuiButton(2,12,54,70,20,".. Назад"));
        addButton(new GuiButton(3,86,54,75,20,"Загрузки"));
        for(int i=0;i<rows && offset+i<entries.size();i++) {
            File f=entries.get(offset+i);String label=(f.equals(selected)?"§e":"")+(f.isDirectory()?"[+] ":"")+f.getName();
            addButton(new GuiButton(100+i,12,80+i*22,width-24,20,fontRenderer.trimStringToWidth(label,width-40)));
        }
        addButton(new GuiButton(4,width/2-104,height-28,100,20,busy?"Загрузка…":"Загрузить")).enabled=!busy && selected!=null && selected.isFile();
        addButton(new GuiButton(5,width/2+4,height-28,100,20,"Назад"));
    }
    private void navigate(File next){if(next.isDirectory()){directory=next;selected=null;offset=0;path.setText(next.getAbsolutePath());refresh();}else status="Папка не найдена";}
    @Override protected void actionPerformed(GuiButton button){
        if(button.id>=100){File file=entries.get(offset+button.id-100);if(file.isDirectory())navigate(file);else{selected=file;refresh();}}
        else if(button.id==1)navigate(new File(path.getText()));
        else if(button.id==2 && directory.getParentFile()!=null)navigate(directory.getParentFile());
        else if(button.id==3)navigate(new File(System.getProperty("user.home"),"Downloads"));
        else if(button.id==5)mc.displayGuiScreen(parent);
        else if(button.id==4 && selected!=null){
            busy=true;refresh();final File input=selected;
            Thread worker=new Thread(()->{
                String result;
                try{EmoteImporter.importFile(input.toPath(),EmoteInstance.instance.getExternalEmoteDir().toPath());result="Загружено: "+input.getName();}
                catch(IOException ex){result=ex.getMessage();}
                final String message=result;
                mc.addScheduledTask(()->{busy=false;status=message;ClientProxy.INSTANCE.reload();refresh();});
            },"Emotecraft import");worker.setDaemon(true);worker.start();
        }
    }
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int d=Mouse.getEventDWheel();if(d!=0){offset+=d>0?-3:3;refresh();}}
    @Override protected void mouseClicked(int x,int y,int b)throws IOException{path.mouseClicked(x,y,b);super.mouseClicked(x,y,b);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1)mc.displayGuiScreen(parent);else if(key==28)navigate(new File(path.getText()));else path.textboxKeyTyped(c,key);}
    @Override public void updateScreen(){path.updateCursorCounter();}
    @Override public void drawScreen(int x,int y,float p){drawDefaultBackground();drawCenteredString(fontRenderer,"Загрузка эмоций",width/2,12,0xFFFFFF);path.drawTextBox();super.drawScreen(x,y,p);fontRenderer.drawStringWithShadow("Прокрутка: колесо мыши",170,60,0xAAAAAA);drawCenteredString(fontRenderer,fontRenderer.trimStringToWidth(status,width-24),width/2,height-42,0xCCCCCC);}
}
