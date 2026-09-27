package io.github.kosmx.emotes.legacy;
import io.github.kosmx.emotes.legacy.client.EmoteImporter;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.executor.EmoteInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
public class ImportTest {
    @TempDir Path temp;
    private Path wave()throws Exception{
        EmoteInstance.config=new SerializableConfig();Path input=temp.resolve("wave.json");
        try(InputStream in=getClass().getResourceAsStream("/assets/emotecraft/emotes/waving.json")){Files.copy(in,input);}return input;
    }
    @Test public void importsAnimationAndCompanionWithoutOverwriting()throws Exception{
        Path input=wave();Files.write(temp.resolve("wave.png"),new byte[]{1,2});
        Path first=EmoteImporter.importFile(input,temp.resolve("emotes"));Path second=EmoteImporter.importFile(input,temp.resolve("emotes"));
        assertNotEquals(first,second);assertTrue(Files.isRegularFile(first.resolve("wave.json")));assertTrue(Files.isRegularFile(first.resolve("wave.png")));assertTrue(Files.exists(input));
    }
    @Test public void importsNestedZip()throws Exception{
        byte[] bytes=Files.readAllBytes(wave());Path zip=temp.resolve("pack.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){out.putNextEntry(new ZipEntry("dances/wave.json"));out.write(bytes);out.closeEntry();}
        Path imported=EmoteImporter.importFile(zip,temp.resolve("emotes"));assertTrue(Files.isRegularFile(imported.resolve("dances/wave.json")));
    }
    @Test public void rejectsTraversalAndInvalidAnimationsBeforeWriting()throws Exception{
        EmoteInstance.config=new SerializableConfig();Path zip=temp.resolve("bad.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){out.putNextEntry(new ZipEntry("../escape.json"));out.write("{}".getBytes("UTF-8"));out.closeEntry();}
        assertThrows(IOException.class,()->EmoteImporter.importFile(zip,temp.resolve("emotes")));assertFalse(Files.exists(temp.resolve("escape.json")));
        Path invalid=temp.resolve("invalid.json");Files.write(invalid,"{}".getBytes("UTF-8"));
        assertThrows(IOException.class,()->EmoteImporter.importFile(invalid,temp.resolve("emotes")));assertFalse(Files.exists(temp.resolve("emotes")));
    }
    @Test public void discardsStagedFilesWhenLaterEntryIsBroken()throws Exception{
        byte[] valid=Files.readAllBytes(wave());Path zip=temp.resolve("partial.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){
            out.putNextEntry(new ZipEntry("valid.json"));out.write(valid);out.closeEntry();
            out.putNextEntry(new ZipEntry("broken.json"));out.write("{}".getBytes("UTF-8"));out.closeEntry();
        }
        assertThrows(IOException.class,()->EmoteImporter.importFile(zip,temp.resolve("emotes")));
        assertFalse(Files.exists(temp.resolve("emotes")));
        try(java.util.stream.Stream<Path> paths=Files.list(temp)){assertFalse(paths.anyMatch(p->p.getFileName().toString().startsWith(".emotecraft-import-")));}
    }
}
