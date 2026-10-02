package dev.omnidisplay.client;

import dev.omnidisplay.region.DualSceneHistory;
import org.lwjgl.opengl.*;

/** GPU-only history copies. Preserve raw GL bindings so Minecraft's binding cache stays valid. */
public final class PhotonTextures implements DualSceneHistory.Backend {
 public static DualSceneHistory.Texture describe(int slot,int id){
  int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
  try{
   GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
   var texture=new DualSceneHistory.Texture(slot,id,GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT),GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_INTERNAL_FORMAT));
   if(texture.width()<=0||texture.height()<=0)throw new IllegalStateException("Invalid Photon history texture "+slot);
   return texture;
  }finally{GL11.glBindTexture(GL11.GL_TEXTURE_2D,previous);}
 }
 @Override public int create(DualSceneHistory.Texture texture){
  int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),id=GL11.glGenTextures();
  try{
   GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
   GL42.glTexStorage2D(GL11.GL_TEXTURE_2D,1,texture.format(),texture.width(),texture.height());
   GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
   GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
   check();
   return id;
  }catch(RuntimeException error){GL11.glDeleteTextures(id);throw error;}
  finally{GL11.glBindTexture(GL11.GL_TEXTURE_2D,previous);}
 }
 @Override public void copy(int source,int destination,int width,int height){
  GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
  GL43.glCopyImageSubData(source,GL11.GL_TEXTURE_2D,0,0,0,0,destination,GL11.GL_TEXTURE_2D,0,0,0,0,width,height,1);
  check();
 }
 private static void check(){int error=GL11.glGetError();if(error!=GL11.GL_NO_ERROR)throw new IllegalStateException("Photon history OpenGL error 0x"+Integer.toHexString(error));}
 @Override public void delete(int texture){GL11.glDeleteTextures(texture);}
}
