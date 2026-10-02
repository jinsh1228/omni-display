package dev.omnidisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;

@Mod(OmniDisplay.ID)
public final class OmniDisplay {
 public static final String ID="omni_display";
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ID);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ID);
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
 public static final DeferredHolder<Block,DisplayBlock> DISPLAY=BLOCKS.register("display",()->new DisplayBlock(BlockBehaviour.Properties.of().strength(2)));
 public static final DeferredHolder<Block,ThinDisplayBlock> THIN_DISPLAY=BLOCKS.register("thin_display",ThinDisplayBlock::new);
 public static final DeferredHolder<Block,CockpitTrapdoor> TRAPDOOR=BLOCKS.register("cockpit_trapdoor",()->new CockpitTrapdoor());
 public static final DeferredHolder<Item,BlockItem> TRAPDOOR_ITEM=ITEMS.register("cockpit_trapdoor",()->new BlockItem(TRAPDOOR.get(),new Item.Properties()));
 public static final DeferredHolder<Item,BlockItem> THIN_ITEM=ITEMS.register("thin_display",()->new BlockItem(THIN_DISPLAY.get(),new Item.Properties()));
 public static final DeferredHolder<Item,ScreenPanelItem> PANEL_ITEM=ITEMS.register("screen_panel",()->new ScreenPanelItem(new Item.Properties()));
 public static final DeferredHolder<Item,HudLocator> HUD_LOCATOR=ITEMS.register("hud_locator",()->new HudLocator(new Item.Properties().stacksTo(1)));
 public static final DeferredHolder<Item,CabinSelector> CABIN_SELECTOR=ITEMS.register("cabin_selector",()->new CabinSelector(new Item.Properties().stacksTo(1)));
 public static final DeferredHolder<Item,HighlightTool> HIGHLIGHT_TOOL=ITEMS.register("highlight_tool",()->new HighlightTool(new Item.Properties().stacksTo(1)));
 public static final DeferredHolder<Item,BlockItem> ITEM=ITEMS.register("display",()->new BlockItem(DISPLAY.get(),new Item.Properties()));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DisplayEntity>> ENTITY=ENTITIES.register("display",()->BlockEntityType.Builder.of(DisplayEntity::new,DISPLAY.get(),THIN_DISPLAY.get(),TRAPDOOR.get()).build(null));
 public static final DeferredHolder<Block,CockpitDoor> DOOR=BLOCKS.register("cockpit_door",()->new CockpitDoor());
 public static final DeferredHolder<Item,DoubleHighBlockItem> DOOR_ITEM=ITEMS.register("cockpit_door",()->new DoubleHighBlockItem(DOOR.get(),new Item.Properties()));
 public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder()
  .title(net.minecraft.network.chat.Component.translatable("itemGroup.omni_display"))
  .icon(()->new ItemStack(ITEM.get()))
  .displayItems((parameters,output)->{output.accept(ITEM.get());output.accept(PANEL_ITEM.get());output.accept(THIN_ITEM.get());output.accept(DOOR_ITEM.get());output.accept(TRAPDOOR_ITEM.get());output.accept(HIGHLIGHT_TOOL.get());output.accept(HUD_LOCATOR.get());output.accept(CABIN_SELECTOR.get());})
  .build());
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity>> DOOR_ENTITY=ENTITIES.register("cockpit_door",()->BlockEntityType.Builder.of((pos,state)->newDoorEntity(pos,state),DOOR.get()).build(null));
 private static com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity newDoorEntity(BlockPos p,BlockState s){return new com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity(DOOR_ENTITY.get(),p,s);}
 public static boolean isScreen(BlockState s){return s.is(DISPLAY.get())||s.is(THIN_DISPLAY.get())||s.is(DOOR.get())||s.is(TRAPDOOR.get());}
 public OmniDisplay(IEventBus bus,net.neoforged.fml.ModContainer container){
  container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,RenderConfig.SPEC);
  bus.addListener(RenderPackets::register);
  bus.addListener(CabinPackets::register);
  BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);
 }
 public static class DisplayBlock extends Block implements EntityBlock {
  public static final net.minecraft.world.level.block.state.properties.IntegerProperty FACES=net.minecraft.world.level.block.state.properties.IntegerProperty.create("screen_faces",0,63);
  public DisplayBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACES,0));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACES);}

  @Override protected void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moving){if(!level.isClientSide&&!next.is(this))CabinManager.broken(level,pos);super.onRemove(state,level,pos,next,moving);}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DisplayEntity(p,s);}
 }
 public static final class DisplayEntity extends CabinEntity {
  public DisplayEntity(BlockPos p,BlockState s){super(p,s);}
 }
}
