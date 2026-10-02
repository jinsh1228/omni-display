package dev.omnidisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class CabinPackets {
 public record Action(BlockPos pos,int op,int value) implements CustomPacketPayload{
  public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"cabin_action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Action::pos,ByteBufCodecs.VAR_INT,Action::op,ByteBufCodecs.VAR_INT,Action::value,Action::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Data(BlockPos pos,boolean open,CompoundTag tag) implements CustomPacketPayload{
  public static final Type<Data> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"cabin_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Data> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Data::pos,ByteBufCodecs.BOOL,Data::open,ByteBufCodecs.COMPOUND_TAG,Data::tag,Data::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Aim(BlockPos pos,BlockPos surface,net.minecraft.world.phys.Vec3 eye,net.minecraft.world.phys.Vec3 direction) implements CustomPacketPayload {
  public static final Type<Aim> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"radar_aim"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Aim> CODEC=new StreamCodec<>(){
   public Aim decode(RegistryFriendlyByteBuf b){return new Aim(b.readBlockPos(),b.readBlockPos(),readVec(b),readVec(b));}
   public void encode(RegistryFriendlyByteBuf b,Aim p){b.writeBlockPos(p.pos);b.writeBlockPos(p.surface);writeVec(b,p.eye);writeVec(b,p.direction);}
  };
  private static net.minecraft.world.phys.Vec3 readVec(RegistryFriendlyByteBuf b){return new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
  private static void writeVec(RegistryFriendlyByteBuf b,net.minecraft.world.phys.Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("16");
  PanelStore.register(r);MovingScreens.register(r);
  r.playToServer(Aim.TYPE,Aim.CODEC,(p,c)->c.enqueueWork(()->{
   if(c.player() instanceof ServerPlayer sp&&RadarHooks.available()){
    var cabin=CabinManager.find(sp.serverLevel(),p.pos());
    if(cabin!=null)RadarBridge.select(sp,cabin,p);
   }
  }));
  r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer sp)CabinManager.action(sp,p);}));
  r.playToClient(Data.TYPE,Data.CODEC,(p,c)->c.enqueueWork(()->dev.omnidisplay.client.CabinScreen.receive(p)));
 }
 public static void fault(CabinManager.Cabin c){
  var tag=c.settings.save(c.level.registryAccess());tag.putBoolean("emergency",true);
  tag.putLongArray("walls",c.shape.walls().stream().mapToLong(v->new BlockPos(v.x(),v.y(),v.z()).asLong()).toArray());
  tag.putLongArray("inside",c.shape.interior().stream().mapToLong(v->new BlockPos(v.x(),v.y(),v.z()).asLong()).toArray());
  PacketDistributor.sendToPlayersInDimension((net.minecraft.server.level.ServerLevel)c.level,new Data(c.anchor.getBlockPos(),false,tag));
 }
 public static void send(ServerPlayer p,CabinManager.Cabin c,boolean open){var tag=c.settings.save(p.registryAccess());tag.putInt("members",c.shape.walls().size());PacketDistributor.sendToPlayer(p,new Data(c.anchor.getBlockPos(),open,tag));}
}
