from pathlib import Path
import shutil,zipfile,datetime,hashlib
project=Path(__file__).resolve().parents[1]
dest=project.parent/'deliverables'
backup=dest/('backup-1.3alpha5-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
backup.mkdir(parents=True)
for name in ['omni-display-1.3alpha5.jar','omni-display-1.3alpha5-source.zip','omni-display-1.3alpha5-说明.md']:
 p=dest/name
 if p.exists():shutil.copy2(p,backup/name)
shutil.copy2(project/'build/libs/omni-display-1.3alpha5.jar',dest/'omni-display-1.3alpha5.jar')
shutil.copy2(project/'RELEASE_1_3ALPHA5.md',dest/'omni-display-1.3alpha5-说明.md')
with zipfile.ZipFile(dest/'omni-display-1.3alpha5-source.zip','w',zipfile.ZIP_DEFLATED) as out:
 for p in sorted(project.rglob('*')):
  rel=p.relative_to(project)
  if p.is_file() and not any(part in {'build','.gradle','__pycache__','.git','logs'} for part in rel.parts) and p.suffix!='.log':out.write(p,Path(project.name)/rel)
with zipfile.ZipFile(dest/'omni-display-1.3alpha5.jar') as jar:
 assert jar.testzip() is None
 for name in ['MovingScreens','client/MovingScreenView','region/RenderRecovery','mixin/MovingScreenInteractionMixin','mixin/MovingPanelTransferMixin','mixin/ContraptionScreenAccessor','ScreenTopologyCache','client/ScreenSurfaceCache','region/ScreenVisibility','HighlightStructures','CabinSelector','region/GroupedCabin','client/HighlightModels','client/ContraptionHighlights']:assert 'dev/omnidisplay/'+name+'.class' in jar.namelist()
 assert 'assets/omni_display/models/item/cabin_selector.json' in jar.namelist()
 assert 'data/omni_display/recipe/cabin_selector.json' in jar.namelist()
 assert b'version="1.3alpha5"' in jar.read('META-INF/neoforge.mods.toml').replace(b' ',b'')
with zipfile.ZipFile(dest/'omni-display-1.3alpha5-source.zip') as source:assert source.testzip() is None
print('Packaged 1.3alpha5 optimization update; previous artifacts backed up.')
print('SHA256:',hashlib.sha256((dest/'omni-display-1.3alpha5.jar').read_bytes()).hexdigest())
