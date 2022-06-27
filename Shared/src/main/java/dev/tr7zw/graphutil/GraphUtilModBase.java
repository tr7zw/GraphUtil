package dev.tr7zw.graphutil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.tr7zw.config.CustomConfigScreen;
import net.minecraft.client.Option;
import net.minecraft.client.gui.screens.Screen;

public abstract class GraphUtilModBase {

    public static GraphUtilModBase instance;

    public Config config;
    private final File settingsFile = new File("config", "graphutil.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void onInitialize() {
		instance = this;
        if (settingsFile.exists()) {
            try {
                config = gson.fromJson(new String(Files.readAllBytes(settingsFile.toPath()), StandardCharsets.UTF_8),
                        Config.class);
            } catch (Exception ex) {
                System.out.println("Error while loading config! Creating a new one!");
                ex.printStackTrace();
            }
        }
        if (config == null) {
            config = new Config();
            writeConfig();
        } else {
            if(ConfigUpgrader.upgradeConfig(config)) {
                writeConfig(); // Config got modified
            }
        }
		initModloader();
	}
	
    public void writeConfig() {
        if (settingsFile.exists())
            settingsFile.delete();
        try {
            Files.write(settingsFile.toPath(), gson.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    
    public abstract void initModloader();

    public Screen createConfigScreen(Screen parent) {
        CustomConfigScreen screen = new CustomConfigScreen(parent, "text.graphutil.title") {

            @Override
            public void initialize() {
                List<Option> options = new ArrayList<>();
                options.add(getOnOffOption("text.graphutil.vanillaScale", () -> config.vanillaScale,
                        (b) -> config.vanillaScale = b));
                options.add(getOnOffOption("text.graphutil.alwaysShowGraph", () -> config.alwaysShowGraph,
                        (b) -> config.alwaysShowGraph = b));
              
                getOptions().addSmall(options.toArray(new Option[0]));

            }

            @Override
            public void save() {
                writeConfig();
            }

            @Override
            public void reset() {
                config = new Config();
                writeConfig();
            }

        };

        return screen;
    }

}
