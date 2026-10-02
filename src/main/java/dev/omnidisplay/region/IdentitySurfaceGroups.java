package dev.omnidisplay.region;
import java.util.*;
import java.util.function.*;
/** Compare owning cabins once per group, never once per screen face. */
public final class IdentitySurfaceGroups<K,V> {
 private record Group<K,V>(K owner,List<V> values){}
 private final List<Group<K,V>> groups;
 public IdentitySurfaceGroups(List<V> values,Function<V,K> owner){
  var indexed=new IdentityHashMap<K,List<V>>();var order=new ArrayList<K>();
  for(var value:values){var key=owner.apply(value);var group=indexed.get(key);if(group==null){group=new ArrayList<>();indexed.put(key,group);order.add(key);}group.add(value);}
  var result=new ArrayList<Group<K,V>>();for(var key:order)result.add(new Group<>(key,List.copyOf(indexed.get(key))));groups=List.copyOf(result);
 }
 public List<V> matching(K current,BiPredicate<K,K> match){
  List<V> result=null;
  for(var group:groups)if(group.owner==current||match.test(group.owner,current)){
   if(result==null)result=group.values;else{var merged=new ArrayList<V>(result);merged.addAll(group.values);result=merged;}
  }
  return result==null?List.of():result;
 }
}
