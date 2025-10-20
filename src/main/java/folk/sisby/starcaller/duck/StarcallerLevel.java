package folk.sisby.starcaller.duck;

import folk.sisby.starcaller.Star;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface StarcallerLevel {
    long starcaller$getSeed();
    int starcaller$getIterations();
    List<Star> starcaller$getStars();

    void starcaller$setGeneratorValues(long seed, int iterations);

    void starcaller$groundStar(Player cause, Star star);

    void starcaller$freeStar(Player cause, Star star);

    void starcaller$colorStar(Player cause, Star star, int color);
}

