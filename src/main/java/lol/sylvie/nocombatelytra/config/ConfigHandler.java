package lol.sylvie.nocombatelytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lol.sylvie.nocombatelytra.NoCombatElytra;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private ConfigRecord record = new ConfigRecord(30);

    public ConfigHandler(File file) {
        this.file = file;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson(record, writer);
        } catch (IOException e) {
            NoCombatElytra.LOGGER.error("IOException while trying to save config to disk!", e);
        }
    }

    public void load() {
        if (!file.exists()) this.save();

        try (FileReader reader = new FileReader(this.file)) {
            record = GSON.fromJson(reader, ConfigRecord.class);
        } catch (IOException e) {
            NoCombatElytra.LOGGER.error("IOException while trying to load config from disk!", e);
        } catch (RuntimeException e) {
            NoCombatElytra.LOGGER.error("RuntimeException while parsing config file. Is the file corrupted?", e);
        }
    }


    public ConfigRecord get() {
        return record;
    }
}
