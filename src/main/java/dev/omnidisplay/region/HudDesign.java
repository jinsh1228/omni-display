package dev.omnidisplay.region;
import java.util.*;
/** Shared angular vector artwork used by the in-game renderer and design preview. */
public final class HudDesign {
 public record Line(double x,double y,double u,double v,int color,int alpha,double weight){}
 public static final int ICE=0xCEEAF4,CYAN=0x70E6DE,VIOLET=0xA398D7,AMBER=0xFFC17E,RED=0xFF627C;
 private final List<Line> lines=new ArrayList<>();
 void line(double x,double y,double u,double v,int c,int a,double w){lines.add(new Line(x,y,u,v,c,a,w));}
 void arc(double x,double y,double r,double start,double end,int c,int a,double w){for(double t=start;t<end;t+=3){double n=Math.min(t+3,end);line(x+r*Math.cos(Math.toRadians(t)),y+r*Math.sin(Math.toRadians(t)),x+r*Math.cos(Math.toRadians(n)),y+r*Math.sin(Math.toRadians(n)),c,a,w);}}
 void frame(double x,double y,double w,double h,int c){line(x,y,x+w-2,y,c,140,1);line(x+w-2,y,x+w,y+2,c,140,1);line(x+w,y+2,x+w,y+h,c,140,1);line(x+w,y+h,x+2,y+h,c,140,1);line(x+2,y+h,x,y+h-2,c,140,1);line(x,y+h-2,x,y,c,140,1);}
 private static final String CHARS="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-./";
 private static final String[] FONT={"111/101/101/101/111","010/110/010/010/111","111/001/111/100/111","111/001/111/001/111","101/101/111/001/001","111/100/111/001/111","111/100/111/101/111","111/001/010/010/010","111/101/111/101/111","111/101/111/001/111","010/101/111/101/101","110/101/110/101/110","111/100/100/100/111","110/101/101/101/110","111/100/110/100/111","111/100/110/100/100","111/100/101/101/111","101/101/111/101/101","111/010/010/010/111","001/001/001/101/111","101/101/110/101/101","100/100/100/100/111","101/111/111/101/101","101/111/111/111/101","111/101/101/101/111","110/101/110/100/100","111/101/101/111/001","110/101/110/101/101","111/100/111/001/111","111/010/010/010/010","101/101/101/101/111","101/101/101/101/010","101/101/111/111/101","101/101/010/101/101","101/101/010/010/010","111/001/010/100/111","000/000/111/000/000","000/000/000/000/010","001/001/010/100/100"};
 void text(String s,double x,double y,double size,int c,int a){
  double q=size/5;for(int n=0;n<s.length();n++){int index=CHARS.indexOf(s.charAt(n));if(index<0)continue;String[] rows=FONT[index].split("/");
   for(int row=0;row<5;row++)for(int col=0;col<3;col++)if(rows[row].charAt(col)=='1'){
    double px=x+(n*4+col)*q,py=y-row*q;
    line(px,py,px+q*.72,py,c,a,.85);
    if(col<2&&rows[row].charAt(col+1)=='1')line(px+q*.72,py,px+q,py,c,a,.85);
    if(row<4&&rows[row+1].charAt(col)=='1')line(px+q*.36,py,px+q*.36,py-q,c,a,.85);
   }
  }
 }
 public static List<Line> front(double now,double speed,double height,double heading,int mask,boolean linked,boolean mouse,boolean lock){
  var d=new HudDesign();double t=now/20;
  // Broad segmented instrument halo, leaving the central sightline free.
  if(mask!=0){for(int i=0;i<120;i++){double a=i*3;boolean major=i%5==0;double r=25.5,r2=major?26.7:26.05;
   double xx=r*Math.cos(Math.toRadians(a)),yy=r*Math.sin(Math.toRadians(a));if((yy>18&&Math.abs(xx)<18)||(yy<-17&&Math.abs(xx)<25))continue;
   int alpha=(i%30<3)?190:110;d.line(r*Math.cos(Math.toRadians(a)),r*Math.sin(Math.toRadians(a)),r2*Math.cos(Math.toRadians(a)),r2*Math.sin(Math.toRadians(a)),major?VIOLET:ICE,alpha,major?1.2:.65);
  }
  for(int sign:new int[]{-1,1}){d.arc(0,0,25,sign<0?140:-40,sign<0?220:40,VIOLET,110,.7);for(int i=0;i<7;i++){double y=-9+i*3;d.line(sign*38,y,sign*(i==3?40:39),y,ICE,110,.7);}d.line(sign*32,-14,sign*35,-17,VIOLET,150,1);d.line(sign*35,-17,sign*43,-17,VIOLET,150,1);}
  d.text("OMNI / PANORAMIC",-8.5,31,1.1,VIOLET,180);
  d.text("VECTOR 25",-4.5,-29,1.1,ICE,160);
  d.line(-11,-26,11,-26,VIOLET,80,.7);
  for(int i=0;i<24;i++)d.line(-10+i*.85,-27,-10+i*.85,-27.5,CYAN,(i==(int)(t*5)%24)?240:60,.8);
  }
  if((mask&32)!=0){d.arc(0,0,2.3,20,70,ICE,235,1.3);d.arc(0,0,2.3,110,160,ICE,235,1.3);d.arc(0,0,2.3,200,250,ICE,235,1.3);d.arc(0,0,2.3,290,340,ICE,235,1.3);d.line(-5,0,-2.9,0,CYAN,245,1.3);d.line(2.9,0,5,0,CYAN,245,1.3);d.line(0,-4,0,-2.8,CYAN,245,1.3);d.line(-.3,0,.3,0,ICE,255,1.8);d.line(0,-.3,0,.3,ICE,255,1.8);}
  if((mask&1)!=0){d.frame(-17,19,34,6,VIOLET);d.text("HDG",-2,24,1,ICE,210);d.text(String.format(java.util.Locale.ROOT,"%03d",Math.floorMod((int)Math.round(heading),360)),-2.5,22,1.5,CYAN,255);
   for(int i=-7;i<=7;i++){double x=(i-((heading%10)+10)%10/10)*2;if(x< -14||x>14)continue;d.line(x,18,x,18-(i%3==0?1.3:.6),ICE,150,.7);}d.line(-.8,15.5,0,16.4,AMBER,240,1);d.line(0,16.4,.8,15.5,AMBER,240,1);
  }
  if((mask&8)!=0)d.instrument(-33,-11,"SPD",String.format(java.util.Locale.ROOT,"%03d",Math.min(99999,Math.round(speed))),"M/S",speed,5,CYAN);
  if((mask&16)!=0)d.instrument(27,-11,"ALT",Long.toString(Math.round(height)),"WORLD Y",height,20,ICE);
  if(mask!=0){d.frame(-24,-22,17,5,VIOLET);d.text("RADAR",-22.5,-18,1.05,VIOLET,190);d.text(linked?"LINK":"OFFLINE",-22.5,-20,1.1,linked?CYAN:AMBER,230);
   d.frame(7,-22,17,5,VIOLET);d.text("TARGET",8.5,-18,1.05,VIOLET,190);d.text(lock?"LOCK":mouse?"SELECT":"STANDBY",8.5,-20,1.1,lock?RED:mouse?AMBER:ICE,230);
   if(linked){double phase=(t*50)%360;d.arc(-27,-20,1.6,phase,phase+95,CYAN,170,.8);d.arc(-27,-20,1.6,phase+180,phase+275,VIOLET,120,.6);}
  }
  return d.lines;
 }
 void instrument(double x,double y,String name,String value,String unit,double reading,double step,int color){
  frame(x-1,y-1,7,23,VIOLET);text(name,x,y+20,1.2,VIOLET,200);text(value,x,y+17,1.6,color,255);text(unit,x,y+14, .75,ICE,140);
  double phase=((reading%step)+step)%step/step;
  for(int i=0;i<9;i++){double yy=y+i*1.2-phase*1.2;if(yy<y||yy>y+10)continue;line(x,yy,x+(i%2==0?2:1),yy,color,140,.7);}
  line(x+3,y+5,x+4,y+5.7,AMBER,235,1);line(x+3,y+5,x+4,y+4.3,AMBER,235,1);
 }
 public static List<Line> target(double now,double age){var d=new HudDesign();double spread=1+.65*Math.exp(-Math.max(0,age)/4);int pulse=210+(int)(30*Math.sin(now*.14));
  for(int i=0;i<3;i++){double a=Math.toRadians(90+i*120),b=Math.toRadians(90+(i+1)*120);double x=Math.cos(a),y=Math.sin(a),u=Math.cos(b),v=Math.sin(b);
   d.line(x*spread,y*spread,u*spread,v*spread,RED,pulse,1.5);d.line(x*1.28,y*1.28,(x*.7+u*.3)*1.28,(y*.7+v*.3)*1.28,VIOLET,190,.85);
  }
  double phase=now*.7;d.arc(0,0,1.48,phase,phase+42,RED,130,.7);d.arc(0,0,1.48,phase+180,phase+222,RED,130,.7);
  d.line(-.1,0,.1,0,ICE,220,.7);d.line(0,-.1,0,.1,ICE,220,.7);d.text("LOCK",1.45,-.3,.32,RED,240);return d.lines;
 }
}
