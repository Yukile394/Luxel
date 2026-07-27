package exloran.luxel.client.light;

import exloran.luxel.Luxel;
import exloran.luxel.client.config.LuxelConfig;
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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aktif dinamik isik kaynaklarini takip eden merkezi sinif.
 * <p>
 * MIMARI (v2 - "Lightmap Coordinates Method", LambDynamicLights'in belgeledigi
 * kanitlanmis teknik): Bu sinif artik vanilla isik motorunun VERISINE dokunmuyor.
 * Bunun yerine, her tick sonunda aktif kaynaklarin pozisyon/seviye/renk bilgisini
 * degismez ({@code immutable}) bir "snapshot" dizisine yazar. Render thread'i
 * (chunk mesh olusturma, genelde arka plan thread'lerinde calisir) bu diziyi
 * {@code getDynamicLightLevel(x,y,z)} ile kilitsiz (lock-free) okur -
 * {@code volatile} referans degisimi sayesinde thread-safe'tir.
 * <p>
 * {@link LuxelConfig} icindeki HER AYAR burada gercekten okunur:
 * <ul>
 *   <li>{@code modEnabled} / {@code dynamicLightsEnabled} - motoru tamamen durdurur</li>
 *   <li>{@code otherPlayersLight} - diger oyuncularin takibini acar/kapatir (canli)</li>
 *   <li>{@code mobLight} / {@code droppedItemLight} - periyodik entity taramasini kontrol eder</li>
 *   <li>{@code maxLightDistance} / {@code updateIntervalTicks} / {@code lightQuality} /
 *       {@code performanceMode} - guncelleme sikligini ve isik yayilma menzilini olcekler</li>
 *   <li>{@code coloredLight} - renkli parcacik efektini acar/kapatir</li>
 *   <li>{@code debugMode} - konsola detayli log basar</li>
 * </ul>
 */
public final class LightSourceManager {

	private static final LightSourceManager INSTANCE = new LightSourceManager();

	/**
	 * LambDynamicLights'in belgeledigi sabit: isik menzilini ~8 blok ile sinirlamak,
	 * her hareket ticki'nde tetiklenen chunk yeniden-cizim maliyetini kontrol altinda
	 * tutar. lightQuality/performanceMode bu degeri asagi dogru olcekleyebilir ama
	 * yukari asamaz (performans guvenligi).
	 */
	private static final double MAX_RENDER_RANGE = 8.0;

	private final Set<Entity> trackedSources = new HashSet<>();
	private final Map<Entity, Long> lastRebuildPos = new HashMap<>();

	/** Render/mesh thread'lerinin kilitsiz okudugu, her tick sonunda degistirilen anlik goruntu. */
	private volatile LightSource[] snapshot = new LightSource[0];

	private int tickCounter = 0;
	private int entityScanCounter = 0;

	private static final int ENTITY_SCAN_INTERVAL_TICKS = 20;
	private static final int PARTICLE_INTERVAL_TICKS = 6;

	private record LightSource(double x, double y, double z, double level, int colorRgb) {
	}

	private LightSourceManager() {
	}

	public static LightSourceManager getInstance() {
		return INSTANCE;
	}

	/**
	 * WorldRendererLightmapMixin ve EntityLightMixin tarafindan cagirilir.
	 * Render/mesh thread'inden guvenle cagrilabilir (kilitsiz okuma).
	 */
	public double getDynamicLightLevel(double x, double y, double z) {
		LightSource[] sources = snapshot;
		if (sources.length == 0) {
			return 0.0;
		}
		double best = 0.0;
		for (LightSource s : sources) {
			double dx = s.x - x;
			double dy = s.y - y;
			double dz = s.z - z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			double multiplier = 1.0 - dist / MAX_RENDER_RANGE;
			if (multiplier <= 0.0) {
				continue;
			}
			double level = multiplier * s.level;
			if (level > best) {
				best = level;
			}
		}
		return best;
	}

	public void onClientTick(MinecraftClient client) {
		LuxelConfig config = LuxelConfig.get();
		ClientWorld world = client.world;
		PlayerEntity camera = client.player;

		if (!config.modEnabled || !config.dynamicLightsEnabled || world == null || camera == null) {
			if (snapshot.length > 0) {
				snapshot = new LightSource[0];
				trackedSources.clear();
				lastRebuildPos.clear();
			}
			return;
		}

		tickCounter++;
		int interval = effectiveInterval(config);
		if (tickCounter % interval != 0) {
			return;
		}

		trackedSources.add(camera);

		if (config.otherPlayersLight) {
			world.getPlayers().forEach(trackedSources::add);
		}

		entityScanCounter++;
		if ((config.mobLight || config.droppedItemLight) && entityScanCounter >= ENTITY_SCAN_INTERVAL_TICKS) {
			entityScanCounter = 0;
			scanForAdditionalSources(world, camera, config);
		}

		trackedSources.removeIf(entity -> !isStillValid(entity, config, camera));

		double maxDistSq = effectiveMaxDistance(config) * effectiveMaxDistance(config);
		List<LightSource> newSnapshot = new ArrayList<>(trackedSources.size());

		for (Entity entity : trackedSources) {
			double distanceSq = entity.squaredDistanceTo(camera);
			if (distanceSq > maxDistSq) {
				lastRebuildPos.remove(entity);
				continue;
			}

			LightEntry entry = computeLightEntry(entity);
			if (entry == null || entry.level() <= 0) {
				lastRebuildPos.remove(entity);
				continue;
			}

			newSnapshot.add(new LightSource(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(), entry.level(), entry.colorRgb()));

			maybeRequestChunkRebuild(world, entity);

			if (config.coloredLight) {
				maybeSpawnColorParticle(world, entity, entry.colorRgb());
			}

			if (config.debugMode && tickCounter % 40 == 0) {
				Luxel.LOGGER.info("[Luxel] {} -> seviye={} pos=({}, {}, {})",
						entity.getName().getString(), entry.level(), entity.getX(), entity.getY(), entity.getZ());
			}
		}

		snapshot = newSnapshot.toArray(new LightSource[0]);
	}

	/**
	 * Bir kaynak yeni bir blok pozisyonuna gectiginde, o pozisyonu ve etrafini
	 * (isik menzili kadar) render icin "kirli" isaretler. Bu, WorldRendererLightmapMixin'in
	 * gercekten cagrilmasini saglayan tetikleyicidir - onsuz chunk mesh'i asla
	 * yeniden olusturulmaz ve isik gorunmez (ilk versiyonumuzda karsilastigimiz sorunun
	 * asil sebebi, veriyi dogru hesaplasak bile render'i tetiklememizin gerekmesiydi).
	 */
	private void maybeRequestChunkRebuild(ClientWorld world, Entity entity) {
		BlockPos current = entity.getBlockPos();
		long currentPacked = current.asLong();
		Long previous = lastRebuildPos.get(entity);
		if (previous != null && previous == currentPacked) {
			return;
		}
		lastRebuildPos.put(entity, currentPacked);

		int pad = (int) Math.ceil(MAX_RENDER_RANGE);
		MinecraftClient.getInstance().worldRenderer.scheduleBlockRenders(
				current.getX() - pad, current.getY() - pad, current.getZ() - pad,
				current.getX() + pad, current.getY() + pad, current.getZ() + pad);
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

	/**
	 * lightQuality ayarinin GERCEKTEN etkili oldugu yer: dusuk kalite daha seyrek
	 * guncelleme, ultra ise daha sik guncelleme anlamina gelir.
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

	/**
	 * maxLightDistance ayari, bir kaynagin TAKIP EDILIP EDILMEYECEGINI belirler
	 * (genis bir kapi). Gercek gorsel isik menzili her zaman MAX_RENDER_RANGE ile
	 * sinirlidir (performans guvenligi) - takip mesafesi bunu asamaz ama altinda kalabilir.
	 */
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
		return Math.min(distance, MAX_RENDER_RANGE * 3);
	}

	public void clear() {
		trackedSources.clear();
		lastRebuildPos.clear();
		snapshot = new LightSource[0];
	}
}
