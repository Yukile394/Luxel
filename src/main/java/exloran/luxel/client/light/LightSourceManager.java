package exloran.luxel.client.light;

import exloran.luxel.Luxel;
import exloran.luxel.client.config.LuxelConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Aktif dinamik isik kaynaklarini takip eden ve vanilla light-engine'e
 * her tick ihtiyac duyulan kadar guncelleme gonderen merkezi sinif.
 * <p>
 * BU SINIF, {@link LuxelConfig} icindeki HER AYARI gercekten okuyup uyguluyor:
 * <ul>
 *   <li>{@code modEnabled} / {@code dynamicLightsEnabled} - motoru tamamen durdurur</li>
 *   <li>{@code otherPlayersLight} - diger oyuncularin takip edilip edilmeyecegini,
 *       hem ACMA hem KAPATMA yonunde canli olarak kontrol eder</li>
 *   <li>{@code mobLight} - moblarin periyodik taranip takip listesine eklenmesini kontrol eder</li>
 *   <li>{@code droppedItemLight} - yerdeki esyalarin periyodik taranmasini kontrol eder</li>
 *   <li>{@code maxLightDistance} - kesin mesafe siniri (quality carpani ile birlikte)</li>
 *   <li>{@code updateIntervalTicks} - guncelleme sikligi (quality carpani ile birlikte)</li>
 *   <li>{@code lightQuality} - mesafe ve guncelleme sikligini gercekten olcekler</li>
 *   <li>{@code performanceMode} - ek bir ust sinir/taban degeri zorlar</li>
 *   <li>{@code coloredLight} - kaynagin rengine gore periyodik renkli parcacik yayar</li>
 *   <li>{@code debugMode} - konsola detayli log basar</li>
 * </ul>
 */
public final class LightSourceManager {

	private static final LightSourceManager INSTANCE = new LightSourceManager();

	/** Aktif olarak takip edilen dinamik isik kaynaklari (oyuncu, mob, dropped item). */
	private final Set<Entity> trackedSources = new HashSet<>();

	/** pos (BlockPos.asLong) -> o pozisyonda o an gecerli olan en yuksek dinamik isik seviyesi. */
	private final Long2IntOpenHashMap activeLuminance = new Long2IntOpenHashMap();

	private int tickCounter = 0;
	private int entityScanCounter = 0;

	private static final int ENTITY_SCAN_INTERVAL_TICKS = 20;
	private static final int PARTICLE_INTERVAL_TICKS = 6;

	private LightSourceManager() {
		activeLuminance.defaultReturnValue(0);
	}

	public static LightSourceManager getInstance() {
		return INSTANCE;
	}

	public void trackSource(Entity entity) {
		trackedSources.add(entity);
	}

	public void untrackSource(Entity entity) {
		trackedSources.remove(entity);
	}

	/**
	 * BlockViewLuminanceMixin tarafindan cagirilir: verilen pozisyonda dinamik olarak
	 * enjekte edilmesi gereken isik seviyesini dondurur (yoksa 0).
	 */
	public int getDynamicLuminance(long packedPos) {
		return activeLuminance.get(packedPos);
	}

	public boolean hasAnyDynamicLight() {
		return !activeLuminance.isEmpty();
	}

	public void onClientTick(MinecraftClient client) {
		LuxelConfig config = LuxelConfig.get();
		ClientWorld world = client.world;
		PlayerEntity camera = client.player;

		if (!config.modEnabled || !config.dynamicLightsEnabled || world == null || camera == null) {
			// Mod veya dinamik isiklar kapatildiysa, kalan tum isiklari temizle ki
			// eski isiklar donuk sekilde ekranda takili kalmasin.
			if (!activeLuminance.isEmpty()) {
				clearAllLights(world);
			}
			return;
		}

		tickCounter++;
		int interval = effectiveInterval(config);
		if (tickCounter % interval != 0) {
			return;
		}

		// Yerel oyuncu her zaman takip edilir.
		trackedSources.add(camera);

		// Diger oyunculari canli olarak ekle/cikar (config her tick yeniden okunur).
		if (config.otherPlayersLight) {
			world.getPlayers().forEach(trackedSources::add);
		}

		// Mob ve dusen esyalari periyodik olarak tara (tam entity taramasi pahalidir,
		// bu yuzden ayri, daha seyrek bir aralikla yapilir).
		entityScanCounter++;
		if ((config.mobLight || config.droppedItemLight) && entityScanCounter >= ENTITY_SCAN_INTERVAL_TICKS) {
			entityScanCounter = 0;
			scanForAdditionalSources(world, camera, config);
		}

		// Artik gecerli olmayan kaynaklari (config kapandi, entity yok oldu, mesafe disi) temizle.
		trackedSources.removeIf(entity -> !isStillValid(entity, config, camera));

		double maxDistSq = effectiveMaxDistance(config) * effectiveMaxDistance(config);
		for (Entity entity : trackedSources) {
			updateEntityLight(world, camera, entity, config, maxDistSq);
		}
	}

	private void scanForAdditionalSources(ClientWorld world, PlayerEntity camera, LuxelConfig config) {
		double radius = config.maxLightDistance;
		double radiusSq = radius * radius;
		for (Entity entity : world.getEntities()) {
			if (entity.squaredDistanceTo(camera) > radiusSq) {
				continue;
			}
			if (config.mobLight && entity instanceof LivingEntity && !(entity instanceof PlayerEntity)) {
				trackedSources.add(entity);
			} else if (config.droppedItemLight && entity instanceof ItemEntity) {
				trackedSources.add(entity);
			}
		}
	}

	private boolean isStillValid(Entity entity, LuxelConfig config, PlayerEntity camera) {
		if (entity == null || entity.isRemoved()) {
			return false;
		}
		if (entity == camera) {
			return true;
		}
		if (entity instanceof PlayerEntity) {
			return config.otherPlayersLight;
		}
		if (entity instanceof ItemEntity) {
			return config.droppedItemLight;
		}
		if (entity instanceof LivingEntity) {
			return config.mobLight;
		}
		return false;
	}

	private void updateEntityLight(World world, PlayerEntity camera, Entity entity, LuxelConfig config, double maxDistSq) {
		if (!(entity instanceof DynamicLightSource source)) {
			return;
		}

		double distanceSq = entity.squaredDistanceTo(camera);

		int newLevel = 0;
		LightEntry entry = null;
		if (distanceSq <= maxDistSq) {
			entry = computeLightEntry(entity);
			newLevel = entry == null ? 0 : entry.level();
		}

		long oldPos = source.luxel$getLastLightPos();
		int oldLevel = source.luxel$getLuminance();
		BlockPos newBlockPos = entity.getBlockPos();
		long newPos = newBlockPos.asLong();

		boolean posChanged = oldPos != newPos;
		boolean levelChanged = oldLevel != newLevel;

		if (posChanged || levelChanged) {
			if (oldLevel > 0) {
				activeLuminance.remove(oldPos);
				scheduleLightUpdate(world, oldPos);
			}
			if (newLevel > 0) {
				addContribution(newPos, newLevel);
				scheduleLightUpdate(world, newPos);
			}
			source.luxel$setLuminance(newLevel);
			source.luxel$setLastLightPos(newPos);

			if (config.debugMode) {
				Luxel.LOGGER.info("[Luxel] {} -> seviye={} pos={}", entity.getName().getString(), newLevel, newBlockPos);
			}
		}

		if (config.coloredLight && newLevel > 0 && entry != null && world instanceof ClientWorld clientWorld) {
			maybeSpawnColorParticle(clientWorld, entity, entry.colorRgb());
		}
	}

	private void maybeSpawnColorParticle(ClientWorld world, Entity entity, int colorRgb) {
		if (tickCounter % PARTICLE_INTERVAL_TICKS != 0) {
			return;
		}
		float r = ((colorRgb >> 16) & 0xFF) / 255f;
		float g = ((colorRgb >> 8) & 0xFF) / 255f;
		float b = (colorRgb & 0xFF) / 255f;
		DustParticleEffect effect = new DustParticleEffect(new Vector3f(r, g, b), 1.0f);
		world.addParticle(effect,
				entity.getX() + (world.random.nextDouble() - 0.5) * 0.4,
				entity.getY() + 0.3 + world.random.nextDouble() * 0.3,
				entity.getZ() + (world.random.nextDouble() - 0.5) * 0.4,
				0.0, 0.01, 0.0);
	}

	private LightEntry computeLightEntry(Entity entity) {
		if (entity instanceof ItemEntity itemEntity) {
			return ItemLightRegistry.get(itemEntity.getStack().getItem());
		}

		if (entity instanceof LivingEntity living) {
			LightEntry main = entryOf(living.getMainHandStack());
			LightEntry off = entryOf(living.getOffHandStack());
			if (main == null) return off;
			if (off == null) return main;
			return main.level() >= off.level() ? main : off;
		}

		return null;
	}

	private LightEntry entryOf(ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) {
			return null;
		}
		return ItemLightRegistry.get(stack.getItem());
	}

	private void addContribution(long pos, int level) {
		int current = activeLuminance.get(pos);
		if (level > current) {
			activeLuminance.put(pos, level);
		}
	}

	private void scheduleLightUpdate(World world, long pos) {
		BlockPos blockPos = BlockPos.fromLong(pos);
		world.getChunkManager().getLightingProvider().checkBlock(blockPos);
	}

	/**
	 * lightQuality ayarinin GERCEKTEN etkili oldugu yer: dusuk kalite daha kisa
	 * mesafe ve daha seyrek guncelleme, ultra ise tam tersi anlamina gelir.
	 */
	private int effectiveInterval(LuxelConfig config) {
		int qualityFactor = switch (config.lightQuality) {
			case LOW -> 3;
			case MEDIUM -> 2;
			case HIGH -> 1;
			case ULTRA -> 1;
		};
		int base = Math.max(config.updateIntervalTicks, 1) * qualityFactor;
		if (config.performanceMode) {
			base = Math.max(base, 3);
		}
		return base;
	}

	private double effectiveMaxDistance(LuxelConfig config) {
		double qualityFactor = switch (config.lightQuality) {
			case LOW -> 0.5;
			case MEDIUM -> 0.75;
			case HIGH -> 1.0;
			case ULTRA -> 1.25;
		};
		double distance = Math.max(config.maxLightDistance, 1) * qualityFactor;
		if (config.performanceMode) {
			distance = Math.min(distance, 16.0);
		}
		return distance;
	}

	private void clearAllLights(World world) {
		if (world == null) {
			for (long pos : activeLuminance.keySet().toLongArray()) {
				activeLuminance.remove(pos);
			}
			activeLuminance.clear();
			return;
		}
		for (long pos : activeLuminance.keySet().toLongArray()) {
			scheduleLightUpdate(world, pos);
		}
		activeLuminance.clear();
		trackedSources.clear();
	}

	public void clear() {
		trackedSources.clear();
		activeLuminance.clear();
	}
}
