package dev.omnidisplay.client;
import dev.omnidisplay.region.ShaderResourceHistory.Resource;
import dev.omnidisplay.region.ShaderResourceHistory.Backend;
import org.lwjgl.opengl.*;

/** GPU copies preserve texture, copy-buffer and indexed SSBO bindings. */
public final class ShaderResources implements Backend {
 public static Resource image(int slot,int id,int target){
  int binding=binding(target),old=GL11.glGetInteger(binding);GL11.glBindTexture(target,id);
  try{
   int w=GL11.glGetTexLevelParameteri(target,0,GL11.GL_TEXTURE_WIDTH);
   int h=target==GL11.GL_TEXTURE_1D?1:GL11.glGetTexLevelParameteri(target,0,GL11.GL_TEXTURE_HEIGHT);
   int d=target==GL12.GL_TEXTURE_3D?GL11.glGetTexLevelParameteri(target,0,GL12.GL_TEXTURE_DEPTH):1;
   int f=GL11.glGetTexLevelParameteri(target,0,GL11.GL_TEXTURE_INTERNAL_FORMAT);
   check();if(w<=0||h<=0||d<=0)throw new IllegalStateException("Unallocated shader image");
   return new Resource(slot,id,target,w,h,d,f,0);
  }finally{GL11.glBindTexture(target,old);}
 }
 private static int binding(int target){return switch(target){
  case GL11.GL_TEXTURE_1D->GL11.GL_TEXTURE_BINDING_1D;
  case GL11.GL_TEXTURE_2D->GL11.GL_TEXTURE_BINDING_2D;
  case GL12.GL_TEXTURE_3D->GL12.GL_TEXTURE_BINDING_3D;
  case GL31.GL_TEXTURE_RECTANGLE->GL31.GL_TEXTURE_BINDING_RECTANGLE;
  default->throw new IllegalArgumentException("Unsupported shader image target: "+target);
 };}
 @Override public int create(Resource r){
  if(r.target()==0){
   int old=GL11.glGetInteger(GL31.GL_COPY_WRITE_BUFFER),id=GL15.glGenBuffers();
   try{GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER,id);GL15.glBufferData(GL31.GL_COPY_WRITE_BUFFER,r.bytes(),GL15.GL_DYNAMIC_COPY);check();return id;}
   catch(RuntimeException e){GL15.glDeleteBuffers(id);throw e;}
   finally{GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER,old);}
  }
  int old=GL11.glGetInteger(binding(r.target())),id=GL11.glGenTextures();
  try{
   GL11.glBindTexture(r.target(),id);
   if(r.target()==GL11.GL_TEXTURE_1D)GL42.glTexStorage1D(r.target(),1,r.format(),r.width());
   else if(r.target()==GL12.GL_TEXTURE_3D)GL42.glTexStorage3D(r.target(),1,r.format(),r.width(),r.height(),r.depth());
   else GL42.glTexStorage2D(r.target(),1,r.format(),r.width(),r.height());
   check();return id;
  }catch(RuntimeException e){GL11.glDeleteTextures(id);throw e;}
  finally{GL11.glBindTexture(r.target(),old);}
 }
 @Override public void copy(Resource r,int from,int to){
  GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
  if(r.target()==0){
   int read=GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER),write=GL11.glGetInteger(GL31.GL_COPY_WRITE_BUFFER);
   try{GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER,from);GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER,to);GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER,GL31.GL_COPY_WRITE_BUFFER,0,0,r.bytes());check();}
   finally{GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER,read);GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER,write);}
  }else{GL43.glCopyImageSubData(from,r.target(),0,0,0,0,to,r.target(),0,0,0,0,r.width(),r.height(),r.depth());check();}
  GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
 }
 @Override public void delete(Resource r,int id){if(r.target()==0)GL15.glDeleteBuffers(id);else GL11.glDeleteTextures(id);}
 private static void check(){int e=GL11.glGetError();if(e!=GL11.GL_NO_ERROR)throw new IllegalStateException("Shader resource copy GL error 0x"+Integer.toHexString(e));}
}
