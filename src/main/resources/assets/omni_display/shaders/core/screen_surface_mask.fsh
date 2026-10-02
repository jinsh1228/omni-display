#version 150
in vec2 texCoord;
in float opacity;
out vec4 fragColor;
void main() {
    // Red remains the binary visibility mask for HUD, radar and depth composition.
    if(opacity<=0.001)discard;
    fragColor = vec4(1.0, texCoord.x, texCoord.y, opacity);
}
