#version 150
uniform sampler2D NormalSampler;
uniform sampler2D ScreenSampler;
uniform sampler2D MaskSampler;
uniform sampler2D NormalDepth;
uniform sampler2D ScreenDepth;
uniform float FilterStrength;
uniform vec4 ColorSettings;
uniform vec4 SurfaceSettings;
uniform vec2 SurfaceDensity;
in vec2 texCoord;
out vec4 fragColor;
const float TAU = 6.28318530718;
void main() {
    vec4 surface = texture(MaskSampler, texCoord);
    float screen = clamp(surface.r * surface.a, 0.0, 1.0);
    vec4 exterior = texture(ScreenSampler, texCoord);
    vec2 uv = surface.gb;
    // Compute derivatives outside divergent branches. Fade unresolved patterns to avoid moire.
    vec2 footprint = fwidth(uv);
    vec2 pixels = uv * SurfaceDensity.x;
    vec2 resolved = 1.0 - smoothstep(vec2(0.20), vec2(0.65), footprint * SurfaceDensity.x);
    float scanResolved = 1.0 - smoothstep(0.20, 0.65, footprint.y * SurfaceDensity.y);
    if (screen > 0.0 && FilterStrength > 0.0) {
        vec3 color = max((exterior.rgb - 0.5) * ColorSettings.y + 0.5, vec3(0.0));
        color = color * ColorSettings.x + ColorSettings.z * (1.0 - clamp(color, 0.0, 1.0));
        color *= mix(vec3(1.0), vec3(0.94, 1.015, 1.035), ColorSettings.w);
        vec2 grid = (0.5 + 0.5 * cos(TAU * pixels)) * resolved;
        color *= 1.0 - SurfaceSettings.x * max(grid.x, grid.y);
        float line = (0.5 + 0.5 * cos(TAU * uv.y * SurfaceDensity.y)) * scanResolved;
        color *= 1.0 - SurfaceSettings.y * line;
        float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
        color *= 1.0 - SurfaceSettings.z * (1.0 - smoothstep(0.0, 0.055, edge));
        exterior.rgb = mix(exterior.rgb, max(color, vec3(0.0)), FilterStrength);
    }
    fragColor = mix(texture(NormalSampler, texCoord), exterior, screen);
    gl_FragDepth = mix(texture(NormalDepth, texCoord).r, texture(ScreenDepth, texCoord).r, screen);
}
