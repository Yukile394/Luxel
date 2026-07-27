package exloran.luxel.client.mixin;

import exloran.luxel.client.light.LightSourceManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ISIK MOTORU ENJEKSIYON NOKTASI (v2 - "Lightmap Coordinates Method")
 * <p>
 * Bir onceki denemede ({@code BlockViewLuminanceMixin}) vanilla'nin gercek isik
 * motoruna (LightingProvider/BlockLightStorage) sahte bir kaynak enjekte etmeye
 * calisiliyordu. Bu, LambDynamicLights modunun kendi belgelerinde ("Not used by
 * this mod") acikca "kirilgan ve calismiyor" diye isaretlenmis bir yontemdir -
 * bizim testimizde de (meşale elde, sifir isik) tam olarak bu sekilde basarisiz oldu.
 * <p>
 * DOGRU VE KANITLANMIS teknik ("Lightmap Coordinates Method"): vanilla isik
 * VERISINE dokunmuyoruz, bunun yerine RENDER anindaki parlaklik hesabina
 * ({@code WorldRenderer#getLightmapCoordinates}) mudahale ediyoruz. Bu metod
 * her blok yuzeyi cizilirken cagrilir ve isik degerini
 * {@code (skyLevel << 20 | blockLevel << 4)} formatinda paketlenmis olarak dondurur.
 * <p>
 * Vanilla degeri aliyoruz, LightSourceManager'dan o pozisyondaki dinamik isik
 * seviyesini (mesafeye gore azalan, ondalikli hassasiyette) soruyoruz, vanilla'dan
 * buyukse blockLevel kismini degistirip geri donuyoruz. skyLevel kismina dokunulmaz.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererLightmapMixin {

	@Inject(method = "getLightmapCoordinates", at = @At("TAIL"), cancellable = true)
	private static void luxel$injectDynamicLightmap(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		double dynamicLevel = LightSourceManager.getInstance().getDynamicLightLevel(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		if (dynamicLevel <= 0.0) {
			return;
		}

		int vanilla = cir.getReturnValueI();
		int skyPart = vanilla & 0xFFF00000;
		int vanillaBlockCoord = (vanilla >> 4) & 0xFFFF;

		// Bitshift yerine 16.0 ile carpma kullaniyoruz (LambDynamicLights'in belgeledigi
		// hassasiyet notu): boylece mesafeye gore yumusak, kademesiz bir gecis olur.
		int dynamicBlockCoord = (int) (dynamicLevel * 16.0);

		if (dynamicBlockCoord > vanillaBlockCoord) {
			int newPacked = skyPart | ((dynamicBlockCoord & 0xFFFF) << 4);
			cir.setReturnValue(newPacked);
		}
	}
}
