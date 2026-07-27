package exloran.luxel.client.mixin;

import exloran.luxel.client.light.DynamicLightSource;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Yere dusmus esyalarin (dropped torch, dropped lantern vb.) isik yaymasini saglar.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements DynamicLightSource {

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
		return ((ItemEntity) (Object) this).isRemoved();
	}
}
