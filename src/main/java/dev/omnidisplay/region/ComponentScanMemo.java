package dev.omnidisplay.region;
import java.util.*;
import java.util.function.*;
/** Reuse both successful and failed detection for every seed in the same component. */
public final class ComponentScanMemo<K,V> {
 private record Outcome<V>(V value,String failure){}
 private final Map<K,Outcome<V>> entries=new HashMap<>();
 public V scan(K seed,Supplier<? extends Collection<K>> collect,Function<Collection<K>,V> detect,Function<V,? extends Collection<K>> accepted){
  var old=entries.get(seed);if(old!=null)return result(old);
  var component=collect.get();Outcome<V> outcome;
  try{outcome=new Outcome<>(detect.apply(component),null);}
  catch(IllegalArgumentException failure){outcome=new Outcome<>(null,failure.getMessage());if(outcome.failure==null)outcome=new Outcome<>(null,"Invalid screen component");}
  if(entries.size()>8192)entries.clear();
  var keys=outcome.failure==null?accepted.apply(outcome.value):component;
  for(var key:keys)entries.put(key,outcome);
  entries.put(seed,outcome);return result(outcome);
 }
 private V result(Outcome<V> outcome){if(outcome.failure!=null)throw new IllegalArgumentException(outcome.failure);return outcome.value;}
 public void clear(){entries.clear();}
}
