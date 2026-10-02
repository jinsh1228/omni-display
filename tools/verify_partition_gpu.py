"""Hidden Windows OpenGL test: depth-limited mask and the actual composite shaders."""
from pathlib import Path
import ctypes as C,struct
from ctypes import wintypes as W
p=Path(__file__).resolve().parents[1]
user=C.WinDLL('user32');gdi=C.WinDLL('gdi32');gl=C.WinDLL('opengl32')
user.CreateWindowExW.restype=W.HWND;user.CreateWindowExW.argtypes=[W.DWORD,W.LPCWSTR,W.LPCWSTR,W.DWORD,C.c_int,C.c_int,C.c_int,C.c_int,W.HWND,W.HMENU,W.HINSTANCE,C.c_void_p]
user.GetDC.restype=W.HDC;user.GetDC.argtypes=[W.HWND]
user.ReleaseDC.argtypes=[W.HWND,W.HDC];user.DestroyWindow.argtypes=[W.HWND]
class PFD(C.Structure):
 _fields_=[('size',W.WORD),('version',W.WORD),('flags',W.DWORD),('pixelType',C.c_ubyte),('colorBits',C.c_ubyte),('redBits',C.c_ubyte),('redShift',C.c_ubyte),('greenBits',C.c_ubyte),('greenShift',C.c_ubyte),('blueBits',C.c_ubyte),('blueShift',C.c_ubyte),('alphaBits',C.c_ubyte),('alphaShift',C.c_ubyte),('accumBits',C.c_ubyte),('accumRed',C.c_ubyte),('accumGreen',C.c_ubyte),('accumBlue',C.c_ubyte),('accumAlpha',C.c_ubyte),('depthBits',C.c_ubyte),('stencilBits',C.c_ubyte),('auxBuffers',C.c_ubyte),('layerType',C.c_ubyte),('reserved',C.c_ubyte),('layerMask',W.DWORD),('visibleMask',W.DWORD),('damageMask',W.DWORD)]
gdi.ChoosePixelFormat.argtypes=[W.HDC,C.POINTER(PFD)];gdi.SetPixelFormat.argtypes=[W.HDC,C.c_int,C.POINTER(PFD)]
gl.wglCreateContext.argtypes=[W.HDC];gl.wglCreateContext.restype=C.c_void_p
gl.wglMakeCurrent.argtypes=[W.HDC,C.c_void_p];gl.wglDeleteContext.argtypes=[C.c_void_p]
gl.wglGetProcAddress.argtypes=[C.c_char_p];gl.wglGetProcAddress.restype=C.c_void_p
window=user.CreateWindowExW(0,'STATIC','Omni Display GPU check',0,0,0,4,4,None,None,None,None)
assert window,'Cannot create hidden GPU test window'
dc=user.GetDC(window);context=None
def fn(name,result,args):
 addr=gl.wglGetProcAddress(name.encode())
 if addr and addr not in (1,2,3,C.c_void_p(-1).value):return C.WINFUNCTYPE(result,*args)(addr)
 f=getattr(gl,name);f.restype=result;f.argtypes=args;return f
U=C.c_uint;I=C.c_int;F=C.c_float;P=C.c_void_p
try:
 desc=PFD();desc.size=C.sizeof(PFD);desc.version=1;desc.flags=0x24;desc.colorBits=32;desc.depthBits=24;desc.stencilBits=8
 fmt=gdi.ChoosePixelFormat(dc,C.byref(desc));assert fmt and gdi.SetPixelFormat(dc,fmt,C.byref(desc))
 context=gl.wglCreateContext(dc);assert context and gl.wglMakeCurrent(dc,context)
 version=fn('glGetString',C.c_char_p,[U])(0x1F02);print('OpenGL:',version.decode())
 create=fn('glCreateShader',U,[U]);source=fn('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),P]);compile_=fn('glCompileShader',None,[U]);check=fn('glGetShaderiv',None,[U,U,C.POINTER(I)]);log=fn('glGetShaderInfoLog',None,[U,I,P,P])
 program=fn('glCreateProgram',U,[]);attach=fn('glAttachShader',None,[U,U]);link=fn('glLinkProgram',None,[U]);pcheck=fn('glGetProgramiv',None,[U,U,C.POINTER(I)]);bindattrib=fn('glBindAttribLocation',None,[U,U,C.c_char_p]);use=fn('glUseProgram',None,[U])
 def shader(code,kind):
  s=create(kind);data=C.c_char_p(code.encode());source(s,1,C.byref(data),None);compile_(s);ok=I();check(s,0x8B81,C.byref(ok))
  if not ok.value:
   out=C.create_string_buffer(4096);log(s,4096,None,out);raise AssertionError(out.value.decode())
  return s
 def makeprogram(vertex,fragment):
  pr=program();attach(pr,shader(vertex,0x8B31));attach(pr,shader(fragment,0x8B30));bindattrib(pr,0,b'Position');bindattrib(pr,1,b'UV0');bindattrib(pr,2 if 'in vec2 UV0' in vertex and 'in vec4 Color' in vertex else 1,b'Color');link(pr);ok=I();pcheck(pr,0x8B82,C.byref(ok));assert ok.value,'Program linking failed';return pr
 import re,json
 patch=(p/'src/main/java/dev/omnidisplay/region/PhotonTemporalPatch.java').read_text(encoding='utf8')
 literal=re.search(r'next=next\.replace\("void main\(\)", "((?:\\.|[^"\\])*)"\);',patch).group(1)
 spatial=json.loads('"'+literal+'"')
 makeprogram('#version 150\nin vec3 Position;void main(){gl_Position=vec4(Position,1);}', '#version 150\nuniform sampler2D inputCloud;out vec4 fragColor;\n'+spatial+'{fragColor=omniCloudSpatial(inputCloud, vec2(0.5));}')
 print('PASS: actual cloud spatial reconstruction helper compiles and links on GPU')
 root=p/'src/main/resources/assets/omni_display/shaders/core'
 composite=makeprogram((root/'partition_composite.vsh').read_text(),(root/'partition_composite.fsh').read_text())
 photon=makeprogram((root/'partition_photon_depth.vsh').read_text(),(root/'partition_photon_depth.fsh').read_text())
 maskprogram=makeprogram((root/'screen_surface_mask.vsh').read_text(),(root/'screen_surface_mask.fsh').read_text())
 uniform=fn('glGetUniformLocation',I,[U,C.c_char_p]);u1=fn('glUniform1i',None,[I,I]);um=fn('glUniformMatrix4fv',None,[I,I,U,P])
 identity=(F*16)(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
 for pr in [composite,maskprogram,photon]:
  use(pr)
  for name in [b'ModelViewMat',b'ProjMat']:um(uniform(pr,name),1,0,identity)
 texgen=fn('glGenTextures',None,[I,C.POINTER(U)]);texbind=fn('glBindTexture',None,[U,U]);teximage=fn('glTexImage2D',None,[U,I,I,I,I,I,U,U,P]);param=fn('glTexParameteri',None,[U,U,I]);active=fn('glActiveTexture',None,[U])
 def texture(data,depth=False):
  t=U();texgen(1,C.byref(t));texbind(0xDE1,t);buf=C.create_string_buffer(data)
  teximage(0xDE1,0,0x8CAC if depth else 0x8058,2,1,0,0x1902 if depth else 0x1908,0x1406 if depth else 0x1401,buf)
  for key,val in [(0x2801,0x2600),(0x2800,0x2600),(0x2802,0x812F),(0x2803,0x812F)]:param(0xDE1,key,val)
  return t
 normal=texture(bytes([255,0,0,255]*2));exterior=texture(bytes([0,255,0,255]*2));normaldepth=texture(struct.pack('ff',.25,.75),True);screendepth=texture(struct.pack('ff',.9,.9),True)
 maskcolor=texture(bytes([0]*8));outcolor=texture(bytes([0]*8));outdepth=texture(struct.pack('ff',1,1),True)
 fg=fn('glGenFramebuffers',None,[I,C.POINTER(U)]);fb=fn('glBindFramebuffer',None,[U,U]);ft=fn('glFramebufferTexture2D',None,[U,U,U,U,I]);status=fn('glCheckFramebufferStatus',U,[U])
 def framebuffer(color,depth):
  f=U();fg(1,C.byref(f));fb(0x8D40,f);ft(0x8D40,0x8CE0,0xDE1,color,0);ft(0x8D40,0x8D00,0xDE1,depth,0);assert status(0x8D40)==0x8CD5;return f
 mf=framebuffer(maskcolor,normaldepth);output=framebuffer(outcolor,outdepth)
 vg=fn('glGenVertexArrays',None,[I,C.POINTER(U)]);vb=fn('glBindVertexArray',None,[U]);bg=fn('glGenBuffers',None,[I,C.POINTER(U)]);bb=fn('glBindBuffer',None,[U,U]);bd=fn('glBufferData',None,[U,C.c_ssize_t,P,U]);attrib=fn('glVertexAttribPointer',None,[U,I,U,U,I,P]);enableattrib=fn('glEnableVertexAttribArray',None,[U])
 vao=U();vg(1,C.byref(vao));vb(vao);buffer=U();bg(1,C.byref(buffer));bb(0x8892,buffer)
 vertices=(F*20)(-1,-1,0,0,0,1,-1,0,1,0,-1,1,0,0,1,1,1,0,1,1);bd(0x8892,C.sizeof(vertices),vertices,0x88E4)
 attrib(0,3,0x1406,0,20,None);attrib(1,2,0x1406,0,20,C.c_void_p(12));enableattrib(0);enableattrib(1)
 viewport=fn('glViewport',None,[I,I,I,I]);enable=fn('glEnable',None,[U]);disable=fn('glDisable',None,[U]);depthmask=fn('glDepthMask',None,[U]);depthfunc=fn('glDepthFunc',None,[U]);draw=fn('glDrawArrays',None,[U,I,I]);read=fn('glReadPixels',None,[I,I,I,I,U,U,P])
 viewport(0,0,2,1);disable(0xBE2);disable(0xB44);enable(0xB71)
 # Screen surface is at depth .5: normal foreground .25 occludes it, .75 does not.
 fb(0x8D40,mf);depthmask(0);depthfunc(0x0203);use(maskprogram);draw(5,0,4)
 maskpixels=(C.c_ubyte*8)();read(0,0,2,1,0x1908,0x1401,maskpixels);assert list(maskpixels[:4])==[0,0,0,0] and maskpixels[4]==255 and abs(maskpixels[5]-191)<=1 and abs(maskpixels[6]-128)<=1 and maskpixels[7]==255,list(maskpixels)
 fb(0x8D40,output);depthmask(1);depthfunc(0x0207);use(composite)
 for i,(name,tex) in enumerate([(b'NormalSampler',normal),(b'ScreenSampler',exterior),(b'MaskSampler',maskcolor),(b'NormalDepth',normaldepth),(b'ScreenDepth',screendepth)]):
  active(0x84C0+i);texbind(0xDE1,tex);u1(uniform(composite,name),i)
 draw(5,0,4);pixels=(C.c_ubyte*8)();read(0,0,2,1,0x1908,0x1401,pixels)
 depth=(F*2)();read(0,0,2,1,0x1902,0x1406,depth)
 assert list(pixels)==[255,0,0,255,0,255,0,255],list(pixels)
 assert abs(depth[0]-.25)<1e-5 and abs(depth[1]-.9)<1e-5,list(depth)
 # Moving doors carry fractional opacity in the real surface-mask vertex color.
 vertexcolor=fn('glVertexAttrib4f',None,[U,F,F,F,F]);vertexcolor(2,1,1,1,0.5)
 fb(0x8D40,mf);depthmask(0);depthfunc(0x0203);use(maskprogram);draw(5,0,4)
 fb(0x8D40,output);depthmask(1);depthfunc(0x0207);use(composite);draw(5,0,4);read(0,0,2,1,0x1908,0x1401,pixels)
 assert list(pixels[:4])==[255,0,0,255] and abs(pixels[4]-127)<=1 and abs(pixels[5]-128)<=1,list(pixels)
 vertexcolor(2,1,1,1,1);fb(0x8D40,mf);depthmask(0);depthfunc(0x0203);use(maskprogram);draw(5,0,4)
 fb(0x8D40,output);depthmask(1);depthfunc(0x0207);use(composite)
 print('PASS: animated door opacity blends normal and screen scenes continuously; foreground stays unchanged')
 # Enable the actual screen filter and verify it never colors a foreground/door-hole pixel.
 ufloat=fn('glUniform1f',None,[I,F]);uvec4=fn('glUniform4f',None,[I,F,F,F,F]);uvec2=fn('glUniform2f',None,[I,F,F])
 ufloat(uniform(composite,b'FilterStrength'),0.8)
 uvec4(uniform(composite,b'ColorSettings'),1.03,1.06,0.02,1)
 uvec4(uniform(composite,b'SurfaceSettings'),0.12,0.08,0.10,0)
 uvec2(uniform(composite,b'SurfaceDensity'),96,64)
 draw(5,0,4);read(0,0,2,1,0x1908,0x1401,pixels);read(0,0,2,1,0x1902,0x1406,depth)
 assert list(pixels[:4])==[255,0,0,255],list(pixels)
 assert list(pixels[4:8])!=[0,255,0,255] and pixels[7]==255,list(pixels)
 assert abs(depth[0]-.25)<1e-5 and abs(depth[1]-.9)<1e-5,list(depth)
 ufloat(uniform(composite,b'FilterStrength'),0);draw(5,0,4);read(0,0,2,1,0x1908,0x1401,pixels)
 assert list(pixels)==[255,0,0,255,0,255,0,255],list(pixels)
 print('PASS: actual surface UV mask, screen-only filter, exact disable restoration and unchanged scene depth')
 # HUD and radar use the actual display mask without changing their geometry transforms.
 hud=makeprogram((root/'screen_hud.vsh').read_text(),(root/'screen_hud.fsh').read_text())
 use(hud)
 for name in [b'ModelViewMat',b'ProjMat']:um(uniform(hud,name),1,0,identity)
 uvec2h=fn('glUniform2f',None,[I,F,F]);uvec4h=fn('glUniform4f',None,[I,F,F,F,F]);colorattrib=fn('glVertexAttrib4f',None,[U,F,F,F,F]);disableattrib=fn('glDisableVertexAttribArray',None,[U])
 disableattrib(1);colorattrib(1,0,0,1,1);uvec4h(uniform(hud,b'ColorModulator'),1,1,1,1);uvec2h(uniform(hud,b'MaskSize'),2,1)
 active(0x84C0);texbind(0xDE1,maskcolor);u1(uniform(hud,b'MaskSampler'),0);disable(0xB71);draw(5,0,4)
 read(0,0,2,1,0x1908,0x1401,pixels);assert list(pixels)==[255,0,0,255,0,0,255,255],list(pixels)
 # HUD behind the composed screen/seam depth must still appear only on mask pixels.
 hud_far=list(identity);hud_far[14]=.98
 um(uniform(hud,b'ModelViewMat'),1,0,(F*16)(*hud_far));draw(5,0,4)
 read(0,0,2,1,0x1908,0x1401,pixels);read(0,0,2,1,0x1902,0x1406,depth)
 assert list(pixels)==[255,0,0,255,0,0,255,255],list(pixels)
 assert abs(depth[0]-.25)<1e-5 and abs(depth[1]-.9)<1e-5,list(depth)
 # Mask clipping keeps foreground/door-hole pixels unchanged even with depth disabled.
 enable(0xB71);enableattrib(1)
 print('PASS: actual HUD shader rejects non-display pixels and retains display pixels')
 # Reproduce a panel entirely between the eye and the near clip plane.
 nearcolor=texture(bytes([0]*8));nearframe=framebuffer(nearcolor,normaldepth)
 fb(0x8D40,nearframe);use(maskprogram);enable(0xB71);depthmask(0);depthfunc(0x0203)
 near_matrix=list(identity);near_matrix[14]=-1.2
 um(uniform(maskprogram,b'ModelViewMat'),1,0,(F*16)(*near_matrix))
 draw(5,0,4);read(0,0,2,1,0x1908,0x1401,maskpixels)
 assert list(maskpixels)==[0]*8,list(maskpixels)
 enable(0x864F);draw(5,0,4);disable(0x864F)
 read(0,0,2,1,0x1908,0x1401,maskpixels)
 assert maskpixels[0]==255 and maskpixels[4]==255,list(maskpixels)
 read(0,0,2,1,0x1902,0x1406,depth)
 assert abs(depth[0]-.25)<1e-5 and abs(depth[1]-.75)<1e-5,list(depth)
 um(uniform(maskprogram,b'ModelViewMat'),1,0,identity);enableattrib(1)
 print('PASS: near-wall screen mask survives near-plane clipping with foreground depth test and unchanged scene depth')
 # Photon normalizes jittered depth after final shading; Voxy depth uses another far plane.
 ufloat=fn('glUniform1f',None,[I,F]);uvec2=fn('glUniform2f',None,[I,F,F]);colormask=fn('glColorMask',None,[U,U,U,U])
 raw=texture(struct.pack('ff',1,.25),True)
 def projected(distance,near,far):
  a=(far+near)/(near-far);b=2*far*near/(near-far)
  return (-a+b/distance)*.5+.5
 combined=texture(struct.pack('ff',projected(10,.1,10000),1),True)
 fb(0x8D40,output);use(photon);depthmask(1);depthfunc(0x0207);colormask(0,0,0,0)
 for i,(name,tex) in enumerate([(b'SceneDepth',raw),(b'CombinedDepth',combined)]):
  active(0x84C0+i);texbind(0xDE1,tex);u1(uniform(photon,name),i)
 ufloat(uniform(photon,b'DepthScale'),1);uvec2(uniform(photon,b'DepthJitter'),0,0);u1(uniform(photon,b'HasLod'),1)
 uvec2(uniform(photon,b'CombinedProjection'),(10000+.1)/(.1-10000),2*10000*.1/(.1-10000))
 uvec2(uniform(photon,b'VanillaProjection'),(100+.1)/(.1-100),2*100*.1/(.1-100))
 draw(5,0,4);read(0,0,2,1,0x1902,0x1406,depth)
 expected=projected(10,.1,100)
 assert abs(depth[0]-expected)<2e-5 and abs(depth[1]-.25)<1e-5,list(depth)
 # A half-screen jitter shift samples the preceding raw pixel, including the clamped screen edge.
 uvec2(uniform(photon,b'DepthJitter'),-.5,0);draw(5,0,4);read(0,0,2,1,0x1902,0x1406,depth)
 assert all(abs(x-expected)<2e-5 for x in depth),list(depth)
 uvec2(uniform(photon,b'DepthJitter'),0,0);ufloat(uniform(photon,b'DepthScale'),.5);draw(5,0,4);read(0,0,2,1,0x1902,0x1406,depth)
 assert all(abs(x-expected)<2e-5 for x in depth),list(depth)
 colormask(1,1,1,1)
 # Real GPU history copies must preserve both float and packed Photon color formats.
 copyimage=fn('glCopyImageSubData',None,[U,U,I,I,I,I,U,U,I,I,I,I,I,I,I]);storage=fn('glTexStorage2D',None,[U,I,U,I,I]);getimage=fn('glGetTexImage',None,[U,I,U,U,P]);barrier=fn('glMemoryBarrier',None,[U])
 for internal,channels in [(0x822E,1),(0x881A,4),(0x8C3A,3)]:
  format_={1:0x1903,3:0x1907,4:0x1908}[channels];values=(F*(2*channels))(*([.25,.5,.75,1][:channels]*2))
  a=U();b=U();texgen(1,C.byref(a));texgen(1,C.byref(b));texbind(0xDE1,a);teximage(0xDE1,0,internal,2,1,0,format_,0x1406,values);param(0xDE1,0x2801,0x2600)
  texbind(0xDE1,b);storage(0xDE1,1,internal,2,1);param(0xDE1,0x2801,0x2600);barrier(0xFFFFFFFF)
  copyimage(a,0xDE1,0,0,0,0,b,0xDE1,0,0,0,0,2,1,1);copied=(F*(2*channels))();getimage(0xDE1,0,format_,0x1406,copied)
  assert all(abs(x-y)<.002 for x,y in zip(values,copied)),list(copied)
 # Exercise additional persistent resources used by general shader packs.
 pixelstore=fn('glPixelStorei',None,[U,I]);pixelstore(0x0CF5,1);pixelstore(0x0D05,1)
 storage3=fn('glTexStorage3D',None,[U,I,U,I,I,I]);sub3=fn('glTexSubImage3D',None,[U,I,I,I,I,I,I,I,U,U,P])
 for internal,format_,type_,data,ctype in [(0x881A,0x1908,0x1406,(F*32)(*[.25,.5,.75,1]*8),F),(0x8232,0x8D94,0x1401,(C.c_ubyte*8)(*range(8)),C.c_ubyte)]:
  a=U();b=U();texgen(1,C.byref(a));texgen(1,C.byref(b));texbind(0x806F,a);storage3(0x806F,1,internal,2,2,2);sub3(0x806F,0,0,0,0,2,2,2,format_,type_,data)
  texbind(0x806F,b);storage3(0x806F,1,internal,2,2,2);barrier(0xFFFFFFFF);copyimage(a,0x806F,0,0,0,0,b,0x806F,0,0,0,0,2,2,2)
  copied=(ctype*len(data))();getimage(0x806F,0,format_,type_,copied);assert all(abs(x-y)<.002 for x,y in zip(data,copied))
 copybuffer=fn('glCopyBufferSubData',None,[U,U,C.c_ssize_t,C.c_ssize_t,C.c_ssize_t]);getbuffer=fn('glGetBufferSubData',None,[U,C.c_ssize_t,C.c_ssize_t,P])
 a=U();b=U();bg(1,C.byref(a));bg(1,C.byref(b));data=(U*16)(*range(16));bb(0x8F36,a);bd(0x8F36,C.sizeof(data),data,0x88EA);bb(0x8F37,b);bd(0x8F37,C.sizeof(data),None,0x88EA)
 barrier(0xFFFFFFFF);copybuffer(0x8F36,0x8F37,0,0,C.sizeof(data));copied=(U*16)();getbuffer(0x8F37,0,C.sizeof(copied),copied);assert list(copied)==list(data)
 print('PASS: GPU persistent 3D RGBA16F / R8UI image and SSBO copies')
 print('PASS: actual Photon depth shader, Voxy far-plane conversion, jitter alignment/edge clamp; GPU history copies in R32F, RGBA16F and R11F_G11F_B10F')
 assert fn('glGetError',U,[])()==0
 print('PASS: actual GLSL shaders compile/link; foreground blocks screen mask; normal area retains color/depth; screen area selects exterior color/depth')
finally:
 gl.wglMakeCurrent(None,None)
 if context:gl.wglDeleteContext(context)
 user.ReleaseDC(window,dc);user.DestroyWindow(window)
