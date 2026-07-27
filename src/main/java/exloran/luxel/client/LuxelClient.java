package exloran.luxel.client;

import exloran.luxel.Luxel;
import exloran.luxel.client.config.LuxelConfig;
import exloran.luxel.client.light.ItemLightRegistry;
import exloran.luxel.client.light.LightSourceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Luxel modunun client giris noktasi. Tamamen client-side calisir,
 * hicbir sunucu bilesenine ihtiyac duymaz.
 */
public final class LuxelClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Luxel.LOGGER.info("Luxel client baslatiliyor...");

		// Config'i erken yukle (dosya yoksa varsayilanlarla olusturur).
		LuxelConfig.get();

		// Varsayilan isik yayan esyalari kaydet.
		ItemLightRegistry.bootstrapDefaults();

		// Her client tick'inde dinamik isik motorunu calistir.
		ClientTickEvents.END_CLIENT_TICK.register(client -> LightSourceManager.getInstance().onClientTick(client));

		Luxel.LOGGER.info("Luxel hazir. Dinamik isik: {}", LuxelConfig.get().dynamicLightsEnabled ? "acik" : "kapali");
	}
}
