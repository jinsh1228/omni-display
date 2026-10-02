package dev.omnidisplay.region;
/** Conservative single-scene optimization: animations and outside views keep partition rendering. */
public final class AdaptiveRenderRules {
 public static boolean singleScene(boolean openCabin,boolean inside){return !openCabin&&inside;}
 public static boolean doorClosed(boolean open,float animation){return !open&&Float.isFinite(animation)&&Math.abs(animation)<=0.00001f;}
}
