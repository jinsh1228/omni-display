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
  assert e['from'][axis]==e['to'][axis] and len(e['faces'])==1 and names[face] in e['faces']
  assert abs(e['from'][axis]-(16 if face%2 else 0))<.02
jar=p/'build/libs/omni-display-1.0.jar'
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 for name in ['ThinDisplayBlock','PanelStore','ScreenPanelItem','client/PanelRendering','mixin/PanelAssemblyMixin','mixin/PanelBlockChangeMixin']:
  b=z.read('dev/omnidisplay/'+name+'.class');assert int.from_bytes(b[6:8],'big')==65
 assert b'1.0' in z.read('META-INF/neoforge.mods.toml')
 assert 'PanelAssemblyMixin' in json.loads(z.read('omni_display.mixins.json'))['mixins']
print('PASS: 384 thin states, all 12 panel surfaces, JSON resources, Java 21 bytecode and package integrity')
