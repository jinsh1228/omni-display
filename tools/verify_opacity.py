import ctypes as C, ctypes.util, os
from pathlib import Path
os.environ['EGL_PLATFORM']='surfaceless'
egl=C.CDLL(ctypes.util.find_library('EGL')); gl=C.CDLL(ctypes.util.find_library('GL'))
def fn(lib,name,restype,args):
 f=getattr(lib,name);f.restype=restype;f.argtypes=args;return f
I=C.c_int;U=C.c_uint;P=C.c_void_p;F=C.c_float
get=fn(egl,'eglGetDisplay',P,[P]);init=fn(egl,'eglInitialize',U,[P,C.POINTER(I),C.POINTER(I)]);choose=fn(egl,'eglChooseConfig',U,[P,C.POINTER(I),C.POINTER(P),I,C.POINTER(I)]);bind=fn(egl,'eglBindAPI',U,[U]);ctx=fn(egl,'eglCreateContext',P,[P,P,P,C.POINTER(I)]);surf=fn(egl,'eglCreatePbufferSurface',P,[P,P,C.POINTER(I)]);make=fn(egl,'eglMakeCurrent',U,[P,P,P,P])
d=get(None);a=I();b=I();assert init(d,C.byref(a),C.byref(b))
attrs=(I*13)(0x3033,1,0x3040,8,0x3024,8,0x3023,8,0x3022,8,0x3025,24,0x3038);config=P();n=I();assert choose(d,attrs,C.byref(config),1,C.byref(n)) and n.value
assert bind(0x30A2);context=ctx(d,config,None,(I*7)(0x3098,3,0x30FB,3,0x30FD,1,0x3038));surface=surf(d,config,(I*5)(0x3057,4,0x3056,1,0x3038));assert context and surface and make(d,surface,surface,context)
def g(name,r,args):return fn(gl,name,r,args)
create=g('glCreateShader',U,[U]);source=g('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),P]);compile_=g('glCompileShader',None,[U]);check=g('glGetShaderiv',None,[U,U,C.POINTER(I)]);log=g('glGetShaderInfoLog',None,[U,I,P,P]);cp=g('glCreateProgram',U,[]);attach=g('glAttachShader',None,[U,U]);link=g('glLinkProgram',None,[U]);pcheck=g('glGetProgramiv',None,[U,U,C.POINTER(I)]);use=g('glUseProgram',None,[U]);uniform=g('glGetUniformLocation',I,[U,C.c_char_p]);u1=g('glUniform1i',None,[I,I]);texgen=g('glGenTextures',None,[I,C.POINTER(U)]);texbind=g('glBindTexture',None,[U,U]);teximage=g('glTexImage2D',None,[U,I,I,I,I,I,U,U,P]);param=g('glTexParameteri',None,[U,U,I]);active=g('glActiveTexture',None,[U]);fg=g('glGenFramebuffers',None,[I,C.POINTER(U)]);fb=g('glBindFramebuffer',None,[U,U]);ft=g('glFramebufferTexture2D',None,[U,U,U,U,I]);fcheck=g('glCheckFramebufferStatus',U,[U]);vg=g('glGenVertexArrays',None,[I,C.POINTER(U)]);vb=g('glBindVertexArray',None,[U]);draw=g('glDrawArrays',None,[U,I,I]);vp=g('glViewport',None,[I,I,I,I]);read=g('glReadPixels',None,[I,I,I,I,U,U,P]);err=g('glGetError',U,[])

def shader(data,kind):
 s=create(kind);data=C.c_char_p(data.encode());source(s,1,C.byref(data),None);compile_(s);v=I();check(s,0x8B81,C.byref(v));assert v.value;return s
p=cp();attach(p,shader("#version 330 core\nvoid main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2.-1.,0.,1.);}",0x8B31));attach(p,shader("#version 330 core\nout vec4 color; uniform float opacity; void main(){color=vec4(1,0,0,opacity);}",0x8B30));link(p);v=I();pcheck(p,0x8B82,C.byref(v));assert v.value
vao=U();vg(1,C.byref(vao));vb(vao);vp(0,0,4,1)
enable=g('glEnable',None,[U]);mask=g('glDepthMask',None,[U]);func=g('glBlendFunc',None,[U,U]);cc=g('glClearColor',None,[F,F,F,F]);cd=g('glClearDepth',None,[C.c_double]);clear=g('glClear',None,[U]);uf=g('glUniform1f',None,[I,F])
cc(1,1,1,1);cd(.8);clear(0x4000|0x100);enable(0xB71);enable(0xBE2);func(0x302,0x303);mask(0);use(p);uf(uniform(p,b'opacity'),.10);draw(4,0,3)
colors=(F*16)();depth=(F*4)();read(0,0,4,1,0x1908,0x1406,colors);read(0,0,4,1,0x1902,0x1406,depth)
for i in range(4):
 assert abs(colors[i*4]-1)<.01 and abs(colors[i*4+1]-.9)<.01 and abs(colors[i*4+2]-.9)<.01,list(colors)
 assert abs(depth[i]-.8)<.01,list(depth)
assert err()==0
print('PASS: 10% color blending; background depth preserved (not a Minecraft integration test)')
