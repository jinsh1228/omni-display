package dev.omnidisplay;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import dev.omnidisplay.region.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Create bearing/pulley structures use an entity-local block map, not another Sable plot. */
public final class MovingScreens {
 public static final String PANEL="omniDisplayPanel";
 public record Screen(BlockPos pos,BlockState state,CabinSettings settings,int mask){}
 public static List<Screen> screens(AbstractContraptionEntity e){
  if(e.isRemoved()||e.getContraption()==null)return List.of();var result=new ArrayList<Screen>();
  for(var info:e.getContraption().getBlocks().values()){
   var t=info.nbt();int mask=t==null?0:t.getCompound(PANEL).getInt("mask");
   if(!OmniDisplay.isScreen(info.state())&&mask==0)continue;
   var data=t==null?new CompoundTag():mask!=0?t.getCompound(PANEL):t;
   var settings=CabinSettings.load(data.getCompound("cabin"),e.level().registryAccess());
   if(data.contains("originOffset"))settings.origin=info.pos().offset(BlockPos.of(data.getLong("originOffset")));
   result.add(new Screen(info.pos(),info.state(),settings,mask));
  }
  return result;
 }
 public static int faces(Screen s){
  if(s.mask!=0)return s.mask;
  int mask=s.settings.manualFaces;
  if(mask==0&&s.state.hasProperty(OmniDisplay.DisplayBlock.FACES))mask=s.state.getValue(OmniDisplay.DisplayBlock.FACES);
  if(mask==0&&s.state.is(OmniDisplay.DOOR.get())){var d=s.state.getValue(CockpitDoor.FACING);int sides=s.state.getValue(CockpitDoor.SIDES);if((sides&1)!=0)mask|=1<<d.get3DDataValue();if((sides&2)!=0)mask|=1<<d.getOpposite().get3DDataValue();}
  return mask;
 }
 public static Enclosure.Result shape(List<Screen> screens){
  if(screens.isEmpty())throw new IllegalArgumentException("No moving screens");
  Set<Enclosure.Cell> cells=new HashSet<>();for(var s:screens)cells.add(CabinManager.cell(s.pos));
  try{var closed=Enclosure.detect(cells,CabinManager.cell(screens.get(0).pos));if(!closed.walls().containsAll(cells))throw new IllegalArgumentException("Disconnected moving screens");return closed;}catch(IllegalArgumentException open){
   var faces=new ArrayList<OpenCabin.Face>();for(var s:screens)for(var d:Direction.values())if((faces(s)&(1<<d.get3DDataValue()))!=0)faces.add(new OpenCabin.Face(CabinManager.cell(s.pos),d.getStepX(),d.getStepY(),d.getStepZ()));
   return OpenCabin.detect(faces);
  }
 }
 public static void prepare(AbstractContraptionEntity e,Direction face){
  if(e.level().isClientSide)return;
  for(var screen:screens(e))if(faces(screen)==0){var settings=screen.settings.copy();var chosen=face;if(screen.state.is(OmniDisplay.THIN_DISPLAY.get())&&chosen.getAxis()!=screen.state.getValue(ThinDisplayBlock.ATTACHED_FACE).getAxis())chosen=screen.state.getValue(ThinDisplayBlock.ATTACHED_FACE).getOpposite();settings.manualFaces=1<<chosen.get3DDataValue();write(e,screen,settings);}
 }
 public static CompoundTag metadata(CompoundTag source){var result=new CompoundTag();for(String key:new String[]{"cabin","originOffset",PANEL})if(source.contains(key))result.put(key,source.get(key).copy());return result;}
 public static void updateTag(Contraption contraption,BlockPos pos,CompoundTag data){var tags=((dev.omnidisplay.mixin.ContraptionScreenAccessor)contraption).omni$updateTags();var tag=tags.get(pos);tag=tag==null?metadata(data):tag.copy();tag.merge(metadata(data));tags.put(pos,tag);}
 public static void write(AbstractContraptionEntity e,Screen screen,CabinSettings settings){
  var info=e.getContraption().getBlocks().get(screen.pos);if(info==null)return;
  var t=info.nbt()==null?new CompoundTag():info.nbt().copy();var data=screen.mask!=0?t.getCompound(PANEL):t;
  data.put("cabin",settings.save(e.level().registryAccess()));
  if(settings.origin!=null)data.putLong("originOffset",settings.origin.subtract(screen.pos).asLong());
  if(screen.mask!=0)t.put(PANEL,data);
  var state=info.state();int mask=faces(new Screen(screen.pos,state,settings,screen.mask));
  if(state.hasProperty(OmniDisplay.DisplayBlock.FACES))state=state.setValue(OmniDisplay.DisplayBlock.FACES,mask);
  e.getContraption().getBlocks().put(screen.pos,new StructureBlockInfo(screen.pos,state,t));
  updateTag(e.getContraption(),screen.pos,t);
  if(!e.level().isClientSide)PacketDistributor.sendToPlayersTrackingEntity(e,new Sync(e.getId(),screen.pos,metadata(t),mask));
 }
 static List<CabinManager.Cabin> cabins(ServerLevel level){
  var result=new ArrayList<CabinManager.Cabin>();
  if(!RenderConfig.experimental())return result;
  for(var entity:level.getAllEntities())if(entity instanceof AbstractContraptionEntity e&&!e.isRemoved()){
   var controller=HighlightStructures.controller(e);if(controller==null||HighlightStructures.at(level,controller)==null)continue;
   var screens=screens(e);if(screens.isEmpty())continue;
   try{
    var shape=shape(screens);var newest=screens.stream().max(Comparator.comparingLong(s->s.settings.revision)).orElseThrow();
    var settings=newest.settings.copy();boolean assign=!e.getUUID().equals(settings.id);settings.id=e.getUUID();settings.moveOrigin(new BlockPos(shape.min().x(),shape.min().y(),shape.min().z()));
    var hosts=new ArrayList<CabinEntity>();for(var screen:screens){var host=new Host(e,screen);host.settings=settings.copy();hosts.add(host);}
    var cabin=new CabinManager.Cabin(level,hosts.get(0),shape,settings);cabin.moving=e;cabin.members=List.copyOf(hosts);result.add(cabin);if(assign)cabin.publish();
   }catch(IllegalArgumentException invalid){}
  }return result;
 }
 private static final class Host extends CabinEntity {
  final AbstractContraptionEntity entity;final Screen screen;
  Host(AbstractContraptionEntity e,Screen screen){super(screen.pos,OmniDisplay.DISPLAY.get().defaultBlockState());entity=e;this.screen=screen;setLevel(e.level());settings=screen.settings.copy();}
  @Override public void apply(CabinSettings s){settings=s.copy();write(entity,screen,s);}
 }
 public static boolean interact(AbstractContraptionEntity e,Player player,BlockPos pos,Direction face,InteractionHand hand){
  if(hand!=InteractionHand.MAIN_HAND||!screens(e).stream().anyMatch(s->s.pos.equals(pos)))return false;
  var stack=player.getItemInHand(hand);if(!stack.is(OmniDisplay.CABIN_SELECTOR.get())&&!stack.isEmpty())return false;
  if(e.level().isClientSide)return true;
  if(!RenderConfig.experimental()){player.displayClientMessage(net.minecraft.network.chat.Component.literal("低性能模式不支持运动结构上的屏幕分组。"),true);return true;}
  if(!e.canInteractWithBlock(player,pos,8))return true;
  prepare(e,face);CabinManager.rebuild(e.level());var cabin=CabinManager.moving(e.level(),e);
  if(cabin==null)return true;
  if(stack.is(OmniDisplay.CABIN_SELECTOR.get()))CabinSelector.select(player,stack,cabin);
  else{CabinManager.power(cabin,!cabin.settings.power,null,pos);cabin.publish();player.displayClientMessage(net.minecraft.network.chat.Component.literal(cabin.settings.power?"座舱已开机":"座舱已关机"),true);}
  return true;
 }
 public record Sync(int entity,BlockPos pos,CompoundTag data,int faces) implements CustomPacketPayload {
  public static final Type<Sync> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"moving_screen"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Sync> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Sync::entity,BlockPos.STREAM_CODEC,Sync::pos,ByteBufCodecs.COMPOUND_TAG,Sync::data,ByteBufCodecs.VAR_INT,Sync::faces,Sync::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Action(int entity,int op,int value) implements CustomPacketPayload {
  public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"moving_screen_action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::entity,ByteBufCodecs.VAR_INT,Action::op,ByteBufCodecs.VAR_INT,Action::value,Action::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(PayloadRegistrar registrar){
  registrar.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{
   if(!(c.player() instanceof net.minecraft.server.level.ServerPlayer player)||!(player.serverLevel().getEntity(p.entity()) instanceof AbstractContraptionEntity e)||e.isRemoved()||!RenderConfig.experimental())return;
   CabinManager.rebuild(player.level());var cabin=CabinManager.moving(player.level(),e);if(cabin==null||!e.canInteractWithBlock(player,cabin.anchor.getBlockPos(),16))return;
   if(p.op()==2)CabinManager.power(cabin,false,player);else if(p.op()==7)cabin.settings.heading=Math.floorMod(p.value(),360);else return;cabin.publish();
  }));registrar.playToClient(Sync.TYPE,Sync.CODEC,(p,c)->c.enqueueWork(()->dev.omnidisplay.client.MovingScreenView.receive(p)));}
}
