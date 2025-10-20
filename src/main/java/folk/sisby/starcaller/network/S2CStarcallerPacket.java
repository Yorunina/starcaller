package folk.sisby.starcaller.network;

import folk.sisby.starcaller.client.StarcallerClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 一个统一的数据包，用于处理所有从服务端到客户端的星星状态同步。
 */
public class S2CStarcallerPacket {

    private final PacketType type;
    private long seed;
    private int iterations;
    private Map<Integer, Long> groundedMap;
    private Map<Integer, Integer> colorMap;

    // 用于发送初始状态
    public S2CStarcallerPacket(long seed, int iterations, Map<Integer, Long> groundedMap, Map<Integer, Integer> colorMap) {
        this.type = PacketType.INITIAL_STATE;
        this.seed = seed;
        this.iterations = iterations;
        this.groundedMap = groundedMap;
        this.colorMap = colorMap;
    }

    // 用于发送落地状态更新
    public S2CStarcallerPacket(Map<Integer, Long> groundedMap) {
        this.type = PacketType.UPDATE_GROUNDED;
        this.groundedMap = groundedMap;
    }

    // 用于发送颜色更新
    public S2CStarcallerPacket(Map<Integer, Integer> colorMap, boolean isColor) {
        this.type = PacketType.UPDATE_COLORS;
        this.colorMap = colorMap;
    }

    // 解码
    public S2CStarcallerPacket(FriendlyByteBuf buf) {
        this.type = buf.readEnum(PacketType.class);
        if (this.type == PacketType.INITIAL_STATE) {
            this.seed = buf.readLong();
            this.iterations = buf.readInt();
        }
        if (this.type == PacketType.INITIAL_STATE || this.type == PacketType.UPDATE_GROUNDED) {
            this.groundedMap = buf.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readLong);
        }
        if (this.type == PacketType.INITIAL_STATE || this.type == PacketType.UPDATE_COLORS) {
            this.colorMap = buf.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readInt);
        }
    }

    // 编码
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        if (this.type == PacketType.INITIAL_STATE) {
            buf.writeLong(this.seed);
            buf.writeInt(this.iterations);
        }
        if (this.type == PacketType.INITIAL_STATE || this.type == PacketType.UPDATE_GROUNDED) {
            buf.writeMap(this.groundedMap, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeLong);
        }
        if (this.type == PacketType.INITIAL_STATE || this.type == PacketType.UPDATE_COLORS) {
            buf.writeMap(this.colorMap, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeInt);
        }
    }

    // 处理
    public static void handle(S2CStarcallerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端主线程处理
            switch (msg.type) {
                case INITIAL_STATE -> StarcallerClient.handleInitialState(msg.seed, msg.iterations, msg.groundedMap, msg.colorMap);
                case UPDATE_GROUNDED -> StarcallerClient.handleGroundedUpdate(msg.groundedMap);
                case UPDATE_COLORS -> StarcallerClient.handleColorUpdate(msg.colorMap);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    enum PacketType {
        INITIAL_STATE,
        UPDATE_GROUNDED,
        UPDATE_COLORS
    }
}