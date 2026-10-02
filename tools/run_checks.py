from pathlib import Path
import json,shlex,subprocess,sys,zipfile
p=Path(__file__).resolve().parents[1];jdk=Path(sys.argv[1]);out=p/'build/local'
args=shlex.split((out/'javac.args').read_text(encoding='utf-8'))
cp=str(out/'classes')+';'+args[args.index('-classpath')+1]
sources=sorted((p/'tools').glob('*Test.java'))
testclasses=out/'tests';testclasses.mkdir(exist_ok=True)
compile_args=['--release','21','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(testclasses)]+list(map(str,sources))
argfile=out/'tests.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in compile_args),encoding='utf-8')
subprocess.run([str(jdk/'bin/javac.exe'),'-J-Duser.language=en','@'+str(argfile)],check=True)
for source in sources:
 runtime=cp
 if source.stem in {'HighlightStructureTest','MovingScreenHooksTest'}:runtime=';'.join(s for s in cp.split(';') if 'datafixerupper' not in s.lower() or '/8.0.16/' in s.replace('\\','/'))
 if source.stem=='OptionalRadarLoadTest':runtime=';'.join(s for s in cp.split(';') if 'create_radar' not in s.lower())
 runargs=out/'java-tests.args';runargs.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in ['-cp',str(testclasses)+';'+runtime,source.stem]),encoding='utf-8')
 subprocess.run([str(jdk/'bin/java.exe'),'@'+str(runargs)],check=True,cwd=p)
r=p/'src/main/resources';assets=r/'assets/omni_display'
for f in r.rglob('*.json'):json.loads(f.read_text(encoding='utf-8-sig'))
variants=json.loads((assets/'blockstates/thin_display.json').read_text())['variants'];assert len(variants)==384
names=['down','up','north','south','west','east'];axes=[1,1,2,2,0,0]
for key,variant in variants.items():
 fields=dict(x.split('=') for x in key.split(','));face=names.index(fields['attached_face']);mask=int(fields['screen_faces'])
 model=json.loads((assets/('models/'+variant['model'].split(':')[1]+'.json')).read_text());element=model['elements'][0]
 lo,hi=element['from'],element['to'];axis=axes[face]
 assert hi[axis]-lo[axis]==1 and (hi[axis]==16 if face%2 else lo[axis]==0)
 assert all(lo[a]==0 and hi[a]==16 for a in range(3) if a!=axis)
 for i in [face,face^1]:assert element['faces'][names[i]]['texture']==('#screen' if mask&(1<<i) else '#back')
for face in range(6):
 for inside in [0,1]:
  model=json.loads((assets/f'models/block/panel_{face}_{inside}.json').read_text());e=model['elements'][0];axis=axes[face]
  assert e['from'][axis]==e['to'][axis] and len(e['faces'])==2 and names[face] in e['faces']
  assert e['faces'][names[face^1]]['texture']=='#back'
  assert model['textures']['back']=='omni_display:block/screen_back'
  back=json.loads((assets/f'models/block/panel_back_{face}.json').read_text())
  assert set(back['elements'][0]['faces'])=={names[face^1]}
  assert abs(e['from'][axis]-(16 if face%2 else 0))<.02
hatches=json.loads((assets/'blockstates/cockpit_trapdoor.json').read_text())['variants'];assert len(hatches)==1024
for key,variant in hatches.items():
 fields=dict(x.split('=') for x in key.split(','));facing=names.index(fields['facing']);attached=(facing^1) if fields['open']=='true' else (1 if fields['half']=='top' else 0);axis=axes[attached];mask=int(fields['screen_faces'])
 model=json.loads((assets/('models/'+variant['model'].split(':')[1]+'.json')).read_text());element=model['elements'][0];lo,hi=element['from'],element['to']
 assert hi[axis]-lo[axis]==3 and (hi[axis]==16 if attached%2 else lo[axis]==0)
 assert all(lo[i]==0 and hi[i]==16 for i in range(3) if i!=axis)
 for i in [attached,attached^1]:assert element['faces'][names[i]]['texture']==('#screen' if mask&(1<<i) else '#back')
recipe=json.loads((r/'data/omni_display/recipe/cockpit_trapdoor.json').read_text())
assert recipe['pattern']==['TTT'] and recipe['key']['T']['item']=='omni_display:thin_display' and recipe['result']=={'id':'omni_display:cockpit_trapdoor','count':1}
print('PASS: all 1024 hatch states match vanilla collision geometry, screen sides and 3-to-1 recipe')
jar=p/'build/libs/omni-display-1.3alpha5.jar'
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 for name in ['CockpitTrapdoor','ThinDisplayBlock','PanelStore','ScreenPanelItem','client/PanelRendering','mixin/PanelAssemblyMixin','mixin/PanelBlockChangeMixin']:
  b=z.read('dev/omnidisplay/'+name+'.class');assert int.from_bytes(b[6:8],'big')==65
 assert b'1.3alpha5' in z.read('META-INF/neoforge.mods.toml')
 assert 'PanelAssemblyMixin' in json.loads(z.read('omni_display.mixins.json'))['mixins']
print('PASS: 384 thin states, all 12 panel surfaces, JSON resources, Java 21 bytecode and package integrity')

with zipfile.ZipFile(jar) as z:
 configs=json.loads(z.read('omni_display.mixins.json'))
 for name in ['PartitionLevelMixin','PartitionFlywheelMixin']:assert name in configs['client'] and 'dev/omnidisplay/mixin/'+name+'.class' in z.namelist()
 shader=json.loads(z.read('assets/omni_display/shaders/core/partition_composite.json'))
 assert {s['name'] for s in shader['samplers']}=={'NormalSampler','ScreenSampler','MaskSampler','NormalDepth','ScreenDepth'}
 assert b'gl_FragDepth' in z.read('assets/omni_display/shaders/core/partition_composite.fsh')
print('PASS: partition mixins, shader samplers and depth compositor packaged')

recipes=r/'data/omni_display/recipe'
panel=json.loads((recipes/'screen_panel.json').read_text());assert panel['key']['P']['item']=='create:precision_mechanism' and panel['result']['id']=='omni_display:screen_panel'
display=json.loads((recipes/'display.json').read_text());assert {i['item'] for i in display['ingredients']}=={'omni_display:screen_panel','minecraft:glass_pane'} and display['result']['id']=='omni_display:display'
for name in ['thin_display','cockpit_door']:assert json.loads((recipes/(name+'.json')).read_text())['key']['D']['item']=='omni_display:screen_panel'
print('PASS: direct panel recipe and panel-based display, thin display and door recipes')
