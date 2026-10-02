import dev.omnidisplay.region.PhotonTemporalPatch;
public class PhotonTemporalPatchTest {
 public static void main(String[] args){
  String clouds="#version 330 core\nvoid main(){\n// clouds_checkerboard_offsets\nvelocity_factor = velocity_factor / (velocity_factor + 1.0);\nfloat ambient_scattering = current_data.y;\ndisocclusion = disocclusion || world_age_changed;\nclouds_history = max0(mix(current, history, history_weight));\n}";
  String ao="#version 330 core\nvoid main(){float history_weight = pixel_age / (pixel_age + 1.0); ambient_history_data = vec2(1.0 - depth, pixel_age + 1.0);}";
  String c=PhotonTemporalPatch.apply(clouds),a=PhotonTemporalPatch.apply(ao);
  if(!c.startsWith("#version")||!c.contains("disocclusion = disocclusion || omniDisplayScreenPass;")||!c.contains("omniCloudSpatial(colortex9")||!c.contains("min(base + ivec2(1, 1), limit)"))throw new AssertionError("cloud patch");
  if(!a.contains("min(history_weight, 0.2)"))throw new AssertionError("AO patch");
  if(!PhotonTemporalPatch.apply(c).equals(c)||!PhotonTemporalPatch.apply(a).equals(a))throw new AssertionError("idempotence");
  String other="#version 330 core\nvoid main(){ }";if(!PhotonTemporalPatch.apply(other).equals(other))throw new AssertionError("unrelated shader");
  System.out.println("PASS: selective Photon clouds/AO adjustment, conditional view gate, original GLSL version and idempotence");
 }
}
