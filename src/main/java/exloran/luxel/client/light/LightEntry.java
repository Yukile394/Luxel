package exloran.luxel.client.light;

/**
 * Bir esyanin veya blogun yaydigi isigin seviyesini (0-15) ve rengini tanimlar.
 * <p>
 * {@code colorRgb} degeri renkli isik ozelligi kapaliyken kullanilmaz,
 * yalnizca HUD/partikul efektlerinde ve gelecekteki renkli isik render
 * katmaninda referans olarak tutulur.
 *
 * @param level    Isik seviyesi, vanilla blok isik olcegiyle ayni (0-15).
 * @param colorRgb Isigin rengi, 0xRRGGBB formatinda.
 */
public record LightEntry(int level, int colorRgb) {

	public static final int WARM_WHITE = 0xFFD9A6;
	public static final int SOUL_BLUE = 0x5AC8FA;
	public static final int LAVA_ORANGE = 0xFF7A1A;
	public static final int SEA_CYAN = 0xB6F0FF;

	public LightEntry {
		if (level < 0 || level > 15) {
			throw new IllegalArgumentException("Isik seviyesi 0-15 araliginda olmalidir: " + level);
		}
	}

	public static LightEntry of(int level) {
		return new LightEntry(level, WARM_WHITE);
	}

	public static LightEntry of(int level, int colorRgb) {
		return new LightEntry(level, colorRgb);
	}
}
