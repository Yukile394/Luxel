package exloran.luxel.client.mixin;

import exloran.luxel.client.light.DynamicLightSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * PlayerEntity'yi (hem yerel oyuncu hem de diger oyuncular) bir dinamik
 * isik kaynagi haline getirir.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements DynamicLightSource {

	@Unique
	private int luxel$luminance = 0;

	@Unique
	private long luxel$lastLightPos = Long.MIN_VALUE;

	@Override
	public int luxel$getLuminance() {
		return luxel$luminance;
	}

	@Override
	public void luxel$setLuminance(int luminance) {
		this.luxel$luminance = luminance;
	}

	@Override
	public long luxel$getLastLightPos() {
		return luxel$lastLightPos;
	}

	@Override
	public void luxel$setLastLightPos(long pos) {
		this.luxel$lastLightPos = pos;
	}

	@Override
	public boolean luxel$isRemoved() {
		return ((PlayerEntity) (Object) this).isRemoved();
	}
}
