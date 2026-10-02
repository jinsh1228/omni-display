package dev.omnidisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** The server records the eye position in the selected vehicle's coordinate system. */
public final class HudLocator extends Item {
 public HudLocator(Properties properties){super(properties);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  if(!level.isClientSide&&player instanceof ServerPlayer server)CabinManager.locateHud(server,null);
  return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
 }
 @Override public InteractionResult useOn(UseOnContext context){
  if(!context.getLevel().isClientSide&&context.getPlayer() instanceof ServerPlayer server)CabinManager.locateHud(server,context.getClickedPos());
  return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
 }
}
