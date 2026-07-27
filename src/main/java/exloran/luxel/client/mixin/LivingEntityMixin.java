package exloran.luxel.client.mixin;

import exloran.luxel.client.light.DynamicLightSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * LivingEntity (zombi, iskelet vb. moblar) icin DynamicLightSource implementasyonu.
 * PlayerEntity zaten LivingEntity'den turedigi ve kendi mixin'ine sahip oldugu icin
 * burada tekrar override edilmez; Mixin bunu otomatik olarak ayirt eder cunku
 * PlayerEntityMixin daha spesifik hedefe sahiptir.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements DynamicLightSource {

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
		LivingEntity self = (LivingEntity) (Object) this;
		return self.isRemoved() || self instanceof PlayerEntity;
	}
}
