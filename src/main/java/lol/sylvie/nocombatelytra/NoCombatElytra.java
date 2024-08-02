package lol.sylvie.nocombatelytra;

import lol.sylvie.nocombatelytra.config.ConfigHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class NoCombatElytra implements ModInitializer {
    public static Logger LOGGER = LoggerFactory.getLogger("NoCombatElytra");
    public static ConfigHandler CONFIG;

    @Override
    public void onInitialize() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("nocombatelytra.json").toFile();
        CONFIG = new ConfigHandler(configFile);
        CONFIG.load();
    }
}
