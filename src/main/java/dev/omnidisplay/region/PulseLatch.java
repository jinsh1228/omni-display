package dev.omnidisplay.region;
/** Ignore registration-time strength; count only subsequent low-to-high edges. */
public final class PulseLatch {
 private boolean armed,high;private int pending;
 public void sample(int strength){boolean next=strength>0;if(armed&&!high&&next)pending++;high=next;}
 public void arm(){armed=true;}
 public void stop(){armed=false;pending=0;}
 public int drain(){int n=pending;pending=0;return n;}
}
