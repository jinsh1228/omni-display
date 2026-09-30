import dev.omnidisplay.region.HudDesign;
public class HudPreview {
 public static void main(String[] args){
  double now=args.length>0?Double.parseDouble(args[0]):120;
  for(var l:HudDesign.front(now,82,256,123,63,true,true,true))print(l,0,0,1);
  for(var l:HudDesign.target(now,now))print(l,12,7,3);
 }
 static void print(HudDesign.Line l,double x,double y,double scale){System.out.printf(java.util.Locale.ROOT,"%f %f %f %f %06x %d %f%n",x+l.x()*scale,y+l.y()*scale,x+l.u()*scale,y+l.v()*scale,l.color(),l.alpha(),l.weight());}
}
