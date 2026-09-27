package io.github.kosmx.emotes.legacy.client;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.opennbs.NBS;
import dev.kosmx.playerAnim.core.data.opennbs.SoundPlayer;
import dev.kosmx.playerAnim.core.util.UUIDMap;
import io.github.kosmx.emotes.common.network.*;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.github.kosmx.emotes.api.events.client.ClientEmoteAPI;
import io.github.kosmx.emotes.api.events.client.ClientEmoteEvents;
import dev.kosmx.playerAnim.core.impl.event.EventResult;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.legacy.*;
import io.github.kosmx.emotes.server.serializer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

public final class ClientProxy extends CommonProxy {
    public static ClientProxy INSTANCE;
    public final List<KeyframeAnimation> library = new ArrayList<>();
    public final Map<UUID, Playback> playing = new HashMap<>();
    public final UUID[] slots = new UUID[8];
    public int wheelPage;
    private final Properties slotPages = new Properties();
    private final Set<Integer> pressedKeys = new HashSet<>();
    public final KeyBinding wheel = new KeyBinding("key.emotecraft.wheel", Keyboard.KEY_B, "Emotecraft");
    private final KeyBinding menu = new KeyBinding("key.emotecraft.menu", Keyboard.KEY_N, "Emotecraft");
    private final KeyBinding stop = new KeyBinding("key.emotecraft.stop", Keyboard.KEY_G, "Emotecraft");
    private final KeyBinding[] shortcuts = new KeyBinding[8];
    private boolean connected;
    private int previousCamera = -1;
    private LegacyPlayerRenderer normal, slim;
    private HashMap<Byte, Byte> serverVersions;
    private static final SoundEvent[] NOTES = {SoundEvents.BLOCK_NOTE_HARP, SoundEvents.BLOCK_NOTE_BASS,
        SoundEvents.BLOCK_NOTE_BASEDRUM, SoundEvents.BLOCK_NOTE_SNARE, SoundEvents.BLOCK_NOTE_HAT,
        SoundEvents.BLOCK_NOTE_GUITAR, SoundEvents.BLOCK_NOTE_FLUTE, SoundEvents.BLOCK_NOTE_BELL,
        SoundEvents.BLOCK_NOTE_CHIME, SoundEvents.BLOCK_NOTE_XYLOPHONE};

    @Override public void init() {
        INSTANCE = this;
        MenuSettings.load();
        new LegacyClientAPI();
        ClientRegistry.registerKeyBinding(wheel); ClientRegistry.registerKeyBinding(menu); ClientRegistry.registerKeyBinding(stop);
        for (int i = 0; i < 8; i++) {
            shortcuts[i] = new KeyBinding("key.emotecraft.slot" + (i + 1), Keyboard.KEY_NONE, "Emotecraft");
            ClientRegistry.registerKeyBinding(shortcuts[i]);
        }
        reload(); loadSlots();
        MinecraftForge.EVENT_BUS.register(this);
    }
    public void reload() {
        EmoteGraphics.clear();
        UUIDMap<KeyframeAnimation> loaded = new UUIDMap<>();
        if (EmoteInstance.config.loadBuiltinEmotes.get()) {
            for (String name : new String[]{"waving", "clap", "crying", "point", "here", "palm", "backflip", "roblox_potion_dance", "kazotsky_kick"}) {
                try (InputStream in = getClass().getResourceAsStream("/assets/emotecraft/emotes/" + name + ".json")) {
                    if (in != null) {
                        List<KeyframeAnimation> emotes=UniversalEmoteSerializer.readData(in,name+".json");
                        try(InputStream icon=getClass().getResourceAsStream("/assets/emotecraft/emotes/"+name+".png")) {
                            if(icon!=null)for(KeyframeAnimation emote:emotes)emote.extraData.put("iconData",dev.kosmx.playerAnim.core.util.MathHelper.readFromIStream(icon));
                        }
                        loaded.addAll(emotes);
                    }
                } catch (Exception ex) { Emotecraft.log.warn("Cannot load bundled emote " + name, ex); }
            }
        }
        File directory = EmoteInstance.instance.getExternalEmoteDir(); directory.mkdirs();
        EmoteSerializer.serializeEmotes(loaded, directory.toPath());
        library.clear(); library.addAll(loaded.values());
        library.sort(Comparator.comparing(ClientProxy::name, String.CASE_INSENSITIVE_ORDER));
    }
    public static String name(KeyframeAnimation animation) {
        String raw = String.valueOf(animation.extraData.getOrDefault("name", "Emote"));
        try {
            net.minecraft.util.text.ITextComponent text = net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(raw);
            if (text != null) return text.getUnformattedText();
        } catch (RuntimeException ignored) { }
        return raw;
    }
    public KeyframeAnimation find(UUID id) {
        if (id != null) for (KeyframeAnimation animation : library) if (id.equals(animation.getUuid())) return animation;
        return null;
    }
    private File bindings() { return EmoteInstance.instance.getGameDirectory().resolve("config/emotecraft-wheel.properties").toFile(); }
    private void loadSlots() {
        Properties properties = slotPages;
        if (bindings().isFile()) {
            try (InputStream in = new FileInputStream(bindings())) { properties.load(in); }
            catch (IOException ex) { Emotecraft.log.warn("Cannot read wheel bindings", ex); }
        } else {
            for (int i = 0; i < slots.length && i < library.size(); i++) slots[i] = library.get(i).getUuid();
            saveSlots(); return;
        }
        for (int i = 0; i < 8; i++) {
            try { slots[i] = UUID.fromString(properties.getProperty("slot" + i, "")); }
            catch (IllegalArgumentException ignored) { slots[i] = null; }
        }
    }
    public void saveSlots() {
        Properties properties = slotPages;
        for (int i = 0; i < 8; i++) {
            String key=(wheelPage==0?"":"page"+wheelPage+".")+"slot"+i;
            if(slots[i]!=null)properties.setProperty(key,slots[i].toString());else properties.remove(key);
        }
        try (OutputStream out = new FileOutputStream(bindings())) { properties.store(out, "Emotecraft wheel"); }
        catch (IOException ex) { Emotecraft.log.warn("Cannot save wheel bindings", ex); }
    }
    public void changePage(int delta) {
        saveSlots();wheelPage=(wheelPage+delta+3)%3;
        for(int i=0;i<8;i++)try { slots[i]=UUID.fromString(slotPages.getProperty((wheelPage==0?"":"page"+wheelPage+".")+"slot"+i,"")); }
        catch(IllegalArgumentException ex){slots[i]=null;}
    }
    public void play(KeyframeAnimation animation) {
        playAt(animation, 0);
    }
    private boolean playAt(KeyframeAnimation animation, int tick) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || tick < 0) return false;
        if (animation == null) { stop(); return true; }
        if (ClientEmoteEvents.EMOTE_VERIFICATION.invoker().verify(animation, mc.player.getUniqueID()) == EventResult.FAIL) return false;
        playing.put(mc.player.getUniqueID(), new Playback(animation, tick, mc.player.getUniqueID()));
        ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(animation, mc.player.getUniqueID());
        if (MenuSettings.get("perspective") && previousCamera < 0 && mc.gameSettings.thirdPersonView == 0) {
            previousCamera = 0; mc.gameSettings.thirdPersonView = MenuSettings.get("front")?2:1;
        }
        if (connected) {
            try { Emotecraft.NETWORK.sendToServer(new EmotePacket.Builder().configureToStreamEmote(animation).configureEmoteTick(tick).setVersion(serverVersions)); }
            catch (IOException ex) { message("Эмоция играет локально: превышен сетевой лимит 1.12.2 (32 КБ)."); }
        }
        return true;
    }
    public void stop() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        Playback old = playing.remove(mc.player.getUniqueID());
        restoreCamera();
        if (old != null) {
            ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(old.animation.getData().getUuid(), mc.player.getUniqueID());
            ClientEmoteEvents.LOCAL_EMOTE_STOP.invoker().onEmoteStop();
        }
        if (old != null && connected) try {
            Emotecraft.NETWORK.sendToServer(new EmotePacket.Builder().configureToSendStop(old.animation.getData().getUuid()));
        } catch (IOException ex) { Emotecraft.log.warn("Cannot send emote stop", ex); }
    }
    private void restoreCamera() {
        Minecraft mc = Minecraft.getMinecraft();
        if (previousCamera >= 0 && mc.gameSettings.thirdPersonView != 0) mc.gameSettings.thirdPersonView = previousCamera;
        previousCamera = -1;
    }
    public void message(String text) {
        if (Minecraft.getMinecraft().player != null) Minecraft.getMinecraft().player.sendMessage(new TextComponentString("[Emotecraft] " + text));
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;
        if (mc.currentScreen == null) {
            for(Map.Entry<UUID,Integer> binding:MenuSettings.keys.entrySet()) {
                int key=binding.getValue();
                if(key>0 && key<Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key)) { if(pressedKeys.add(key))play(find(binding.getKey())); }
                else pressedKeys.remove(key);
            }
            if (wheel.isPressed()) mc.displayGuiScreen(new EmoteWheel());
            else if (menu.isPressed()) mc.displayGuiScreen(new EmoteMenu());
            if (stop.isPressed()) stop();
            for (int i = 0; i < shortcuts.length; i++) if (shortcuts[i].isPressed()) play(find(slots[i]));
        }
        if (mc.isGamePaused()) return;
        if (playing.containsKey(mc.player.getUniqueID()) &&
            ((MenuSettings.get("stopOnMove") && (mc.player.movementInput.moveForward != 0 || mc.player.movementInput.moveStrafe != 0 ||
             mc.player.movementInput.jump || mc.player.movementInput.sneak)) || mc.player.hurtTime > 0 || !mc.player.isEntityAlive())) stop();
        Iterator<Map.Entry<UUID, Playback>> iterator = playing.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Playback> entry = iterator.next();
            Playback playback = entry.getValue(); playback.animation.tick();
            if (playback.song != null && playback.animation.isActive()) playback.song.tick();
            if (!playback.animation.isActive()) {
                iterator.remove(); if (entry.getKey().equals(mc.player.getUniqueID())) restoreCamera();
            }
        }
    }
    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            connected = false; serverVersions = null; playing.clear(); restoreCamera();
        });
    }
    @Override public void receive(byte[] bytes) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                NetData data = new EmotePacket.Builder().build().read(ByteBuffer.wrap(bytes));
                if (data == null) return;
                if (data.purpose == PacketTask.CONFIG) {
                    boolean first = !connected; connected = true; serverVersions = data.versions;
                    if (first) Emotecraft.NETWORK.sendToServer(new EmotePacket.Builder().configureToConfigExchange(true));
                } else if (data.player != null && data.purpose == PacketTask.STREAM && data.tick >= 0) {
                    if (ClientEmoteEvents.EMOTE_VERIFICATION.invoker().verify(data.emoteData, data.player) == EventResult.FAIL) return;
                    playing.put(data.player, new Playback(data.emoteData, data.tick, data.player));
                    ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(data.emoteData, data.player);
                } else if (data.player != null && data.purpose == PacketTask.STOP) {
                    Playback active = playing.get(data.player);
                    if (active != null && active.animation.getData().getUuid().equals(data.stopEmoteID)) {
                        playing.remove(data.player);
                        ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(data.stopEmoteID, data.player);
                        if (Minecraft.getMinecraft().player != null && data.player.equals(Minecraft.getMinecraft().player.getUniqueID())) restoreCamera();
                    }
                }
            } catch (IOException | RuntimeException ex) { Emotecraft.log.warn("Cannot read emote packet", ex); }
        });
    }
    @SubscribeEvent public void render(RenderPlayerEvent.Pre event) {
        if(!MenuSettings.get("otherPlayers") && event.getEntityPlayer()!=Minecraft.getMinecraft().player)return;
        if (event.getRenderer() instanceof LegacyPlayerRenderer || !(event.getEntityPlayer() instanceof AbstractClientPlayer)) return;
        Playback playback = playing.get(event.getEntityPlayer().getUniqueID());
        if (playback == null || !playback.animation.isActive()) return;
        if (normal == null) {
            normal = new LegacyPlayerRenderer(Minecraft.getMinecraft().getRenderManager(), false);
            slim = new LegacyPlayerRenderer(Minecraft.getMinecraft().getRenderManager(), true);
        }
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntityPlayer();
        event.setCanceled(true);
        ("slim".equals(player.getSkinType()) ? slim : normal).doRender(player, event.getX(), event.getY(), event.getZ(), player.rotationYaw, event.getPartialRenderTick());
    }
    public static final class Playback {
        public final KeyframeAnimationPlayer animation;
        final SoundPlayer song;
        Playback(KeyframeAnimation data, int tick, UUID owner) {
            animation = new KeyframeAnimationPlayer(data, tick);
            Object nbs = data.extraData.get("song");
            song = nbs instanceof NBS ? new SoundPlayer((NBS) nbs, note -> {
                if(!MenuSettings.get("music"))return;
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.world == null) return;
                net.minecraft.entity.player.EntityPlayer player = mc.world.getPlayerEntityByUUID(owner);
                if (player != null) mc.world.playSound(player.posX, player.posY, player.posZ,
                    NOTES[note.instrument >= 0 && note.instrument < NOTES.length ? note.instrument : 0],
                    SoundCategory.PLAYERS, note.getVolume(), note.getPitch(), false);
            }, tick) : null;
        }
    }
    private static final class LegacyClientAPI extends ClientEmoteAPI {
        LegacyClientAPI() { ClientEmoteAPI.INSTANCE = this; }
        @Override protected boolean playEmoteImpl(KeyframeAnimation animation, int tick) { return ClientProxy.INSTANCE.playAt(animation, tick); }
        @Override protected Collection<KeyframeAnimation> clientEmoteListImpl() { return Collections.unmodifiableList(ClientProxy.INSTANCE.library); }
    }
}
