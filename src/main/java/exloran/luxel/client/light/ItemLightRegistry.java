package exloran.luxel.client.light;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Hangi esyalarin ne kadar isik yaydigini tutan merkezi, genisletilebilir kayit defteri.
 * <p>
 * Yeni bir esya/blok desteklemek icin tek yapilmasi gereken {@link #register} cagrisi
 * eklemektir; motorun geri kalani (LightSourceManager, mixinler) otomatik olarak
 * bu kaydi kullanir. Boylece ileride yeni isik kaynaklari eklemek tek satirlik bir islemdir.
 */
public final class ItemLightRegistry {

	private static final Map<Item, LightEntry> REGISTRY = new HashMap<>();

	private ItemLightRegistry() {
	}

	public static void register(Item item, LightEntry entry) {
		REGISTRY.put(item, entry);
	}

	public static void register(Item item, int level) {
		register(item, LightEntry.of(level));
	}

	public static void register(Item item, int level, int colorRgb) {
		register(item, LightEntry.of(level, colorRgb));
	}

	/**
	 * Verilen esya icin isik kaydini dondurur, yoksa null.
	 */
	public static LightEntry get(Item item) {
		if (item == null || item == Items.AIR) {
			return null;
		}
		return REGISTRY.get(item);
	}

	public static boolean isLightSource(Item item) {
		return get(item) != null;
	}

	/**
	 * Varsayilan esya listesini kaydeder. Ayarlar menusunden seviyeler
	 * calisma zamaninda LuxelConfig uzerinden ezilebilir (override).
	 */
	public static void bootstrapDefaults() {
		register(Items.TORCH, 14);
		register(Items.WALL_TORCH, 14);
		register(Items.SOUL_TORCH, 10, LightEntry.SOUL_BLUE);
		register(Items.SOUL_WALL_TORCH, 10, LightEntry.SOUL_BLUE);
		register(Items.LANTERN, 15);
		register(Items.SOUL_LANTERN, 10, LightEntry.SOUL_BLUE);
		register(Items.CAMPFIRE, 15);
		register(Items.SOUL_CAMPFIRE, 10, LightEntry.SOUL_BLUE);
		register(Items.GLOWSTONE, 15);
		register(Items.SEA_LANTERN, 15, LightEntry.SEA_CYAN);
		register(Items.JACK_O_LANTERN, 15);
		register(Items.REDSTONE_TORCH, 7);
		register(Items.REDSTONE_WALL_TORCH, 7);
		register(Items.END_ROD, 14);
		register(Items.BLAZE_ROD, 12, LightEntry.LAVA_ORANGE);
		register(Items.BLAZE_POWDER, 8, LightEntry.LAVA_ORANGE);
		register(Items.LAVA_BUCKET, 15, LightEntry.LAVA_ORANGE);
		register(Items.SHROOMLIGHT, 15);
		register(Items.OCHRE_FROGLIGHT, 15);
		register(Items.PEARLESCENT_FROGLIGHT, 15);
		register(Items.VERDANT_FROGLIGHT, 15);

		// Tum mum renkleri
		for (Item candle : new Item[] {
				Items.CANDLE, Items.WHITE_CANDLE, Items.ORANGE_CANDLE, Items.MAGENTA_CANDLE,
				Items.LIGHT_BLUE_CANDLE, Items.YELLOW_CANDLE, Items.LIME_CANDLE, Items.PINK_CANDLE,
				Items.GRAY_CANDLE, Items.LIGHT_GRAY_CANDLE, Items.CYAN_CANDLE, Items.PURPLE_CANDLE,
				Items.BLUE_CANDLE, Items.BROWN_CANDLE, Items.GREEN_CANDLE, Items.RED_CANDLE,
				Items.BLACK_CANDLE
		}) {
			register(candle, 8);
		}
	}
}
