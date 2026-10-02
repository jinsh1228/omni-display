package dev.omnidisplay.region;
/** A transient frame error never permanently disables every cockpit in the world. */
public final class RenderRecovery {
 private long retryAt=Long.MIN_VALUE,failedAt,version;private int failures;
 public void fail(long tick,long topology){failedAt=tick;version=topology;retryAt=tick+Math.min(400L,20L<<Math.min(failures++,4));}
 public boolean retry(long tick,long topology){return retryAt!=Long.MIN_VALUE&&(tick>=retryAt||tick>=failedAt+2&&topology!=version||tick<failedAt);}
 public void reset(){retryAt=Long.MIN_VALUE;failures=0;}
}
