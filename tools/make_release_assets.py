"""Native item models reuse the mod's existing screen textures. No world models change."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1];r=p/'src/main/resources';a=r/'assets/omni_display'
def put(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
textures={'screen':'omni_display:block/screen_hex','back':'omni_display:block/screen_back','edge':'omni_display:block/door_edge','seams':'omni_display:block/screen_edges','brass':'minecraft:block/gold_block','cyan':'minecraft:block/oxidized_copper','particle':'omni_display:block/screen_back'}
def box(lo,hi,texture='edge',front=None):
 return {'from':lo,'to':hi,'faces':{d:{'uv':[0,0,16,16],'texture':'#'+(front if front and d=='north' else texture)} for d in ['down','up','north','south','west','east']}}
def screen(lo,hi):
 main=box(lo,hi,'back','screen');x,y,z=lo;xx,yy,zz=hi
 lines={'from':[x,y,z-.015],'to':[xx,yy,z-.015],'shade':False,'faces':{'north':{'uv':[0,0,16,16],'texture':'#seams'}}}
 return [main,lines]
def save(name,elements,scale=.85):
 put(a/f'models/item/{name}.json',{'parent':'minecraft:block/block','gui_light':'front','textures':textures,'elements':elements,'display':{
  'gui':{'rotation':[15,155,0],'translation':[0,0,0],'scale':[scale]*3},
  'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5]*3},
  'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.85]*3},
  'thirdperson_righthand':{'rotation':[75,20,0],'translation':[0,2.5,0],'scale':[.45]*3},
  'firstperson_righthand':{'rotation':[0,-25,0],'translation':[0,2,0],'scale':[.65]*3}}})
# Tall door silhouette, structural rim, split glass, brass pull handle.
door=[box([3,0,7],[4,16,9]),box([12,0,7],[13,16,9]),box([4,0,7],[12,1,9]),box([4,15,7],[12,16,9])]
door+=screen([4,1,7.4],[12,15,8.6])
door += [box([7.85,1,7],[8.15,15,7.35]),box([10.75,6.5,6.5],[11.35,9.5,7.5],'brass'),box([4.25,14.4,7.1],[6.5,14.7,7.35],'cyan')]
save('cockpit_door',door,.95)
# Rigid thin panel: square cyan corner reinforcements and a visible metal edge.
thin=[box([0,0,7],[16,16,8.2])]+screen([.7,.7,6.85],[15.3,15.3,7])
for x,y in [(0,0),(14,0),(0,14),(14,14)]:thin.append(box([x,y,6.6],[x+2,y+2,6.85],'cyan'))
save('thin_display',thin,.82)
# Adhesive film: an offset release liner and a lifted corner distinguish it from a rigid panel.
panel=[box([.5,.5,8],[13.5,13.5,8.15],'brass')]+screen([2,2,7.6],[15,15,7.8])
flap=box([12.5,12.5,7.4],[15.5,15.5,7.6],'brass');flap['rotation']={'origin':[12.5,12.5,7.5],'axis':'x','angle':-22.5};panel.append(flap)
save('screen_panel',panel,.88)
# Survival recipes and recipe-book unlocks.
def shaped(name,pattern,key,count=1):
 put(r/f'data/omni_display/recipe/{name}.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':'omni_display:'+name,'count':count}})
shaped('display',['IGI','RPR','IGI'],{'I':'minecraft:iron_ingot','G':'minecraft:glass','R':'minecraft:redstone','P':'create:precision_mechanism'})
put(r/'data/omni_display/recipe/screen_panel.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':[{'item':'omni_display:display'},{'item':'minecraft:glass_pane'}],'result':{'id':'omni_display:screen_panel','count':1}})
shaped('cockpit_door',['D','D'],{'D':'omni_display:display'})
shaped('thin_display',['DDD'],{'D':'omni_display:display'},6)
for name in ['display','screen_panel','cockpit_door','thin_display']:
 ingredient='create:precision_mechanism' if name=='display' else 'omni_display:display'
 put(r/f'data/omni_display/advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_ingredient':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[ingredient]}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'omni_display:'+name}}},'requirements':[['has_ingredient','has_the_recipe']],'rewards':{'recipes':['omni_display:'+name]}})
for lang in ['zh_cn','en_us']:
 f=a/f'lang/{lang}.json';d=json.loads(f.read_text(encoding='utf-8-sig'));d['itemGroup.omni_display']='全周天显示器' if lang=='zh_cn' else 'Omni Display';d['block.omni_display.display']='全周天显示器' if lang=='zh_cn' else 'Omnidirectional Display';put(f,d)
print('Generated 3 distinct item models, 4 recipes, recipe unlocks and tab translations.')
