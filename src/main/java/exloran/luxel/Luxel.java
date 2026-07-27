package exloran.luxel;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Luxel modunun ortak (common) giris noktasi.
 * <p>
 * Luxel tamamen client-side calisan bir mod oldugu icin bu sinif sadece
 * loglama ve sabitler icin kullanilir; asil mantik {@code exloran.luxel.client}
 * paketinde bulunur.
 */
public final class Luxel implements ModInitializer {

	public static final String MOD_ID = "luxel";
	public static final Logger LOGGER = LoggerFactory.getLogger("Luxel");

	@Override
	public void onInitialize() {
		LOGGER.info("Luxel yuklendi (common init) - dinamik isik sistemi client tarafinda baslatilacak.");
	}
}
