package folk.sisby.starcaller.network;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.Starcaller;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
}
