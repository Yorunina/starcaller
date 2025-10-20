package folk.sisby.starcaller.util;

import folk.sisby.starcaller.Star;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class StarUtil {
    public static List<Star> generateStars(long seed, int iterations) {
        RandomSource random = RandomSource.create(seed);
        List<Star> list = new ArrayList<>();

        for (int i = 0; i < iterations; ++i) {
            double d = random.nextFloat() * 2.0F - 1.0F;
            double e = random.nextFloat() * 2.0F - 1.0F;
            double f = random.nextFloat() * 2.0F - 1.0F;
            random.nextFloat(); // Match the random usage from the original method
            double r2 = d * d + e * e + f * f;
            if (r2 < 1.0 && r2 > 0.01) {
                random.nextDouble(); // Match the random usage from the original method
                double ir2 = 1.0 / Math.sqrt(r2);
                d *= ir2;
                e *= ir2;
                f *= ir2;
                list.add(new Star(new Vec3(-f * 100.0, e * 100.0, d * 100.0)));
            }
        }
        return list;
    }

    public static int getGeneratorIterations(long seed, int limit) {
		RandomSource random = RandomSource.create(seed);
        int stars = 0;

        for (int i = 0; i < limit * 3; ++i) {
            double d = random.nextFloat() * 2.0F - 1.0F;
            double e = random.nextFloat() * 2.0F - 1.0F;
            double f = random.nextFloat() * 2.0F - 1.0F;
            random.nextFloat();
            double r2 = d * d + e * e + f * f;
            if (r2 < 1.0 && r2 > 0.01) {
                random.nextDouble();
                stars++;
                if (stars >= limit) return i;
            }
        }
        return limit * 3;
    }

    public static Vec3 getStarCursor(float yawDegrees, float pitchDegrees) {
        double azimuth = (yawDegrees) * Math.PI / 180;
        double inclination = (90.0F + pitchDegrees) * Math.PI / 180;
        return new Vec3(-1 * Math.sin(azimuth) * Math.sin(inclination), Math.cos(inclination), Math.cos(azimuth) * Math.sin(inclination)).scale(100F);
    }

    public static Vec3 correctForSkyAngle(Vec3 starCursor, float skyAngle) {
        double skyboxRotation = (1.0F - skyAngle) * 2 * Math.PI;
        return new Vec3(
                Math.cos(skyboxRotation) * starCursor.x - Math.sin(skyboxRotation) * starCursor.y,
                Math.sin(skyboxRotation) * starCursor.x + Math.cos(skyboxRotation) * starCursor.y,
                starCursor.z
        );
    }
}
