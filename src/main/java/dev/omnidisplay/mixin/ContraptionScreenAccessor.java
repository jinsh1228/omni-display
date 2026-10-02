package dev.omnidisplay.mixin;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import com.simibubi.create.content.contraptions.Contraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Contraption.class,remap=false)
public interface ContraptionScreenAccessor {
 @Accessor("updateTags") Map<BlockPos,CompoundTag> omni$updateTags();
}
