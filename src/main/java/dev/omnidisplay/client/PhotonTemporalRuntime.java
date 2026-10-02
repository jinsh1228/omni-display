package dev.omnidisplay.client;
import dev.omnidisplay.region.PhotonTemporalPatch;
import org.lwjgl.opengl.GL20;
import java.util.*;
public final class PhotonTemporalRuntime {
 private static final Map<Integer,Integer> locations=new HashMap<>();
 public static void bind(int program){int location=locations.computeIfAbsent(program,id->GL20.glGetUniformLocation(id,PhotonTemporalPatch.UNIFORM));if(location>=0)GL20.glUniform1i(location,IrisPhotonCompat.viewPair()&&IrisPhotonCompat.screenView()?1:0);}
 public static void destroyed(int program){locations.remove(program);}
}
