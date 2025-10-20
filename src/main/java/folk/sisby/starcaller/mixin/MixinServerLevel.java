package folk.sisby.starcaller.mixin;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.StarState;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.util.StarUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements StarcallerLevel {
	@Override
	public void starcaller$groundStar(Player cause, Star star) {
		Starcaller.groundStar(cause, (ServerLevel) (Object) this, star);
	}

	@Override
	public void starcaller$freeStar(Player cause, Star star) {
		Starcaller.freeStar(cause, (ServerLevel) (Object) this, star);
	}

	@Override
	public void starcaller$colorStar(Player cause, Star star, int color) {
		Starcaller.colorStar(cause, (ServerLevel) (Object) this, star, color);
	}

	@Override
	public long starcaller$getSeed() {
		ServerLevel self = (ServerLevel) (Object) this;
		StarState state = self.getDataStorage().get(nbt -> StarState.load(nbt, self.getSeed()), Starcaller.STATE_KEY);
		return state != null ? state.seed : (StarcallerConfig.starSeed != -1 ? StarcallerConfig.starSeed : self.getSeed());
	}

	@Override
	public int starcaller$getIterations() {
		ServerLevel self = (ServerLevel) (Object) this;
		StarState state = self.getDataStorage().get(nbt -> StarState.load(nbt, self.getSeed()), Starcaller.STATE_KEY);
		return state != null ? state.iterations : StarUtil.getGeneratorIterations(starcaller$getSeed(), StarcallerConfig.starLimit);
	}

	@Override
	public List<Star> starcaller$getStars() {
		ServerLevel self = (ServerLevel) (Object) this;
		StarState state = self.getDataStorage().get(nbt -> StarState.load(nbt, self.getSeed()), Starcaller.STATE_KEY);
		return state != null ? state.stars : List.of();
	}

	@Override
	public void starcaller$setGeneratorValues(long seed, int iterations) {
		throw new UnsupportedOperationException("Server generator values are set through the config!");
	}
}
