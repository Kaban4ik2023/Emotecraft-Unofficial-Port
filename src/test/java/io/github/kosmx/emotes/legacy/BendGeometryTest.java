package io.github.kosmx.emotes.legacy;
import io.github.kosmx.emotes.legacy.client.BendGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class BendGeometryTest {
    @Test public void jointsStayClosedInEveryDirection() {
        for(boolean upper:new boolean[]{false,true})for(float axis:new float[]{0,.7f,-1.2f,3.14f})for(float bend:new float[]{-2.14f,-.8f,.9f,2.64f}) {
            float[] above=BendGeometry.transform(1,6-.00001f,2,6,0,12,axis,bend,upper);
            float[] below=BendGeometry.transform(1,6+.00001f,2,6,0,12,axis,bend,upper);
            assertArrayEquals(above,below,.001f,"Open joint at axis="+axis+" bend="+bend);
        }
    }
    @Test public void anchorsAndHandsFollowExpectedRotation() {
        float halfPi=(float)Math.PI/2;
        assertArrayEquals(new float[]{1,0,2},BendGeometry.transform(1,0,2,6,0,12,0,halfPi,false),.0001f);
        assertArrayEquals(new float[]{1,4,6},BendGeometry.transform(1,12,2,6,0,12,0,halfPi,false),.0001f);
        assertArrayEquals(new float[]{1,12,2},BendGeometry.transform(1,12,2,6,0,12,0,halfPi,true),.0001f);
        assertArrayEquals(new float[]{1,4,-6},BendGeometry.transform(1,0,2,6,0,12,0,halfPi,true),.0001f);
    }
}
