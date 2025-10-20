package folk.sisby.starcaller;

import folk.sisby.starcaller.util.StarUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;

public class StarState extends SavedData {
    public static final String KEY_SEED = "seed";
    public static final String KEY_LIMIT = "limit";
    public static final String KEY_STARS = "stars";

    public long seed;
    public int limit;
    public int iterations; // Not persistent, just convenient for the client.
    public List<Star> stars;

    public StarState(long worldSeed) {
        this.seed = (StarcallerConfig.starSeed != -1 ? StarcallerConfig.starSeed : worldSeed);
        this.limit = StarcallerConfig.starLimit;
        this.iterations = StarUtil.getGeneratorIterations(seed, limit);
        this.stars = StarUtil.generateStars(this.seed, this.iterations);
        setDirty();
    }

    public StarState(long seed, int limit, int iterations, List<Star> stars) {
        this.seed = seed;
        this.limit = limit;
        this.iterations = iterations;
        this.stars = stars;
    }

    public static StarState load(CompoundTag nbt, long worldSeed) {
        long seed = nbt.contains(KEY_SEED) ? nbt.getLong(KEY_SEED) : (StarcallerConfig.starSeed != -1 ? StarcallerConfig.starSeed : worldSeed);
        int limit = nbt.contains(KEY_LIMIT) ? nbt.getInt(KEY_LIMIT) : StarcallerConfig.starLimit;
        int iterations = StarUtil.getGeneratorIterations(seed, limit);
        List<Star> stars = StarUtil.generateStars(seed, iterations);
        int i = 0;
        for (Tag starElement : nbt.getList(KEY_STARS, CompoundTag.TAG_COMPOUND)) {
            if (i < stars.size() && starElement instanceof CompoundTag starCompound) {
                stars.get(i).readNbt(starCompound);
            }
            i++;
        }
        return new StarState(seed, limit, iterations, stars);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag nbtList = new ListTag();
        for (Star star : stars) {
            nbtList.add(star.toNbt());
        }
        nbt.putLong(KEY_SEED, this.seed);
        nbt.putInt(KEY_LIMIT, this.limit);
        nbt.put(KEY_STARS, nbtList);
        return nbt;
    }
}
