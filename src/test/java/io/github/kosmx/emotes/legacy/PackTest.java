package io.github.kosmx.emotes.legacy;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.legacy.client.EmoteImporter;
import io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
public class PackTest {
    @TempDir Path temp;
    @Test public void auditEveryAnimation()throws Exception {
        String source=System.getProperty("emotecraft.testPack");Assumptions.assumeTrue(source!=null);
        EmoteInstance.config=new SerializableConfig();int count=0,oversized=0,max=0;Set<UUID> ids=new HashSet<>();
        try(ZipFile zip=new ZipFile(source)) {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry e=entries.nextElement();if(!e.getName().endsWith(".json"))continue;
                List<KeyframeAnimation> parsed=UniversalEmoteSerializer.readData(zip.getInputStream(e),e.getName());
                for(KeyframeAnimation animation:parsed) {
                    count++;ids.add(animation.getUuid());
                    ByteBuffer packet=new EmotePacket.Builder().configureToStreamEmote(animation,UUID.randomUUID()).build(2*1024*1024).write();
                    max=Math.max(max,packet.remaining());if(packet.remaining()>32000)oversized++;
                    assertTrue(packet.remaining()<=LegacyNetwork.LIMIT,e.getName()+" cannot be relayed by the server");
                    LegacyNetwork.encode(new EmotePacket.Builder().configureToStreamEmote(animation,UUID.randomUUID()));
                    assertEquals(animation.getUuid(),new EmotePacket.Builder().build().read(packet).emoteData.getUuid(),e.getName());
                    KeyframeAnimationPlayer player=new KeyframeAnimationPlayer(animation);
                    for(int tick=0;tick<animation.stopTick+2;tick++) {
                        player.setupAnim(.5f);
                        for(String part:animation.getBodyParts().keySet())for(TransformType type:TransformType.values()) {
                            Vec3f p=player.get3DTransform(part,type,.5f,Vec3f.ZERO);
                            assertTrue(Float.isFinite(p.getX())&&Float.isFinite(p.getY())&&Float.isFinite(p.getZ()),e.getName()+" "+part);
                        }
                        player.tick();
                    }
                }
            }
        }
        String report="Parsed="+count+" unique="+ids.size()+" over32KB="+oversized+" maxPacket="+max;
        System.out.println(report);Files.createDirectories(Paths.get("build/reports"));Files.write(Paths.get("build/reports/spemotes-audit.txt"),report.getBytes("UTF-8"));
        assertEquals(223,count);
    }
    @Test public void importWholeUserPack()throws Exception {
        String source=System.getProperty("emotecraft.testPack");Assumptions.assumeTrue(source!=null);EmoteInstance.config=new SerializableConfig();
        Path imported=EmoteImporter.importFile(Paths.get(source),temp.resolve("emotes"));
        try(java.util.stream.Stream<Path> files=Files.walk(imported)){assertEquals(223,files.filter(p->p.toString().endsWith(".json")).count());}
    }
}
