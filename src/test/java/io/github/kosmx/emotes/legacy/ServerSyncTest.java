package io.github.kosmx.emotes.legacy;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.*;
import dev.kosmx.playerAnim.core.util.*;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.objects.NetData;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ServerSyncTest {
    private KeyframeAnimation loop() {
        KeyframeAnimation.AnimationBuilder b=new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_EMOTECRAFT);
        b.endTick=40;b.stopTick=45;b.returnTick=20;b.isLooped=true;
        b.rightArm.pitch.addKeyFrame(0,0,Ease.LINEAR);b.rightArm.pitch.addKeyFrame(10,-1,Ease.LINEAR);
        b.rightArm.pitch.addKeyFrame(30,1,Ease.LINEAR);b.rightArm.pitch.addKeyFrame(40,0,Ease.LINEAR);
        return b.build();
    }
    @Test public void lateJoinMatchesContinuouslyPlayingLoop() {
        KeyframeAnimation data=loop();KeyframeAnimationPlayer original=new KeyframeAnimationPlayer(data);
        for(int tick=0;tick<200;tick++) {
            KeyframeAnimationPlayer joined=new KeyframeAnimationPlayer(data,tick);
            original.setupAnim(.5f);joined.setupAnim(.5f);
            float a=original.get3DTransform("rightArm",TransformType.ROTATION,0,Vec3f.ZERO).getX();
            float b=joined.get3DTransform("rightArm",TransformType.ROTATION,0,Vec3f.ZERO).getX();
            assertEquals(a,b,.00001f,"Different pose after joining at tick "+tick);
            original.tick();
        }
    }
    @Test public void relayHasRoomForAuthenticatedSenderAtClientPayloadLimit() throws Exception {
        KeyframeAnimation.AnimationBuilder b=new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_EMOTECRAFT);
        b.endTick=5000;
        int base=LegacyNetwork.encode(new EmotePacket.Builder().configureToStreamEmote(b.build())).length;
        for(int i=0;i<(LegacyNetwork.LIMIT-base)/9;i++)b.rightArm.pitch.addKeyFrame(i,(float)Math.sin(i),Ease.LINEAR);
        KeyframeAnimation animation=b.build();
        byte[] outgoing=LegacyNetwork.encode(new EmotePacket.Builder().configureToStreamEmote(animation));
        assertTrue(outgoing.length<=LegacyNetwork.LIMIT);
        assertTrue(outgoing.length+23>LegacyNetwork.LIMIT);
        NetData received=new EmotePacket.Builder().build().read(ByteBuffer.wrap(outgoing));
        UUID sender=UUID.randomUUID();received.player=sender;received.isForced=false;
        byte[] forwarded=LegacyNetwork.encodeForClient(new EmotePacket.Builder(received));
        assertTrue(forwarded.length>LegacyNetwork.LIMIT);
        assertTrue(forwarded.length<=LegacyNetwork.CLIENTBOUND_LIMIT);
        NetData decoded=new EmotePacket.Builder().build().read(ByteBuffer.wrap(forwarded));
        assertEquals(sender,decoded.player);assertEquals(animation.getUuid(),decoded.emoteData.getUuid());
        assertFalse(decoded.isForced);
    }
}
