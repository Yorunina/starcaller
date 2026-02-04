package folk.sisby.starcaller.network;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.duck.StarcallerLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Starcaller.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, S2CStarcallerPacket.class, S2CStarcallerPacket::encode, S2CStarcallerPacket::new, S2CStarcallerPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

	public static void syncStarGrounded(Player cause, ServerLevel level, Star star) {
		int starIndex = ((StarcallerLevel) level).starcaller$getStars().indexOf(star);
		S2CStarcallerPacket packet = new S2CStarcallerPacket(Map.of(starIndex, star.groundedTick));
		level.getPlayers((pPlayer) -> true).forEach(player -> {
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		});
	}

	public static void syncStarColor(Player cause, ServerLevel level, Star star) {
		int starIndex = ((StarcallerLevel) level).starcaller$getStars().indexOf(star);
		S2CStarcallerPacket packet = new S2CStarcallerPacket(Map.of(starIndex, star.color), true);
		level.getPlayers((pPlayer) -> true).forEach(player -> {
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		});
	}

	public static void sendInitialStarState(ServerPlayer player) {
		if (player.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level instanceof StarcallerLevel scw) {
			Map<Integer, Long> groundedMap = new Int2ObjectArrayMap<>();
			Map<Integer, Integer> colorMap = new Int2ObjectArrayMap<>();
			for (Star star : scw.starcaller$getStars()) {
				if (star.groundedTick != Star.DEFAULT_GROUNDED_TICK) {
					groundedMap.put(scw.starcaller$getStars().indexOf(star), star.groundedTick);
				}
				if (star.color != Star.DEFAULT_COLOR) {
					colorMap.put(scw.starcaller$getStars().indexOf(star), star.color);
				}
			}
			S2CStarcallerPacket packet = new S2CStarcallerPacket(scw.starcaller$getSeed(), scw.starcaller$getIterations(), groundedMap, colorMap);
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		}
	}
}
