import dev.omnidisplay.region.RenderRules;
public class RenderRulesTest {
 public static void main(String[] args){
  for(boolean mode:new boolean[]{false,true})for(boolean active:new boolean[]{false,true})if(RenderRules.locks(mode,active)!=(active&&!mode))throw new AssertionError("interlock");
  for(boolean mode:new boolean[]{false,true})for(boolean broken:new boolean[]{false,true})for(boolean open:new boolean[]{false,true}){boolean got=RenderRules.mayStart(mode,broken,open);if(broken&&got)throw new AssertionError("damaged start");if(!broken&&mode&&!got)throw new AssertionError("experimental open door");if(!broken&&!mode&&got==open)throw new AssertionError("ordinary open door");}
  if(RenderRules.mayEdit(false,false)||!RenderRules.mayEdit(true,false)||!RenderRules.mayEdit(false,true))throw new AssertionError("server authority");
  System.out.println("PASS: ordinary/experimental power and door interlocks, damaged cabin refusal, operator and singleplayer owner authority");
 }
}
