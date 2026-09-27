package io.github.kosmx.emotes.legacy;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.util.Vec3f;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.objects.NetData;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class BackportTest {
    @Test public void rejectsInvalidSubpacketLengths() throws Exception {
        ByteBuffer oversized=ByteBuffer.allocate(12);
        oversized.putInt(8).put((byte)8).put((byte)1).put((byte)8).put((byte)1).putInt(Integer.MAX_VALUE);
        oversized.flip();
        assertThrows(java.io.IOException.class,()->new EmotePacket.Builder().build().read(oversized));
        ByteBuffer negative=ByteBuffer.allocate(12);
        negative.putInt(8).put((byte)8).put((byte)1).put((byte)8).put((byte)1).putInt(-1);
        negative.flip();
        assertThrows(java.io.IOException.class,()->new EmotePacket.Builder().build().read(negative));
    }
    @Test public void bundledEmotesLoadAnimateAndSurviveNetworkRoundTrip() throws Exception {
        for(String name:new String[]{"waving","clap","crying","point","here","palm","backflip","roblox_potion_dance","kazotsky_kick"}) {
            KeyframeAnimation animation;
            try(InputStream in=getClass().getResourceAsStream("/assets/emotecraft/emotes/"+name+".json")) {
                assertNotNull(in);animation=AnimationSerializing.deserializeAnimation(in).get(0);
            }
            UUID owner=UUID.randomUUID();
            ByteBuffer bytes=new EmotePacket.Builder().configureToStreamEmote(animation,owner).build(32000).write();
            NetData decoded=new EmotePacket.Builder().build().read(bytes);
            assertNotNull(decoded);assertEquals(owner,decoded.player);assertEquals(animation.getUuid(),decoded.emoteData.getUuid());
            KeyframeAnimationPlayer player=new KeyframeAnimationPlayer(decoded.emoteData);
            for(int tick=0;tick<Math.min(500,animation.stopTick);tick++) {
                player.setupAnim(.5f);
                for(String part:animation.getBodyParts().keySet()) {
                    Vec3f rotation=player.get3DTransform(part,TransformType.ROTATION,.5f,Vec3f.ZERO);
                    assertTrue(Float.isFinite(rotation.getX())&&Float.isFinite(rotation.getY())&&Float.isFinite(rotation.getZ()),name);
                }
                player.tick();
            }
        }
    }
}
