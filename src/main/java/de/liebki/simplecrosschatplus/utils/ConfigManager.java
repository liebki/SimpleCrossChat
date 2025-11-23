package de.liebki.simplecrosschatplus.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final File file;
    private final FileConfiguration fileConfig;
    Plugin pluginInstance;

    public ConfigManager(String path, String fileName, Runnable callback, Plugin pluginInstance) {
        this.pluginInstance = pluginInstance;

        if (!fileName.contains(".yml")) {
            fileName = fileName + ".yml";
        }

        file = new File(path, fileName);
        fileConfig = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {
            fileConfig.options().copyDefaults(true);
            callback.run();

            try {
                fileConfig.save(file);
            } catch (IOException exception) {
                pluginInstance.getLogger().severe("Could not save config file, error: " + exception.getMessage());
            }

        }
    }

    public ConfigManager(String path, String fileName, Plugin pluginInstance) {
        if (!fileName.contains(".yml")) {
            fileName = fileName + ".yml";
        }

        file = new File(path, fileName);
        fileConfig = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {

            fileConfig.options().copyDefaults(true);
            try {
                fileConfig.save(file);
            } catch (IOException exception) {
                pluginInstance.getLogger().severe("Could not save config file, error: " + exception.getMessage());
            }

        }
    }

    public FileConfiguration getConfig() {
        return fileConfig;
    }

    public void saveConfig() {
        try {
            fileConfig.save(file);
        } catch (IOException exception) {
            pluginInstance.getLogger().severe("Could not save config file, error: " + exception.getMessage());
        }
    }

    public Boolean check(String path) {
        return fileConfig.get(path) != null;
    }

    public void set(String path, Object configValue) {
        fileConfig.set(path, configValue);
        saveConfig();
    }

    public <T> T get(String path) {
        if (fileConfig.getString(path) == null) {
            return null;
        }

        return (T) fileConfig.get(path);
    }

    public <T> T get(String path, T defaultValue) {
        if (fileConfig.getString(path) == null) {
            return defaultValue;
        }

        return (T) fileConfig.get(path);
    }
}