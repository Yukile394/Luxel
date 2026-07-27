package exloran.luxel.client.mixin;

import exloran.luxel.client.light.LightSourceManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ISIK MOTORU ENJEKSIYON NOKTASI (duzeltilmis)
 * <p>
 * {@code getLuminance(BlockPos)} bir blok sinifinin (WorldChunk vb.) kendi metodu degil,
 * {@link BlockView} arayuzunde tanimli bir "default method"dur:
 * <pre>
 *   default int getLuminance(BlockPos pos) {
 *       return this.getBlockState(pos).getLuminance();
 *   }
 * </pre>
 * Bu yuzden mixin'in WorldChunk'i hedeflemesi basarisiz oluyordu (metod o sinifin
 * bytecode'unda fiilen yok, sadece miras aliniyor). Dogru hedef BlockView arayuzunun
 * kendisidir; Mixin, arayuzlerdeki default metodlara da enjeksiyon yapabilir.
 * <p>
 * Bu, WorldChunk'i implement eden butun siniflari (ChunkCache, World, WorldChunk...)
 * tek seferde kapsar.
 */
@Mixin(BlockView.class)
public interface BlockViewLuminanceMixin {

	@Inject(method = "getLuminance", at = @At("RETURN"), cancellable = true)
	default void luxel$injectDynamicLuminance(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int dynamic = LightSourceManager.getInstance().getDynamicLuminance(pos.asLong());
		if (dynamic > cir.getReturnValueI()) {
			cir.setReturnValue(dynamic);
		}
	}
}
