package com.littlesquad.dungeon.internal.file;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FileManager {
    private static volatile ConfigParser config;
    private static volatile FileConfiguration messages;
    private static final Map<String, DungeonParser> dungeons = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

    private static File pluginFolder;

    private FileManager () {}

    public static CompletableFuture<Void> loadAll (final File dir) {
        if (pluginFolder == null) {
            //noinspection ResultOfMethodCallIgnored
            (pluginFolder = dir).mkdirs();
            //Load dynamically directly from the jar if they don't exist...
            File f;
            if (!(f = new File(pluginFolder, "config.yml")).exists())
                try (final InputStream stream = FileManager.class.getResourceAsStream("/config.yml");
                     final FileOutputStream out = new FileOutputStream(f)) {
                    if (stream != null)
                        out.write(stream.readAllBytes());
                    out.flush();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            if (!(f = new File(pluginFolder, "messages.yml")).exists())
                try (final InputStream stream = FileManager.class.getResourceAsStream("/messages.yml");
                     final FileOutputStream out = new FileOutputStream(f)) {
                    if (stream != null)
                        out.write(stream.readAllBytes());
                    out.flush();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            if (!(f = new File(pluginFolder, "dungeons")).exists()
                    && f.mkdirs()
                    && !(f = new File(f, "Example.yml")).exists())
                try (final InputStream stream = FileManager.class.getResourceAsStream("/dungeons/Example.yml");
                     final FileOutputStream out = new FileOutputStream(f)) {
                    if (stream != null)
                        out.write(stream.readAllBytes());
                    out.flush();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
        }
        //Load in parallel the configurations and let the caller wait on it...
        final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(2);
        futures.add(CompletableFuture.runAsync(() -> config = new ConfigParser(
                        YamlConfiguration.loadConfiguration(new File(
                                pluginFolder,
                                "config.yml"))),
                executor));
        futures.add(CompletableFuture.runAsync(() -> messages = YamlConfiguration
                        .loadConfiguration(new File(
                                pluginFolder,
                                "messages.yml")),
                executor));
        final File[] dungeonFiles;
        if ((dungeonFiles = new File(
                pluginFolder,
                "dungeons")
                .listFiles()) != null) {
            dungeons.clear();
            for (int i = 0; i < dungeonFiles.length; i++) {
                final int finalI = i;
                futures.add(CompletableFuture.runAsync(() -> {
                    final String fileName;
                    final String name;
                    dungeons.put(
                            name = (fileName = dungeonFiles[finalI]
                                    .getName())
                                    .substring(0, fileName.lastIndexOf('.')),
                            new DungeonParser(
                                    name,
                                    YamlConfiguration.loadConfiguration(dungeonFiles[finalI])));
                    }, executor));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public static ConfigParser getConfig () {
        return config;
    }
    public static FileConfiguration getMessages () {
        return messages;
    }
    public static Map<String, DungeonParser> getDungeons () {
        return dungeons;
    }

    public static void close () {
        executor.shutdownNow();
    }
}
