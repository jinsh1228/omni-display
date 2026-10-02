package dev.omnidisplay.mixin;

import dev.omnidisplay.PanelStore;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=SubLevelAssemblyHelper.class,remap=false)
public abstract class PanelAssemblyMixin {
 @Unique private static final ThreadLocal<Deque<List<CompoundTag>>> omni$transfers=ThreadLocal.withInitial(ArrayDeque::new);
 @Inject(method="moveBlocks",at=@At("HEAD"))
 private static void omni$capture(ServerLevel level,SubLevelAssemblyHelper.AssemblyTransform transform,Iterable<BlockPos> blocks,CallbackInfo ci){omni$transfers.get().push(PanelStore.capture(level,blocks));PanelStore.beginTransfer();}
 @Inject(method="moveBlocks",at=@At("RETURN"))
 private static void omni$move(ServerLevel level,SubLevelAssemblyHelper.AssemblyTransform transform,Iterable<BlockPos> blocks,CallbackInfo ci){try{PanelStore.transfer(level,transform,omni$transfers.get().pop());}finally{PanelStore.endTransfer();if(omni$transfers.get().isEmpty())omni$transfers.remove();}}
}
