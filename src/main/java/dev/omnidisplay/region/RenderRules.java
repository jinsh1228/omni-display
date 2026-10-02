package dev.omnidisplay.region;
public final class RenderRules {
 public static boolean locks(boolean experimental,boolean active){return !experimental&&active;}
 public static boolean mayStart(boolean experimental,boolean damaged,boolean openDoor){return !damaged&&(experimental||!openDoor);}
 public static boolean mayEdit(boolean operator,boolean singleplayerOwner){return operator||singleplayerOwner;}
}
