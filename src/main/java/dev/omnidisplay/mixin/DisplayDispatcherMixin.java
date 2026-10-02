package dev.omnidisplay.mixin;
import dev.omnidisplay.client.DisplayRenderer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets={"dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher","dev.ryanhcode.sable.sublevel.render.dispatcher.FancySubLevelRenderDispatcher"},remap=false)
public abstract class DisplayDispatcherMixin {
 @ModifyVariable(method={"renderSectionLayer","renderAfterSections"},at=@At("HEAD"),argsOnly=true,ordinal=0)
 private Iterable<ClientSubLevel> omni$filter(Iterable<ClientSubLevel> original){return DisplayRenderer.visible(original);}
}
