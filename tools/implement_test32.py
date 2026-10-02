from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]
j=p/'src/main/java/dev/omnidisplay'
def edit(path,a,b):
 s=path.read_text(encoding='utf-8');assert a in s,(path,a);path.write_text(s.replace(a,b),encoding='utf-8')
edit(j/'OmniDisplay.java','public static final class DisplayBlock','public static class DisplayBlock')
edit(j/'OmniDisplay.java',' public static final DeferredHolder<Item,HighlightTool>', ''' public static final DeferredHolder<Block,ThinDisplayBlock> THIN_DISPLAY=BLOCKS.register("thin_display",ThinDisplayBlock::new);
 public static final DeferredHolder<Item,BlockItem> THIN_ITEM=ITEMS.register("thin_display",()->new BlockItem(THIN_DISPLAY.get(),new Item.Properties()));
 public static final DeferredHolder<Item,ScreenPanelItem> PANEL_ITEM=ITEMS.register("screen_panel",()->new ScreenPanelItem(new Item.Properties()));
 public static final DeferredHolder<Item,HighlightTool>''')
edit(j/'OmniDisplay.java','DisplayEntity::new,DISPLAY.get()','DisplayEntity::new,DISPLAY.get(),THIN_DISPLAY.get()')
edit(j/'OmniDisplay.java','return s.is(DISPLAY.get())||s.is(DOOR.get());','return s.is(DISPLAY.get())||s.is(THIN_DISPLAY.get())||s.is(DOOR.get());')
edit(j/'OmniDisplay.java','e.accept(ITEM.get());','e.accept(ITEM.get());e.accept(THIN_ITEM.get());e.accept(PANEL_ITEM.get());')
edit(j/'ScreenFaces.java','OmniDisplay.isScreen(level.getBlockState(p))','isScreen(level,p)')
edit(j/'ScreenFaces.java','OmniDisplay.isScreen(level.getBlockState(n))','isScreen(level,n)')
edit(j/'ScreenFaces.java',' public static Enclosure.Result scan', ''' public static boolean isScreen(Level level,BlockPos pos){return OmniDisplay.isScreen(level.getBlockState(pos))||PanelStore.mask(level,pos)!=0;}
 public static boolean supportsFace(Level level,BlockPos pos,Direction face){
  var state=level.getBlockState(pos);
  if(state.is(OmniDisplay.THIN_DISPLAY.get()))return face.getAxis()==state.getValue(ThinDisplayBlock.ATTACHED_FACE).getAxis();
  if(state.is(OmniDisplay.DOOR.get()))return face.getAxis()==state.getValue(DoorBlock.FACING).getAxis();
  return state.is(OmniDisplay.DISPLAY.get())||(PanelStore.mask(level,pos)&(1<<face.get3DDataValue()))!=0;
 }
 public static Enclosure.Result scan''')
edit(j/'ScreenFaces.java','if(state.is(OmniDisplay.DISPLAY.get()))','if(state.is(OmniDisplay.DISPLAY.get())||state.is(OmniDisplay.THIN_DISPLAY.get()))')
edit(j/'ScreenFaces.java','if(enclosure.interior().contains(cell(pos.relative(dir))))','if(supportsFace(level,pos,dir)&&enclosure.interior().contains(cell(pos.relative(dir))))')
edit(j/'ScreenFaces.java','    if(next!=state)', '''    if(PanelStore.mask(level,pos)!=0){int mask=0;for(var dir:Direction.values())if(supportsFace(level,pos,dir)&&enclosure.interior().contains(cell(pos.relative(dir))))mask|=1<<dir.get3DDataValue();PanelStore.paint(level,pos,mask);}
    if(next!=state)''')
edit(j/'CabinManager.java','  state.hosts.removeIf','  PanelStore.refresh(level);\n  state.hosts.removeIf')
edit(j/'CabinManager.java','if(topology)ScreenFaces.paint(level,shape);','ScreenFaces.paint(level,shape);')
edit(j/'CabinManager.java','if(!state.is(OmniDisplay.DISPLAY.get())&&!(state.is(OmniDisplay.DOOR.get())&&sp.isShiftKeyDown()))return;', 'if(!state.is(OmniDisplay.DISPLAY.get())&&!state.is(OmniDisplay.THIN_DISPLAY.get())&&!(state.is(OmniDisplay.DOOR.get())&&sp.isShiftKeyDown())&&!(PanelStore.mask(e.getLevel(),e.getPos())!=0&&sp.isShiftKeyDown()))return;')
edit(j/'CabinManager.java','if(e.getLevel().isClientSide)return;var state=worlds.get(e.getLevel());','if(e.getLevel().isClientSide)return;PanelStore.refresh(e.getLevel());var state=worlds.get(e.getLevel());')
edit(j/'client/DisplayRenderer.java','   for(var holder:client.getPlot().getLoadedChunks())for(var be:holder.getChunk().getBlockEntities().values())','   var allHosts=new ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();\n   for(var holder:client.getPlot().getLoadedChunks()){allHosts.addAll(holder.getChunk().getBlockEntities().values());allHosts.addAll(dev.omnidisplay.PanelStore.hosts(mc.level,holder.getChunk().getPos()));}\n   for(var be:allHosts)')
edit(j/'client/DisplayRenderer.java','if(OmniDisplay.isScreen(state))continue;', 'if(OmniDisplay.isScreen(state)||dev.omnidisplay.PanelStore.interiorMask(world,p)!=0)continue;')
edit(j/'client/Attachments.java','return hiddenBlocks.contains(be);','return hiddenBlocks.contains(be)||(DisplayRenderer.active()&&at(be.getBlockPos())==DisplayRenderer.owner()&&DisplayRenderer.keeps(be.getBlockPos())&&dev.omnidisplay.PanelStore.interiorMask(be.getLevel(),be.getBlockPos())!=0);')
edit(j/'client/ScreenAnimation.java','    var n=pos.relative(dir);','    if(!ScreenFaces.supportsFace(mc.level,pos,dir))continue;\n    var n=pos.relative(dir);')
edit(j/'client/ScreenAnimation.java','  surface+=modelFace', '  var state=mc.level.getBlockState(pos);if(state.is(OmniDisplay.THIN_DISPLAY.get()))surface=ThinDisplayBlock.plane(state,modelFace);\n  surface+=modelFace')
edit(j/'client/ScreenEdges.java',' private static void draw(', ' public static void draw(')
edit(j/'client/ScreenEdges.java','   if(mask!=0)draw("edges_"+mask,state,pose,buffers,be.getBlockPos(),partial);','''   if(state.is(OmniDisplay.THIN_DISPLAY.get())){
    for(var face:net.minecraft.core.Direction.values())if((mask&(1<<face.get3DDataValue()))!=0){
     double offset=ThinDisplayBlock.plane(state,face)-(face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?1:0);
     pose.pushPose();pose.translate(face.getAxis()==net.minecraft.core.Direction.Axis.X?offset:0,face.getAxis()==net.minecraft.core.Direction.Axis.Y?offset:0,face.getAxis()==net.minecraft.core.Direction.Axis.Z?offset:0);
     draw("edges_"+(1<<face.get3DDataValue()),state,pose,buffers,be.getBlockPos(),partial);pose.popPose();
    }
   }else if(mask!=0)draw("edges_"+mask,state,pose,buffers,be.getBlockPos(),partial);''')
edit(j/'CabinPackets.java','e.registrar("15")','e.registrar("16")')
edit(j/'CabinPackets.java','  r.playToServer(Aim.TYPE','  PanelStore.register(r);\n  r.playToServer(Aim.TYPE')
for path in [p/'build.gradle',p/'src/main/resources/META-INF/neoforge.mods.toml']:
 edit(path,'0.1.0-test31','0.1.0-test32')
# Generate native models; broad screen faces stay at the actual collision planes.
r=p/'src/main/resources'
def put(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
faces=['down','up','north','south','west','east'];axes=[1,1,2,2,0,0]
variants={}
for attached in range(6):
 for mask in range(64):
  lo=[0,0,0];hi=[16,16,16];axis=axes[attached]
  if attached%2:lo[axis]=15
  else:hi[axis]=1
  name=f'thin_{attached}_{mask}'
  put(r/f'assets/omni_display/models/block/{name}.json',{'textures':{'particle':'omni_display:block/screen_back','back':'omni_display:block/screen_back','screen':'omni_display:block/screen_hex','edge':'omni_display:block/door_edge'},'elements':[{'from':lo,'to':hi,'faces':{f:{'uv':[0,0,16,16],'texture':'#'+('screen' if mask&(1<<i) else 'back') if axes[i]==axis else '#edge'} for i,f in enumerate(faces)}}]})
  variants[f'attached_face={faces[attached]},screen_faces={mask}']={'model':f'omni_display:block/{name}'}
put(r/'assets/omni_display/blockstates/thin_display.json',{'variants':variants})
put(r/'assets/omni_display/models/item/thin_display.json',{'parent':'omni_display:block/thin_2_8'})
put(r/'assets/omni_display/models/item/screen_panel.json',{'parent':'omni_display:block/thin_2_8'})
for face in range(6):
 for inside in [False,True]:
  lo=[0,0,0];hi=[16,16,16];axis=axes[face];lo[axis]=hi[axis]=16.015 if face%2 else -.015
  put(r/f'assets/omni_display/models/block/panel_{face}_{int(inside)}.json',{'textures':{'panel':'omni_display:block/'+('screen_hex' if inside else 'screen_back')},'elements':[{'from':lo,'to':hi,'faces':{faces[face]:{'uv':[0,0,16,16],'texture':'#panel'}}}]})
for lang,thin,panel in [('zh_cn','轻薄全周天显示器','全周天屏幕面板'),('en_us','Thin Omnidirectional Display','Omnidirectional Screen Panel')]:
 path=r/f'assets/omni_display/lang/{lang}.json';obj=json.loads(path.read_text(encoding='utf-8'));obj.update({'block.omni_display.thin_display':thin,'item.omni_display.screen_panel':panel});put(path,obj)
put(r/'data/omni_display/loot_table/blocks/thin_display.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'omni_display:thin_display'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
edit(j/'client/DoorRendering.java','  for(int i=1;i<64;i++)','  for(int i=0;i<6;i++)for(int side=0;side<2;side++)e.register(ScreenEdges.model("panel_"+i+"_"+side));\n  for(int i=1;i<64;i++)')
print('Integrated thin displays and panel hooks; generated models.')
