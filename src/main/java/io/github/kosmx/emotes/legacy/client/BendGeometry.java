package io.github.kosmx.emotes.legacy.client;

/** Continuous cuboid joint: both halves meet on the bend's bisector plane. */
public final class BendGeometry {
    private BendGeometry() { }
    public static float[] transform(float x,float y,float z,float joint,float top,float bottom,float axis,float bend,boolean upper) {
        double ax=Math.cos(-axis),az=Math.sin(-axis),q=-az*x+ax*z;
        boolean rotating=upper ? y<joint : y>joint;
        double distance=y-joint;
        double taper=y<=joint ? (y-top)/(joint-top) : (bottom-y)/(bottom-joint);
        taper=Math.max(0,Math.min(1,taper));
        double tangent=Math.max(-100,Math.min(100,Math.tan(bend/2d)));
        distance+=q*tangent*taper*(rotating?1:-1);
        if(!rotating)return new float[]{x,(float)(distance+joint),z};
        double c=Math.cos(bend),s=Math.sin(bend),dot=ax*x+az*z;
        return new float[]{(float)(x*c-az*distance*s+ax*dot*(1-c)),
                (float)(distance*c-q*s+joint),(float)(z*c+ax*distance*s+az*dot*(1-c))};
    }
}
