package dev.omnidisplay.mixin;

import dev.omnidisplay.client.PartitionRenderer;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Wrap the caller, never re-enter a LevelRenderer with an active Iris pipeline. */
@Mixin(value=GameRenderer.class,remap=false)
public abstract class PartitionLevelMixin {
 @Redirect(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
 private void omni$normalScene(LevelRenderer renderer,DeltaTracker delta,boolean outline,Camera camera,GameRenderer game,LightTexture light,Matrix4f view,Matrix4f projection){
  PartitionRenderer.renderScenes(renderer,delta,outline,camera,game,light,view,projection);
 }
}
