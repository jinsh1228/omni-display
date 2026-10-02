package dev.omnidisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class RenderPackets {
 public record State(CompoundTag tag) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"render_state"));
  public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,State::tag,State::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Request(boolean enabled) implements CustomPacketPayload {
  public static final Type<Request> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"render_request"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Request::enabled,Request::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record FilterRequest(String key,int value) implements CustomPacketPayload {
  public static final Type<FilterRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"filter_request"));
  public static final StreamCodec<RegistryFriendlyByteBuf,FilterRequest> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,FilterRequest::key,ByteBufCodecs.VAR_INT,FilterRequest::value,FilterRequest::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("1.1");
  r.playToClient(State.TYPE,State.CODEC,(p,c)->c.enqueueWork(()->dev.omnidisplay.client.ServerRenderState.receive(p.tag())));
  r.playToServer(FilterRequest.TYPE,FilterRequest.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer sp)RenderControl.filterRequest(sp,p.key(),p.value());}));
  r.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer sp)RenderControl.request(sp,p.enabled());}));
 }
}
