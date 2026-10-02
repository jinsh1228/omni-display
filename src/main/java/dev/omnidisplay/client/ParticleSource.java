package dev.omnidisplay.client;
import java.util.UUID;
import net.minecraft.core.BlockPos;
public interface ParticleSource {UUID omni$source();BlockPos omni$anchor();void omni$source(UUID id,BlockPos anchor);}
