package folk.sisby.starcaller.mixin.client;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.client.StarcallerClient;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.util.StarUtil;import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel implements StarcallerLevel {
	@Unique private long starcaller$seed = 10842L;
	@Unique private int starcaller$iterations = 1500;
	@Unique private List<Star> starcaller$stars = StarUtil.generateStars(starcaller$seed, starcaller$iterations);

	@Inject(method = "getStarBrightness", at = @At("HEAD"), cancellable = true)
	public void fullBrightStarsWithSpear(float f, CallbackInfoReturnable<Float> cir) {
		Player player = Minecraft.getInstance().player;
		if (player.getMainHandItem().is(Starcaller.SPEAR.get()) || player.getOffhandItem().is(Starcaller.SPEAR.get())) {
			cir.setReturnValue(1.0F);
			cir.cancel();
		}
	}

	@Override
	public void starcaller$groundStar(Player cause, Star star) {
		StarcallerClient.groundStar(((ClientLevel) (Object) this), star);
	}


	@Override
	public void starcaller$freeStar(Player cause, Star star) {
		StarcallerClient.freeStar(((ClientLevel) (Object) this), star);
	}

	@Override
	public void starcaller$colorStar(Player cause, Star star, int color) {
		StarcallerClient.colorStar(cause, ((ClientLevel) (Object) this), star, color);
	}

	@Override
	public long starcaller$getSeed() {
		return starcaller$seed;
	}

	@Override
	public int starcaller$getIterations() {
		return starcaller$iterations;
	}

	@Override
	public List<Star> starcaller$getStars() {
		return starcaller$stars;
	}

	@Override
	public void starcaller$setGeneratorValues(long seed, int iterations) {
		this.starcaller$seed = seed;
		this.starcaller$iterations = iterations;
		this.starcaller$stars = StarUtil.generateStars(starcaller$seed, starcaller$iterations);
		StarcallerClient.reloadStars((ClientLevel) (Object) this);
	}
}
