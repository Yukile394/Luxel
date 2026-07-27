package exloran.luxel.client.mixin;

import exloran.luxel.client.light.LightSourceManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WorldRendererLightmapMixin sadece zemin/blok yuzeylerini aydinlatir. Bunun
 * disinda entity modelinin KENDISI (oyuncu, mob) de aydinlik bir yerde karanlik
 * gorunmemelidir - bu da ayni "lightmap coordinates" teknigiyle burada saglanir.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityLightMixin {

	@Inject(method = "getLight", at = @At("TAIL"), cancellable = true)
	private void luxel$injectDynamicEntityLight(Entity entity, float tickDelta, CallbackInfoReturnable<Integer> cir) {
		double dynamicLevel = LightSourceManager.getInstance().getDynamicLightLevel(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
		if (dynamicLevel <= 0.0) {
			return;
		}

		int vanilla = cir.getReturnValueI();
		int skyPart = vanilla & 0xFFF00000;
		int vanillaBlockCoord = (vanilla >> 4) & 0xFFFF;

		int dynamicBlockCoord = (int) (dynamicLevel * 16.0);

		if (dynamicBlockCoord > vanillaBlockCoord) {
			int newPacked = skyPart | ((dynamicBlockCoord & 0xFFFF) << 4);
			cir.setReturnValue(newPacked);
		}
	}
}
