package dev.omnidisplay.region;
/** Unlit HUD colour and coverage. No halo or keyline is drawn. */
public final class HudLight {
 public static int color(int rgb,int brightness){double gain=Math.clamp(brightness,0,300)/100d;int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;return channel(r,gain)<<16|channel(g,gain)<<8|channel(b,gain);}
 private static int channel(int n,double gain){return (int)Math.clamp(Math.round(n*Math.min(1,gain)+(255-n)*Math.max(0,gain-1)*.26),0,255);}
 public static int alpha(int a,int brightness){return (int)Math.clamp(Math.round(a*Math.clamp(brightness,0,300)/100d),0,255);}
}
