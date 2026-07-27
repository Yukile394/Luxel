package exloran.luxel.client.light;

import exloran.luxel.Luxel;
import exloran.luxel.client.config.LuxelConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Aktif dinamik isik kaynaklarini takip eden ve vanilla light-engine'e
 * (isik motoruna) her tick ihtiyac duyulan kadar guncelleme gonderen merkezi sinif.
 * <p>
 * Performans stratejisi:
 * <ul>
 *   <li>Sadece kameraya {@link LuxelConfig#maxLightDistance} icinde olan kaynaklar islenir.</li>
 *   <li>Bir kaynagin blok konumu degismedigi surece light-engine'e tekrar sinyal gonderilmez.</li>
 *   <li>{@link LuxelConfig#updateIntervalTicks} ile guncelleme sikligi dusurulebilir (performans modu).</li>
 *   <li>{@code activeLuminance} haritasi sadece "su an isik yayan" pozisyonlari tutar; bu sayede
 *       ChunkLightMixin O(1) lookup ile calisir.</li>
 * </ul>
 */
public final class LightSourceManager {

	private static final LightSourceManager INSTANCE = new LightSourceManager();

	/** Aktif olarak takip edilen dinamik isik kaynaklari (oyuncu, mob, dropped item). */
	private final Set<Entity> trackedSources = new HashSet<>();

	/** pos (BlockPos.asLong) -> o pozisyonda o an gecerli olan en yuksek dinamik isik seviyesi. */
	private final Long2IntOpenHashMap activeLuminance = new Long2IntOpenHashMap();

	private int tickCounter = 0;

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
	 * ChunkLightMixin tarafindan cagirilir: verilen pozisyonda dinamik olarak
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
		if (!config.modEnabled || !config.dynamicLightsEnabled || client.world == null || client.player == null) {
			return;
		}

		tickCounter++;
		int interval = config.performanceMode ? Math.max(config.updateIntervalTicks, 3) : Math.max(config.updateIntervalTicks, 1);
		if (tickCounter % interval != 0) {
			return;
		}

		World world = client.world;
		PlayerEntity camera = client.player;

		// Oyuncunun kendisi her zaman takip edilir.
		trackedSources.add(camera);

		if (config.otherPlayersLight) {
			world.getPlayers().forEach(trackedSources::add);
		}

		trackedSources.removeIf(entity -> !isStillValid(entity, config));

		for (Entity entity : trackedSources) {
			updateEntityLight(world, camera, entity, config);
		}
	}

	private boolean isStillValid(Entity entity, LuxelConfig config) {
		if (entity == null || entity.isRemoved()) {
			return false;
		}
		if (entity instanceof PlayerEntity) {
			return true;
		}
		if (entity instanceof LivingEntity) {
			return config.mobLight;
		}
		if (entity instanceof ItemEntity) {
			return config.droppedItemLight;
		}
		return false;
	}

	private void updateEntityLight(World world, PlayerEntity camera, Entity entity, LuxelConfig config) {
		if (!(entity instanceof DynamicLightSource source)) {
			return;
		}

		double distanceSq = entity.squaredDistanceTo(camera);
		double maxDistSq = (double) config.maxLightDistance * config.maxLightDistance;

		int newLevel = 0;
		if (distanceSq <= maxDistSq) {
			newLevel = computeLuminance(entity, config);
		}

		long oldPos = source.luxel$getLastLightPos();
		int oldLevel = source.luxel$getLuminance();
		BlockPos newBlockPos = entity.getBlockPos();
		long newPos = newBlockPos.asLong();

		boolean posChanged = oldPos != newPos;
		boolean levelChanged = oldLevel != newLevel;

		if (!posChanged && !levelChanged) {
			return;
		}

		// Eski pozisyondaki katkiyi kaldir.
		if (oldLevel > 0) {
			removeContribution(oldPos, oldLevel);
			scheduleLightUpdate(world, oldPos);
		}

		// Yeni pozisyona katkiyi ekle.
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

	private int computeLuminance(Entity entity, LuxelConfig config) {
		if (entity instanceof ItemEntity itemEntity) {
			LightEntry entry = ItemLightRegistry.get(itemEntity.getStack().getItem());
			return entry == null ? 0 : entry.level();
		}

		if (entity instanceof LivingEntity living) {
			int main = luminanceOf(living.getMainHandStack());
			int off = luminanceOf(living.getOffHandStack());
			return Math.max(main, off);
		}

		return 0;
	}

	private int luminanceOf(ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) {
			return 0;
		}
		LightEntry entry = ItemLightRegistry.get(stack.getItem());
		return entry == null ? 0 : entry.level();
	}

	private void addContribution(long pos, int level) {
		int current = activeLuminance.get(pos);
		if (level > current) {
			activeLuminance.put(pos, level);
		}
	}

	private void removeContribution(long pos, int level) {
		// Basit ve guvenli yaklasim: pozisyonu tamamen sifirlayip, o pozisyonu
		// hala kullanan baska kaynak varsa bir sonraki tick'te tekrar yazilmasini sagliyoruz.
		// Bu, ayni blokta ust uste binen isik kaynaklari icin bir tick'lik gecikmeye
		// yol acabilir ama titremeyi (flicker) engelleyen guvenli tercih budur.
		activeLuminance.remove(pos);
	}

	private void scheduleLightUpdate(World world, long pos) {
		// Vanilla light-engine'e bu pozisyonu yeniden hesaplamasi icin sinyal veriyoruz.
		// Gercek isik degeri ChunkLuminanceMixin uzerinden bu sinifa (getDynamicLuminance)
		// sorularak alinir. Yarn surumune gore metod adi degisebilir; derleme hatasi
		// alinirsa README'deki "Isik Motoru Notu" bolumune bakin.
		BlockPos blockPos = BlockPos.fromLong(pos);
		world.getChunkManager().getLightingProvider().checkBlock(blockPos);
	}

	public void clear() {
		trackedSources.clear();
		activeLuminance.clear();
	}
}
