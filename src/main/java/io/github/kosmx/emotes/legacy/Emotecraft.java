package io.github.kosmx.emotes.legacy;

import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.executor.Logger;
import io.github.kosmx.emotes.server.config.Serializer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import java.nio.file.Path;
import java.util.logging.Level;

@Mod(modid = "emotecraft", name = "Emotecraft (Unofficial Port) (1.12.2)", version = "2.4.12-backport.4",
     guiFactory = "io.github.kosmx.emotes.legacy.client.LegacyGuiFactory",
     acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*",
     dependencies = "required-after:forge@[14.23.5.2859,)")
public class Emotecraft {
    @SidedProxy(clientSide = "io.github.kosmx.emotes.legacy.client.ClientProxy",
                serverSide = "io.github.kosmx.emotes.legacy.CommonProxy")
    public static CommonProxy proxy;
    public static org.apache.logging.log4j.Logger log;
    public static final LegacyNetwork NETWORK = new LegacyNetwork();

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        final Path root = event.getModConfigurationDirectory().getParentFile().toPath();
        EmoteInstance.instance = new EmoteInstance() {
            public Path getGameDirectory() { return root; }
            public boolean isClient() { return event.getSide().isClient(); }
            public Logger getLogger() { return new Logger() {
                public void writeLog(Level level, String msg) { log.info(msg); }
                public void writeLog(Level level, String msg, Throwable error) { log.warn(msg, error); }
            }; }
        };
        Serializer.INSTANCE = new Serializer();
        EmoteInstance.config = Serializer.getConfig();
        NETWORK.init();
        proxy.init();
    }
    @Mod.EventHandler public void stopped(FMLServerStoppedEvent event) { NETWORK.clear(); }
}
