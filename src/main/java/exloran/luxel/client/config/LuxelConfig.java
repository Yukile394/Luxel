package exloran.luxel.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exloran.luxel.Luxel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Luxel ayarlari. {@code config/luxel.json} dosyasinda saklanir.
 * Cloth Config gibi harici bir kutuphaneye bagimli olmadan, sade bir
 * Gson serilestirmesi ile calisir; boylece mod tek basina (standalone) kalir.
 */
public final class LuxelConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("luxel.json");

	private static LuxelConfig instance;

	// --- Ayarlar ---
	public boolean modEnabled = true;
	public boolean dynamicLightsEnabled = true;
	public boolean otherPlayersLight = true;
	public boolean mobLight = false;
	public boolean droppedItemLight = true;
	public int maxLightDistance = 24;
	public int updateIntervalTicks = 2;
	public Quality lightQuality = Quality.HIGH;
	public boolean performanceMode = false;
	public boolean coloredLight = true;
	public boolean shaderCompatMode = false;
	public boolean debugMode = false;

	public enum Quality {
		LOW, MEDIUM, HIGH, ULTRA
	}

	public static LuxelConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static LuxelConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				LuxelConfig loaded = GSON.fromJson(reader, LuxelConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException e) {
				Luxel.LOGGER.warn("luxel.json okunamadi, varsayilan ayarlar kullanilacak.", e);
			}
		}
		LuxelConfig fresh = new LuxelConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Luxel.LOGGER.error("luxel.json kaydedilemedi.", e);
		}
	}
}
