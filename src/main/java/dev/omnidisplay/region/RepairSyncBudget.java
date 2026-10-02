package dev.omnidisplay.region;
public final class RepairSyncBudget {
 public static int perTick(int panels){return Math.max(32,(int)(((long)Math.max(0,panels)+99)/100));}
}
