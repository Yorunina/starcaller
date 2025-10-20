package folk.sisby.starcaller.duck;

import folk.sisby.starcaller.Star;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public interface StarcallerLevel {
	static Optional<StarcallerLevel> of(Level level) {
		return level instanceof StarcallerLevel ? Optional.of((StarcallerLevel) level) : Optional.empty();
	}

	long starcaller$getSeed();
    int starcaller$getIterations();
    List<Star> starcaller$getStars();

    void starcaller$setGeneratorValues(long seed, int iterations);

    void starcaller$groundStar(Player cause, Star star);

    void starcaller$freeStar(Player cause, Star star);

    void starcaller$colorStar(Player cause, Star star, int color);
}

