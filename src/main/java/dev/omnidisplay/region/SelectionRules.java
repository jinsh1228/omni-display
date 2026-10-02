package dev.omnidisplay.region;
import java.util.UUID;
public final class SelectionRules {
 public static boolean sameVehicle(boolean surfaceMode,UUID a,UUID b){return surfaceMode&&a!=null&&a.equals(b);}
}
