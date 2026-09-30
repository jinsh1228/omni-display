"""Orthographic resource preview, not an in-game screenshot."""
from pathlib import Path
import sys,json,zipfile,io,math
import numpy as np
from PIL import Image,ImageDraw
p=Path(__file__).resolve().parents[1];a=p/'src/main/resources/assets/omni_display'
vanilla=zipfile.ZipFile(sys.argv[1]);dest=Path(sys.argv[2])
def texture(name):
 ns,path=name.split(':');rel=f'assets/{ns}/textures/{path}.png'
 return Image.open(io.BytesIO(vanilla.read(rel)) if ns=='minecraft' else p/'src/main/resources'/rel).convert('RGBA')
def rot(axis,degrees):
 t=math.radians(degrees);c,s=math.cos(t),math.sin(t)
 if axis=='x':return np.array([[1,0,0],[0,c,-s],[0,s,c]])
 if axis=='y':return np.array([[c,0,s],[0,1,0],[-s,0,c]])
 return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def preview(name):
 m=json.loads((a/f'models/item/{name}.json').read_text());gui=m['display']['gui'];rx,ry,rz=gui['rotation'];matrix=rot('x',rx)@rot('y',ry)@rot('z',rz);faces=[]
 for e in m['elements']:
  x,y,z=e['from'];X,Y,Z=e['to']
  corners={'north':[[X,Y,z],[x,Y,z],[x,y,z],[X,y,z]],'south':[[x,Y,Z],[X,Y,Z],[X,y,Z],[x,y,Z]],'east':[[X,Y,Z],[X,Y,z],[X,y,z],[X,y,Z]],'west':[[x,Y,z],[x,Y,Z],[x,y,Z],[x,y,z]],'up':[[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]],'down':[[x,y,Z],[X,y,Z],[X,y,z],[x,y,z]]}
  normals={'north':[0,0,-1],'south':[0,0,1],'east':[1,0,0],'west':[-1,0,0],'up':[0,1,0],'down':[0,-1,0]}
  for direction,f in e['faces'].items():
   v=np.array(corners[direction],float);n=np.array(normals[direction],float)
   if 'rotation' in e:
    r=e['rotation'];o=np.array(r['origin']);local=rot(r['axis'],r['angle']);v=(v-o)@local.T+o;n=local@n
   v=(v-8)@matrix.T;n=matrix@n
   if n[2]<=0:continue
   xy=np.stack([128+v[:,0]*gui['scale'][0]*14,128-v[:,1]*gui['scale'][1]*14],axis=1)
   faces.append((v[:,2],xy,texture(m['textures'][f['texture'][1:]])))
 canvas=np.zeros((256,256,4),dtype=np.uint8);depth=np.full((256,256),-np.inf);yy,xx=np.mgrid[:256,:256]
 for zz,xy,tex in faces:
  w,h=tex.size;uv=[(0,0),(w,0),(w,h),(0,h)];eq=[];rhs=[]
  for (x,y),(u,v) in zip(xy,uv):eq.extend([[x,y,1,0,0,0,-u*x,-u*y],[0,0,0,x,y,1,-v*x,-v*y]]);rhs.extend([u,v])
  try:coeff=np.linalg.solve(eq,rhs)
  except np.linalg.LinAlgError:continue
  tile=tex.transform((256,256),Image.Transform.PERSPECTIVE,coeff,Image.Resampling.NEAREST)
  mask=Image.new('L',(256,256));ImageDraw.Draw(mask).polygon([tuple(pt) for pt in xy],fill=255)
  alpha=np.minimum(np.asarray(mask),np.asarray(tile.getchannel('A')))
  plane=np.linalg.solve(np.column_stack([xy[:3],np.ones(3)]),zz[:3]);z=plane[0]*xx+plane[1]*yy+plane[2]
  take=(alpha>0)&(z>depth);pixels=np.asarray(tile);canvas[take]=pixels[take];depth[take]=z[take]
 return Image.fromarray(canvas)
sheet=Image.new('RGB',(840,350),(30,34,42));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(['cockpit_door','thin_display','screen_panel']):
 im=preview(name);sheet.paste(im,(12+i*280,10),im);draw.text((30+i*280,275),name,fill='white')
 small=im.resize((16,16),Image.Resampling.LANCZOS).resize((48,48),Image.Resampling.NEAREST);sheet.paste(small,(112+i*280,296),small)
dest.parent.mkdir(parents=True,exist_ok=True);sheet.save(dest);print(dest)
