package io.github.kosmx.emotes.legacy;

import io.github.kosmx.emotes.common.network.*;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/** Original Emotecraft wire format on the legacy Forge custom-payload channel. */
public final class LegacyNetwork {
    public static final int LIMIT = 32767;
    public static final String CHANNEL = "emotecraft:emote";
    private FMLEventChannel channel;
    private final Map<UUID, EntityPlayerMP> clients = new HashMap<>();
    private final Map<UUID, Playing> playing = new HashMap<>();
    private final Map<UUID, Long> lastPacket = new HashMap<>();
    public void init() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
        channel.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }
    public void clear() { clients.clear(); playing.clear(); lastPacket.clear(); }
    public static byte[] encode(EmotePacket.Builder builder) throws IOException {
        ByteBuffer buffer = builder.build(LIMIT).write();
        byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); return bytes;
    }
    private FMLProxyPacket packet(byte[] bytes) {
        return new FMLProxyPacket(new net.minecraft.network.PacketBuffer(Unpooled.wrappedBuffer(bytes)), CHANNEL);
    }
    public void sendToServer(EmotePacket.Builder builder) throws IOException {
        channel.sendToServer(packet(encode(builder)));
    }
    private void send(EntityPlayerMP player, EmotePacket.Builder builder) {
        try { channel.sendTo(packet(encode(builder)), player); }
        catch (IOException ex) { Emotecraft.log.warn("Emote too large for the legacy channel", ex); }
    }
    private EmotePacket.Builder config() {
        EmotePacket.Builder builder = new EmotePacket.Builder().configureToConfigExchange(true);
        NetData data = builder.copyAndGetData();
        data.versions.put(PacketConfig.SERVER_TRACK_EMOTE_PLAY, (byte) 1);
        data.versions.put(PacketConfig.ALLOW_EMOTE_STREAM, (byte) 0);
        data.versions.put(PacketConfig.ALLOW_EMOTE_SYNC, (byte) -1);
        return new EmotePacket.Builder(data);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) send((EntityPlayerMP) event.player, config());
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        Playing old = playing.remove(id);
        if (old != null) broadcast(new EmotePacket.Builder().configureToSendStop(old.data.emoteData.getUuid(), id), id);
        clients.remove(id); lastPacket.remove(id);
    }
    @SubscribeEvent public void tracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP recipient = (EntityPlayerMP) event.getEntityPlayer();
        if (!clients.containsKey(recipient.getUniqueID())) return;
        Playing state = playing.get(event.getTarget().getUniqueID());
        if (state != null) replay(recipient, state);
    }
    private void replay(EntityPlayerMP recipient, Playing state) {
        int tick = state.data.tick + (int) ((System.nanoTime() - state.started) / 50000000L);
        if (state.data.emoteData.isInfinite() || tick < state.data.emoteData.stopTick) {
            NetData copy = state.data.copy(); copy.tick = tick;
            send(recipient, new EmotePacket.Builder(copy));
        }
    }
    @SubscribeEvent public void client(FMLNetworkEvent.ClientCustomPacketEvent event) {
        byte[] bytes = copy(event.getPacket());
        if (bytes != null) Emotecraft.proxy.receive(bytes);
    }
    @SubscribeEvent public void server(FMLNetworkEvent.ServerCustomPacketEvent event) {
        byte[] bytes = copy(event.getPacket());
        if (bytes == null || !(event.getHandler() instanceof NetHandlerPlayServer)) return;
        EntityPlayerMP sender = ((NetHandlerPlayServer) event.getHandler()).player;
        sender.getServerWorld().addScheduledTask(() -> handle(sender, bytes));
    }
    private byte[] copy(FMLProxyPacket packet) {
        int length = packet.payload().readableBytes();
        if (length < 6 || length > LIMIT) return null;
        byte[] bytes = new byte[length];
        packet.payload().getBytes(packet.payload().readerIndex(), bytes); return bytes;
    }
    private void handle(EntityPlayerMP sender, byte[] bytes) {
        UUID id = sender.getUniqueID();
        long now = System.nanoTime();
        try {
            NetData data = new EmotePacket.Builder().build().read(ByteBuffer.wrap(bytes));
            if (data == null) return;
            if (data.purpose == PacketTask.CONFIG) {
                boolean first = clients.put(id, sender) == null;
                if (first) { send(sender, config()); for (Playing state : playing.values()) replay(sender, state); }
                return;
            }
            if (data.purpose != PacketTask.STREAM && data.purpose != PacketTask.STOP) return;
            // Never trust a UUID or forced flag supplied by a client.
            data.player = id; data.isForced = false;
            if (data.purpose == PacketTask.STREAM) {
                Long previous = lastPacket.put(id, now);
                if (previous != null && now - previous < 50000000L) return;
                if (data.tick < 0 || data.emoteData.getLength() > 72000) return;
                playing.put(id, new Playing(data));
            } else {
                Playing active = playing.get(id);
                if (active != null && active.data.emoteData.getUuid().equals(data.stopEmoteID)) playing.remove(id);
            }
            clients.put(id, sender);
            broadcast(new EmotePacket.Builder(data), id);
        } catch (IOException | RuntimeException ex) { Emotecraft.log.debug("Rejected invalid emote packet", ex); }
    }
    private void broadcast(EmotePacket.Builder builder, UUID except) {
        for (EntityPlayerMP player : clients.values()) if (!player.getUniqueID().equals(except)) send(player, builder.copy());
    }
    private static final class Playing {
        final NetData data; final long started = System.nanoTime();
        Playing(NetData data) { this.data = data; }
    }
}
