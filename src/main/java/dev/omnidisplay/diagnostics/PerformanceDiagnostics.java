package dev.omnidisplay.diagnostics;
import java.nio.file.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import jdk.jfr.*;
/** Diagnostic build only. All file output and JFR dumps run off the game threads. */
public final class PerformanceDiagnostics {
 public enum Phase {RENDER_ALL,WORLD_PASS,WORLD_SCENES,COMPOSE,MASK_BUILD,PANELS,HUD,HOLOGRAM_BUILD,DISCOVERY,SURFACE_CACHE,CONTENTS_CACHE,ANIMATION_BUILD,CLIENT_TICK,SERVER_TICK,CABIN_REBUILD,TOPOLOGY_SCAN,PANEL_REFRESH,PANEL_RECEIVE,RADAR}
 private static final class Counters {final long[] total=new long[Phase.values().length],max=new long[Phase.values().length],calls=new long[Phase.values().length];void clear(){Arrays.fill(total,0);Arrays.fill(max,0);Arrays.fill(calls,0);}}
 private static final ThreadLocal<Counters> counters=ThreadLocal.withInitial(Counters::new);
 private static final ArrayBlockingQueue<String> output=new ArrayBlockingQueue<>(512);
 private static final AtomicLong dropped=new AtomicLong();
 private static final ThreadMXBean threads=ManagementFactory.getThreadMXBean();
 private static final List<GarbageCollectorMXBean> collectors=ManagementFactory.getGarbageCollectorMXBeans();
 private static volatile boolean enabled;private static boolean initialized;
 private static volatile Thread renderThread,serverThread;
 private static volatile long frameStart,serverStart;
 private static volatile String context="",sceneMode="unknown";
 public static void sceneMode(String mode){sceneMode=mode;}
 private static long frameCpu,lastGcTime,lastGcCount,lastFrameReport,lastServerReport,lastPeriodic,frameId;
 private static volatile long lastWatchdog;private static final AtomicLong lastDump=new AtomicLong();
 private static volatile Recording recording;private static Path directory;
 private static BufferedWriter writer;private static long written;private static int dumpIndex;
 private static final ExecutorService dumps=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Omni-Performance-JFR");t.setDaemon(true);return t;});
 public static synchronized void initialize(Path game){
  enabled=true;if(initialized)return;initialized=true;
  try{
   directory=game.resolve("omni-display-debug/perf-1.3alpha5").resolve(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));Files.createDirectories(directory);
   writer=Files.newBufferedWriter(directory.resolve("stutter.log"));
   var worker=Executors.newScheduledThreadPool(2,r->{var t=new Thread(r,"Omni-Performance-Logger");t.setDaemon(true);return t;});
   worker.scheduleWithFixedDelay(PerformanceDiagnostics::flush,100,100,TimeUnit.MILLISECONDS);
   worker.scheduleWithFixedDelay(PerformanceDiagnostics::watchdog,250,250,TimeUnit.MILLISECONDS);
   log("SESSION version=1.3alpha5 java="+System.getProperty("java.version")+" maxHeapMB="+(Runtime.getRuntime().maxMemory()/1048576)+" directory="+directory);
   log("Timings are inclusive/nested; do not add parent and child times. Frame includes tick/render/swap/FPS limiter. CPU/GPU stalls require stack/JFR correlation.");
   org.slf4j.LoggerFactory.getLogger("OmniDisplay").info("Performance diagnostics: {}",directory);
   dumps.execute(PerformanceDiagnostics::startRecording);
   Runtime.getRuntime().addShutdownHook(new Thread(()->{flush();var r=recording;if(r!=null)try{r.close();}catch(Exception ignored){}},"Omni-Performance-Shutdown"));
  }catch(Exception error){enabled=false;org.slf4j.LoggerFactory.getLogger("OmniDisplay").warn("Cannot initialize performance diagnostics",error);}
 }
 public static void idle(){enabled=false;frameStart=0;}
 public static long begin(Phase phase){return enabled?System.nanoTime():0;}
 public static void end(Phase phase,long start){if(start==0)return;long elapsed=System.nanoTime()-start;var c=counters.get();int i=phase.ordinal();c.total[i]+=elapsed;c.max[i]=Math.max(c.max[i],elapsed);c.calls[i]++;}
 private static long cpu(){return threads.isCurrentThreadCpuTimeSupported()&&threads.isThreadCpuTimeEnabled()?threads.getCurrentThreadCpuTime():-1;}
 private static long gc(boolean count){long total=0;for(var collector:collectors){long n=count?collector.getCollectionCount():collector.getCollectionTime();if(n>=0)total+=n;}return total;}
 public static void beginFrame(){if(!enabled)return;counters.get().clear();renderThread=Thread.currentThread();frameCpu=cpu();lastGcTime=gc(false);lastGcCount=gc(true);frameStart=System.nanoTime();frameId++;}
 public static void finishFrame(String state){long start=frameStart;if(!enabled||start==0)return;long now=System.nanoTime();frameStart=0;context=state;state+=" sceneMode="+sceneMode;long elapsed=now-start,ct=cpu();long cpuNanos=ct<0||frameCpu<0?-1:ct-frameCpu;long gcTime=gc(false),gcCount=gc(true);long deltaGc=Math.max(0,gcTime-lastGcTime),deltaCount=Math.max(0,gcCount-lastGcCount);lastGcTime=gcTime;lastGcCount=gcCount;
  boolean slow=elapsed>=100_000_000L;
  if(slow&&now-lastFrameReport>=500_000_000L||now-lastPeriodic>=5_000_000_000L){
   if(slow)lastFrameReport=now;lastPeriodic=now;
   log((slow?"FRAME_STALL":"FRAME_STATUS")+" id="+frameId+" wallMs="+ms(elapsed)+" cpuMs="+ms(cpuNanos)+" gcMsSinceLastFrame="+deltaGc+" gcCount="+deltaCount+" heapMB="+heap()+" "+state+" phases="+summary(counters.get()));
  }
  if(elapsed>=200_000_000L)requestDump(now);
 }
 public static void beginServer(){if(!enabled)return;serverThread=Thread.currentThread();counters.get().clear();serverStart=System.nanoTime();}
 public static void finishServer(){long start=serverStart;if(!enabled||start==0)return;long now=System.nanoTime();serverStart=0;end(Phase.SERVER_TICK,start);if(now-start>=100_000_000L&&now-lastServerReport>=500_000_000L){lastServerReport=now;log("SERVER_STALL wallMs="+ms(now-start)+" phases="+summary(counters.get()));if(now-start>=200_000_000L)requestDump(now);}}
 private static String summary(Counters c){var text=new StringBuilder();for(var phase:Phase.values()){int i=phase.ordinal();if(c.calls[i]==0)continue;if(text.length()>0)text.append(';');text.append(phase).append('=').append(ms(c.total[i])).append("ms/").append(c.calls[i]).append("calls/max").append(ms(c.max[i])).append("ms");}return text.toString();}
 private static String ms(long nanos){return nanos<0?"unavailable":String.format(Locale.ROOT,"%.2f",nanos/1_000_000d);}
 private static long heap(){var runtime=Runtime.getRuntime();return (runtime.totalMemory()-runtime.freeMemory())/1048576;}
 public static void note(String message){if(enabled)log(message);}
 private static void log(String text){if(!output.offer(Instant.now()+" "+text))dropped.incrementAndGet();}
 private static synchronized void flush(){if(writer==null)return;try{String line;int n=0;while(n++<128&&(line=output.poll())!=null){if(written>20*1024*1024L)continue;writer.write(line);writer.newLine();written+=line.length()+1;}long lost=dropped.getAndSet(0);if(lost>0){writer.write("Dropped diagnostic messages: "+lost);writer.newLine();}writer.flush();}catch(Exception ignored){}}
 private static void watchdog(){try{if(!enabled)return;long now=System.nanoTime();boolean frame=frameStart!=0&&now-frameStart>500_000_000L,server=serverStart!=0&&now-serverStart>500_000_000L;if((frame||server)&&now-lastWatchdog>=2_000_000_000L){lastWatchdog=now;log("WATCHDOG frameMs="+(frame?ms(now-frameStart):"0")+" serverMs="+(server?ms(now-serverStart):"0")+" heapMB="+heap()+" "+context);if(frame)stack(renderThread);if(server)stack(serverThread);requestDump(now);}}catch(Exception error){log("Watchdog error: "+error);}}
 private static void stack(Thread thread){if(thread==null)return;var b=new StringBuilder("STACK "+thread.getName()+" state="+thread.getState());for(var frame:thread.getStackTrace())b.append("\n  at ").append(frame);log(b.toString());}
 private static void startRecording(){try{var r=new Recording();r.setName("Omni Display stutter diagnostics");r.setToDisk(true);r.setMaxAge(Duration.ofSeconds(60));r.setMaxSize(16*1024*1024L);r.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(20)).withStackTrace();r.enable("jdk.GarbageCollection").withThreshold(Duration.ZERO);r.enable("jdk.GCPhasePause").withThreshold(Duration.ZERO);r.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));r.enable("jdk.ThreadCPULoad").withPeriod(Duration.ofSeconds(1));r.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(20)).withStackTrace();r.enable("jdk.ThreadPark").withThreshold(Duration.ofMillis(50)).withStackTrace();r.start();recording=r;log("JFR rolling capture started: 60 seconds, 16MB, 20ms CPU sampling");}catch(Exception|LinkageError error){log("JFR unavailable; timing/stack diagnostics continue: "+error);}}
 private static void requestDump(long now){if(recording==null)return;long old=lastDump.get();if(now-old<20_000_000_000L||!lastDump.compareAndSet(old,now))return;dumps.execute(()->{try{var file=directory.resolve("stutter-"+(dumpIndex++%3+1)+".jfr");recording.dump(file);log("JFR_SAVED "+file);}catch(Exception error){log("JFR dump failed: "+error);}});}
}
