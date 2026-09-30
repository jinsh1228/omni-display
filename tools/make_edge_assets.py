from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]
r=p/"src/main/resources"
# Generate a new transparent border texture from the same procedural lattice (no black panels).
from PIL import Image,ImageDraw
im=Image.new('RGBA',(256,256),(0,0,0,0));d=ImageDraw.Draw(im)
for col in range(-1,6):
 for row in range(-1,6):
  x=col*64;y=row*64+(col%2)*32
  d.polygon([(x-44,y),(x-20,y-32),(x+20,y-32),(x+44,y),(x+20,y+32),(x-20,y+32)],fill=(0,0,0,0),outline=(24,42,49,255),width=1)
im.save(r/'assets/omni_display/textures/block/screen_edges.png')
root=r/'assets/omni_display/models/block'
faces=['down','up','north','south','west','east']
# Match Minecraft cube UVs; epsilon moves only along the face normal, not the tiling axes.
for mask in range(1,64):
 elements=[]
 for i,face in enumerate(faces):
  if not mask&(1<<i):continue
  lo=[0,0,0];hi=[16,16,16];axis={'down':1,'up':1,'north':2,'south':2,'west':0,'east':0}[face]
  if face in ['down','north','west']:lo[axis]=hi[axis]=-.012
  else:lo[axis]=hi[axis]=16.012
  elements.append({'from':lo,'to':hi,'shade':False,'faces':{face:{'uv':[0,0,16,16],'texture':'#edge'}}})
 (root/f'edges_{mask}.json').write_text(json.dumps({'textures':{'edge':'omni_display:block/screen_edges'},'elements':elements}))
for mask in range(1,4):
 elements=[]
 for bit,face,x in [(1,'east',3.012),(2,'west',-.012)]:
  if mask&bit:elements.append({'from':[x,0,0],'to':[x,16,16],'shade':False,'faces':{face:{'uv':[0,0,16,16],'texture':'#edge'}}})
 (root/f'door_edges_{mask}.json').write_text(json.dumps({'textures':{'edge':'omni_display:block/screen_edges'},'elements':elements}))
print('Generated border layers and renderers')
