package dev.omnidisplay.region;
public final class TopologyCachePolicy {
 public static boolean reusable(long generation,long savedGeneration,boolean mode,boolean savedMode,long tick,long savedTick){return generation==savedGeneration&&mode==savedMode&&tick>=savedTick;}
}
