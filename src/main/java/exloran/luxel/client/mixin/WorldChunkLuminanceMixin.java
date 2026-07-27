package exloran.luxel.client.mixin;

import exloran.luxel.client.light.LightSourceManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ISIK MOTORU ENJEKSIYON NOKTASI
 * <p>
 * Vanilla'nin blok isik hesaplayicisi ({@code BlockLightStorage}), bir pozisyondaki
 * isik kaynagini {@code WorldChunk#getLuminance(BlockPos)} uzerinden okur. Bu mixin,
 * o degeri LightSourceManager'daki dinamik kaynaklarla kiyaslayip daha buyuk olani
 * dondurerek, gercek bir blok yerlestirmeden isik olusturur.
 * <p>
 * <b>NOT (Yarn mapping uyarisi):</b> {@code getLuminance(BlockPos)} imzasi kullandigin
 * Yarn build'ine gore farkli olabilir (bazi 1.21.x snapshotlarinda parametre sirasi
 * veya metod adi degisebiliyor). Eger Gradle derlemesi
 * "Unable to locate method GETLUMINANCE" hatasi verirse, WorldChunk sinifini
 * decompiled (genclass) halinde ac ve "getLuminance" icin dogru imzayi bul; hemen hemen
 * her zaman tek bir int (BlockPos) parametresi alir. Bu, gecmiste cozdugumuz
 * HandledScreenAccessor / refmap sorunlariyla ayni turden bir mapping uyumsuzlugudur.
 */
@Mixin(WorldChunk.class)
public abstract class WorldChunkLuminanceMixin {

	@Inject(method = "getLuminance", at = @At("RETURN"), cancellable = true)
	private void luxel$injectDynamicLuminance(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int dynamic = LightSourceManager.getInstance().getDynamicLuminance(pos.asLong());
		if (dynamic > cir.getReturnValueI()) {
			cir.setReturnValue(dynamic);
		}
	}
}
