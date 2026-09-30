"""Compile against the installed NeoForge 21.1.228 development-named runtime.

The regular Gradle build remains supported. This offline path avoids downloading
another full Minecraft toolchain when the exact dependencies are installed.
"""
from pathlib import Path
import subprocess,zipfile,sys,shutil
p=Path(__file__).resolve().parents[1]
game=Path(sys.argv[1]);jdk=Path(sys.argv[2]);libs=game/'libraries'
out=p/'build/local';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
nested=out/'dependencies';nested.mkdir(parents=True,exist_ok=True)
for file in (p/'libs').glob('*.jar'):
 with zipfile.ZipFile(file) as z:
  for name in z.namelist():
   if name.startswith('META-INF/jarjar/') and name.endswith('.jar'):
    (nested/Path(name).name).write_bytes(z.read(name))
nf=libs/'net/neoforged/neoforge/21.1.228'
cp=[nf/'neoforge-21.1.228-client.jar',nf/'neoforge-21.1.228-universal.jar']
cp+=[libs/'net/minecraft/client/1.21.1-20240808.144430/client-1.21.1-20240808.144430-srg.jar']
cp+=list((p/'libs').glob('*.jar'))+list(nested.glob('*.jar'))
cp+=[f for f in libs.rglob('*.jar') if 'net/minecraftforge' not in f.as_posix() and 'net/neoforged/neoforge/' not in f.as_posix() and 'net/minecraft/client/' not in f.as_posix()]
args=['--release','21','-encoding','UTF-8','-proc:none','-classpath',';'.join(map(str,cp)),'-d',str(classes)]+list(map(str,(p/'src/main/java').rglob('*.java')))
argfile=out/'javac.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),encoding='utf-8')
subprocess.run([str(jdk/'bin/javac.exe'),'-J-Duser.language=en','@'+str(argfile)],check=True,cwd=p)
dest=p/'build/libs/omni-display-1.0.jar';dest.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
 for root in [classes,p/'src/main/resources']:
  for f in root.rglob('*'):
   if f.is_file():z.write(f,f.relative_to(root).as_posix())
print(dest)
