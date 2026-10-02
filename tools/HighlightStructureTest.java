import dev.omnidisplay.HighlightRegion;
import net.minecraft.core.BlockPos;
import java.util.UUID;
public final class HighlightStructureTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var a=new BlockPos(-4,3,9);var b=new BlockPos(2,7,12);var region=HighlightRegion.between(b,a,70);
  check(region.contains(a)&&region.contains(b)&&!region.contains(a.offset(-1,0,0)));
  check(HighlightRegion.load(region.save()).equals(region));check(region.move(new BlockPos(8,0,0)).contains(a.offset(8,0,0)));
  var id=UUID.randomUUID();
  var blocks=HighlightRegion.blocks(id,b,a,40);check(blocks.matches(a)&&blocks.matches(b)&&!blocks.matches(a.offset(-1,0,0)));check(!blocks.contains(a));check(HighlightRegion.load(blocks.save()).equals(blocks));check(blocks.move(new BlockPos(99,8,3)).equals(blocks));check(blocks.withOpacity(90).matches(b));
  for(var kind:new String[]{"ship","contraption"}){
   var structure=HighlightRegion.structure(id,kind,120);check(structure.opacity()==100&&!structure.contains(BlockPos.ZERO));
   check(HighlightRegion.load(structure.save()).equals(structure));check(structure.move(a).equals(structure));
   check(structure.withOpacity(25).body().equals(id)&&structure.withOpacity(25).kind().equals(kind));
   check(HighlightRegion.load(structure.withOpacity(0).save()).opacity()==0);
  }
  var legacy=region.save();legacy.putUUID("body",id);legacy.putString("kind","unknown");check(HighlightRegion.load(legacy).body()==null);
  var entity=RuntimeHooksTest.read("com/simibubi/create/content/contraptions/AbstractContraptionEntity");
  check(entity.methods.stream().anyMatch(m->m.name.equals("toGlobalVector")&&m.desc.equals("(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;")));
  var contraption=RuntimeHooksTest.read("com/simibubi/create/content/contraptions/Contraption");check(contraption.methods.stream().anyMatch(m->m.name.equals("getBlocks")&&m.desc.equals("()Ljava/util/Map;")));
  System.out.println("PASS: legacy regions, structure UUID/opacity persistence, origin changes and installed contraption APIs");
 }
}
