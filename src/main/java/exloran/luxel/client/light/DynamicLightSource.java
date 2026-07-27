package exloran.luxel.client.light;

/**
 * Mixin ile entity siniflarina (oyuncu, mob, item entity) enjekte edilen arayuz.
 * {@link LightSourceManager} her tick bu arayuzu implemente eden entity'lerin
 * elindeki/tasidigi esyayi kontrol ederek isik seviyesini gunceller.
 */
public interface DynamicLightSource {

	/**
	 * Bu tick icin hesaplanmis isik seviyesini dondurur (0 = isik yok).
	 */
	int luxel$getLuminance();

	/**
	 * Isik seviyesini gunceller. Sadece LightSourceManager tarafindan cagirilmalidir.
	 */
	void luxel$setLuminance(int luminance);

	/**
	 * Bu kaynagin son islenen blok konumunu dondurur (paketlenmis long, BlockPos.asLong()).
	 * Konum degismediyse gereksiz light-engine guncellemesi yapilmaz.
	 */
	long luxel$getLastLightPos();

	void luxel$setLastLightPos(long pos);

	/**
	 * Bu entity'nin dunya uzerinden kaldirilip kaldirilmadigini (ayrilmis) belirtir,
	 * boylece LightSourceManager onu aktif kaynaklar listesinden temizleyebilir.
	 */
	boolean luxel$isRemoved();
}
