package dev.omnidisplay.region;
/** Conservative in-memory Photon fragment adjustment; ordinary view remains unchanged. */
public final class PhotonTemporalPatch {
 public static final String UNIFORM="omniDisplayScreenPass";
 public static String apply(String source){
  if(source.contains("uniform bool "+UNIFORM)||!source.contains("void main()"))return source;
  String next=source;
  if(source.contains("clouds_checkerboard_offsets")&&source.contains("clouds_history = max0(mix(current, history, history_weight));")&&source.contains("velocity_factor = velocity_factor / (velocity_factor + 1.0);")){
   if(!source.contains("float ambient_scattering = current_data.y;")||!source.contains("disocclusion = disocclusion || world_age_changed;"))return source;
   next=next.replace("float ambient_scattering = current_data.y;","float ambient_scattering = current_data.y;\n    if (omniDisplayScreenPass) {\n        current = omniCloudSpatial(colortex9, uv_clamped * taau_render_scale);\n        current_data = omniCloudSpatial(colortex10, uv_clamped * taau_render_scale).xy;\n        apparent_distance = current_data.x;\n        ambient_scattering = current_data.y;\n    }");
   next=next.replace("disocclusion = disocclusion || world_age_changed;","disocclusion = disocclusion || world_age_changed;\n    disocclusion = disocclusion || omniDisplayScreenPass;");
   next=next.replace("void main()", "vec4 omniCloudSpatial(sampler2D image, vec2 sampleUv) {\n    ivec2 size = textureSize(image, 0);\n    vec2 pos = clamp(sampleUv * vec2(size) - 0.5, vec2(0.0), vec2(size - 1));\n    ivec2 base = ivec2(floor(pos));\n    ivec2 limit = size - 1;\n    vec2 fraction = fract(pos);\n    vec4 a = texelFetch(image, base, 0);\n    vec4 b = texelFetch(image, min(base + ivec2(1, 0), limit), 0);\n    vec4 c = texelFetch(image, min(base + ivec2(0, 1), limit), 0);\n    vec4 d = texelFetch(image, min(base + ivec2(1, 1), limit), 0);\n    return mix(mix(a, b, fraction.x), mix(c, d, fraction.x), fraction.y);\n}\nvoid main()");
  }else if(source.contains("ambient_history_data = vec2(1.0 - depth, pixel_age + 1.0);")&&source.contains("float history_weight = pixel_age / (pixel_age + 1.0);")){
   next=next.replace("float history_weight = pixel_age / (pixel_age + 1.0);","float history_weight = pixel_age / (pixel_age + 1.0);\n        if (omniDisplayScreenPass) history_weight = min(history_weight, 0.2);");
  }
  return next.equals(source)?source:next.replace("void main()","uniform bool "+UNIFORM+";\nvoid main()");
 }
}
