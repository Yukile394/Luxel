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
 * <p>
 * BURADA TANIMLI HER ALAN, {@code LightSourceManager} icinde gercekten okunur
 * ve motorun davranisini degistirir. Kullanilmayan ("dummy") ayar yoktur.
 * <p>
 * {@code shaderCompatMode} bilerek kaldirilmistir: gercek bir Iris/Oculus
 * entegrasyonu olmadan bu ayarin arkasinda anlamli bir davranis olusturulamaz,
 * bu yuzden sahte bir ayar birakmak yerine tamamen cikarildi.
 */
public final class LuxelConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("luxel.json");

	private static LuxelConfig instance;

	// --- Ayarlar (hepsi LightSourceManager tarafindan gercekten kullanilir) ---
	public boolean modEnabled = true;
	public boolean dynamicLightsEnabled = true;
	public boolean otherPlayersLight = true;
	public boolean mobLight = false;
	public boolean droppedItemLight = true;
	public boolean coloredLight = true;
	public boolean performanceMode = false;
	public boolean debugMode = false;
	public Quality lightQuality = Quality.HIGH;
	public int maxLightDistance = 24;
	public int updateIntervalTicks = 2;

	public enum Quality {
		LOW, MEDIUM, HIGH, ULTRA
	}

	public static LuxelConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	/**
	 * Diskteki dosyayi yeniden okuyup mevcut singleton'a uygular. Boylece
	 * elde tutulan tum referanslar (LightSourceManager, config ekrani)
	 * ayni obje uzerinden gunceli gorur.
	 */
	public static LuxelConfig reload() {
		LuxelConfig fresh = load();
		if (instance == null) {
			instance = fresh;
		} else {
			instance.copyFrom(fresh);
		}
		return instance;
	}

	private void copyFrom(LuxelConfig other) {
		this.modEnabled = other.modEnabled;
		this.dynamicLightsEnabled = other.dynamicLightsEnabled;
		this.otherPlayersLight = other.otherPlayersLight;
		this.mobLight = other.mobLight;
		this.droppedItemLight = other.droppedItemLight;
		this.coloredLight = other.coloredLight;
		this.performanceMode = other.performanceMode;
		this.debugMode = other.debugMode;
		this.lightQuality = other.lightQuality;
		this.maxLightDistance = other.maxLightDistance;
		this.updateIntervalTicks = other.updateIntervalTicks;
	}

	private static LuxelConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				LuxelConfig loaded = GSON.fromJson(reader, LuxelConfig.class);
				if (loaded != null) {
					loaded.sanitize();
					return loaded;
				}
			} catch (IOException | com.google.gson.JsonSyntaxException e) {
				Luxel.LOGGER.warn("luxel.json okunamadi, varsayilan ayarlar kullanilacak.", e);
			}
		}
		LuxelConfig fresh = new LuxelConfig();
		fresh.save();
		return fresh;
	}

	/**
	 * Bozuk veya elle duzenlenirken hatali girilmis degerleri guvenli araliga ceker.
	 * Boylece kullanicinin json'u elle degistirmesi motoru bozmaz.
	 */
	private void sanitize() {
		if (lightQuality == null) {
			lightQuality = Quality.HIGH;
		}
		if (maxLightDistance < 2) {
			maxLightDistance = 2;
		}
		if (maxLightDistance > 64) {
			maxLightDistance = 64;
		}
		if (updateIntervalTicks < 1) {
			updateIntervalTicks = 1;
		}
		if (updateIntervalTicks > 20) {
			updateIntervalTicks = 20;
		}
	}

	public void save() {
		sanitize();
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
