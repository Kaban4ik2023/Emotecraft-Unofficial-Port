package io.github.kosmx.emotes.legacy.client;

import io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class EmoteImporter {
    public static boolean animation(String name){return name.toLowerCase(Locale.ROOT).matches(".*\\.(json|emotecraft|emote)$");}
    public static boolean selectable(File file){return file.isDirectory()||animation(file.getName())||file.getName().toLowerCase(Locale.ROOT).endsWith(".zip");}
    private static byte[] read(InputStream in)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Файл больше 4 МБ");out.write(buffer,0,n);}return out.toByteArray();
    }
    public static Path importFile(Path source,Path directory)throws IOException {
        directory=directory.toAbsolutePath().normalize();
        Files.createDirectories(directory.getParent());
        Path staging=Files.createTempDirectory(directory.getParent(),".emotecraft-import-");
        int count=0;long total=0;
        try {
        if(source.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            try(ZipInputStream zip=new ZipInputStream(Files.newInputStream(source))) {
                ZipEntry entry;int entries=0;
                while((entry=zip.getNextEntry())!=null) {
                    if(++entries>2048)throw new IOException("Слишком много файлов в архиве");
                    String name=entry.getName().replace('\\','/');
                    if(name.startsWith("/")||name.contains(":")||Arrays.asList(name.split("/")).contains(".."))throw new IOException("Недопустимый путь в архиве");
                    if(entry.isDirectory()||!name.toLowerCase(Locale.ROOT).matches(".*\\.(json|emotecraft|emote|png|nbs)$"))continue;
                    byte[] bytes=read(zip);total+=bytes.length;
                    if(total>256L*1024*1024)throw new IOException("Архив больше 256 МБ");
                    count+=stage(staging,name,bytes);
                }
            }
        }else {
            if(!animation(source.toString()))throw new IOException("Выберите JSON, EMOTECRAFT, EMOTE или ZIP");
            try(InputStream in=Files.newInputStream(source)){count+=stage(staging,source.getFileName().toString(),read(in));}
            String base=source.getFileName().toString().replaceFirst("\\.[^.]+$","");
            for(String ext:new String[]{".png",".nbs"}) {
                Path sidecar=source.resolveSibling(base+ext);
                if(Files.isRegularFile(sidecar))try(InputStream in=Files.newInputStream(sidecar)){stage(staging,base+ext,read(in));}
            }
        }
        if(count==0)throw new IOException("В архиве нет эмоций");
        Files.createDirectories(directory);
        return Files.move(staging,directory.resolve("imported-"+UUID.randomUUID()));
        } finally {
            if(Files.exists(staging))try(java.util.stream.Stream<Path> paths=Files.walk(staging)) {
                for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(path);
            }
        }
    }
    private static int stage(Path directory,String name,byte[] bytes)throws IOException {
        Path target=directory.resolve(name).normalize();
        if(!target.startsWith(directory))throw new IOException("Недопустимый путь в архиве");
        boolean animation=animation(name);
        if(animation)try {
            if(UniversalEmoteSerializer.readData(new ByteArrayInputStream(bytes),name).isEmpty())throw new IOException("Пустая эмоция");
        } catch(Exception ex){throw new IOException("Не удалось прочитать эмоцию: "+name,ex);}
        Files.createDirectories(target.getParent());Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        return animation?1:0;
    }
}
