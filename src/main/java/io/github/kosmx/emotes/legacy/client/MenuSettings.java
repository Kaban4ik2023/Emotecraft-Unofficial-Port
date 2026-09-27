package io.github.kosmx.emotes.legacy.client;

import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.legacy.Emotecraft;
import java.io.*;
import java.util.*;

public final class MenuSettings {
    private static final Properties values = new Properties();
    public static final Map<UUID,Integer> keys = new HashMap<>();
    public static final String[] OPTIONS = {"dark","icons","preview","perspective","front","music","otherPlayers","stopOnMove","autoReload"};
    public static final String[] LABELS = {"Тёмное колесо","Показывать иконки","Предпросмотр при выборе","Вид от третьего лица","Камера спереди","Музыка эмоций","Эмоции других игроков","Остановка при движении","Обновлять библиотеку автоматически"};
    private static File file() { return EmoteInstance.instance.getGameDirectory().resolve("config/emotecraft-interface.properties").toFile(); }
    public static boolean get(String key) { return Boolean.parseBoolean(values.getProperty(key, "front".equals(key) ? "false" : "true")); }
    public static void toggle(String key) { values.setProperty(key,Boolean.toString(!get(key)));save(); }
    public static void defaults() { for(String key:OPTIONS) values.remove(key);save(); }
    public static void load() {
        values.clear();keys.clear();
        if(file().isFile())try(InputStream in=new FileInputStream(file())) { values.load(in); }
        catch(IOException ex) { Emotecraft.log.warn("Cannot load interface settings",ex); }
        for(String key:values.stringPropertyNames())if(key.startsWith("key."))try {
            keys.put(UUID.fromString(key.substring(4)),Integer.parseInt(values.getProperty(key)));
        }catch(IllegalArgumentException ignored) { }
    }
    public static void bind(UUID id,int code) {
        keys.entrySet().removeIf(entry->entry.getValue()==code || entry.getKey().equals(id));
        if(code>0) keys.put(id,code);save();
    }
    public static void save() {
        values.keySet().removeIf(key->key.toString().startsWith("key."));
        keys.forEach((id,key)->values.setProperty("key."+id,key.toString()));
        try(OutputStream out=new FileOutputStream(file())) { values.store(out,"Emotecraft interface"); }
        catch(IOException ex) { Emotecraft.log.warn("Cannot save interface settings",ex); }
    }
}
