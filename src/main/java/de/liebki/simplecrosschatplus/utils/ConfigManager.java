package de.liebki.simplecrosschatplus.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

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

    public void setWithComment(String path, Object configValue, String... comments) {
        fileConfig.set(path, configValue);
        saveConfig();
        injectCommentAbovePath(path, comments);
    }

    private void injectCommentAbovePath(String path, String... comments) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();

            String searchKey = path.substring(path.lastIndexOf('.') + 1) + ":";
            boolean commentAdded = false;

            for (String line : lines) {
                if (!commentAdded && line.trim().startsWith(searchKey)) {
                    for (String comment : comments) {
                        newLines.add("# " + comment);
                    }
                    commentAdded = true;
                }
                newLines.add(line);
            }

            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            pluginInstance.getLogger().warning("Could not inject comments: " + e.getMessage());
        }
    }

    public <T> T get(String path) {
        if (fileConfig.get(path) == null) {
            return null;
        }

        return (T) fileConfig.get(path);
    }

    public <T> T get(String path, T defaultValue) {
        if (fileConfig.get(path) == null) {
            return defaultValue;
        }

        return (T) fileConfig.get(path);
    }
}