import dev.omnidisplay.region.RenderRecovery;
public class RenderRecoveryTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  var r=new RenderRecovery();check(!r.retry(100,4));r.fail(100,4);
  check(!r.retry(101,5));check(r.retry(102,5));check(!r.retry(119,4));check(r.retry(120,4));
  r.fail(120,4);check(!r.retry(159,4));check(r.retry(160,4));
  long tick=200;for(int i=0;i<12;i++){r.fail(tick,4);check(!r.retry(tick+1,4));check(r.retry(tick+400,4));tick+=400;}
  check(r.retry(0,4));r.reset();check(!r.retry(100000,5));r.fail(0,5);check(!r.retry(19,5));check(r.retry(20,5));
  System.out.println("PASS: temporary render failure recovery, topology repair retry, exponential backoff, bounded repeated failures, clock reset and successful-frame reset");
 }
}
