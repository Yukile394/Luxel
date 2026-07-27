package exloran.luxel.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu'de "Luxel" icin ayarlar butonunu gosterir.
 * Mod Menu kurulu degilse bu sinif hic yuklenmez (fabric.mod.json'daki
 * "modmenu" entrypoint'i sadece Mod Menu varsa devreye girer).
 */
public final class LuxelModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return LuxelConfigScreen::create;
	}
}
