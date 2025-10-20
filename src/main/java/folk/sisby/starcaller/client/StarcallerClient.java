package folk.sisby.starcaller.client;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.duck.StarcallerWorld;
import folk.sisby.starcaller.item.StardustItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public class StarcallerClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("starcaller_client");

    public static float isStardustExpired(ItemStack stack, @Nullable Level level) {
        return Objects.requireNonNullElse(StardustItem.getRemainingTicks(stack, level), 1L) <= 0 ? 1.0F : 0.0F;
    }

    public static void groundStar(ClientLevel world, Star star) {
        star.groundedTick = world.getGameTime();
        reloadStars(world);
    }

    public static void freeStar(ClientLevel world, Star star) {
        star.groundedTick = -1;
        reloadStars(world);
    }

    public static void colorStar(Player cause, ClientLevel world, Star star, int color) {
        star.color = color;
        TextColor nameColor = cause.getDisplayName().getStyle().getColor();
        star.editor = cause.getDisplayName().getString();
        star.editorColor = nameColor != null ? nameColor.getValue() : 0xFFFFFF;
        reloadStars(world);
    }

    public static void reloadStars(ClientLevel world) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().levelRenderer.starBuffer != null) {
                Minecraft.getInstance().levelRenderer.createStars();
            }
        });
    }

    @Mod.EventBusSubscriber(modid = "starcaller", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ClientLevel level = Minecraft.getInstance().level;
                if (level instanceof StarcallerLevel scw) {
                    List<Star> stars = scw.starcaller$getStars();
                    boolean reloadStars = false;
                    for (Star star : stars) {
                        if (star.groundedTick != -1 && star.groundedTick + StarcallerConfig.starGroundedTicks <= level.getGameTime()) {
                            star.groundedTick = -1;
                            reloadStars = true;
                        }
                    }
                    if (reloadStars) reloadStars(level);
                }
            }
        }
    }

    public static void handleInitialState(long seed, int iterations, Map<Integer, Long> groundedMap, Map<Integer, Integer> colorMap) {
        if (Minecraft.getInstance().level instanceof StarcallerLevel scw) {
            scw.starcaller$setGeneratorValues(seed, iterations);
            handleGroundedUpdate(groundedMap);
            handleColorUpdate(colorMap);
            reloadStars(Minecraft.getInstance().level);
        }
    }

    public static void handleGroundedUpdate(Map<Integer, Long> groundedMap) {
        if (Minecraft.getInstance().level instanceof StarcallerLevel scw) {
            List<Star> stars = scw.starcaller$getStars();
            groundedMap.forEach((index, tick) -> { if (index < stars.size()) stars.get(index).groundedTick = tick; });
            reloadStars(Minecraft.getInstance().level);
        }
    }

    public static void handleColorUpdate(Map<Integer, Integer> colorMap) {
        if (Minecraft.getInstance().level instanceof StarcallerLevel scw) {
            List<Star> stars = scw.starcaller$getStars();
            colorMap.forEach((index, color) -> { if (index < stars.size()) stars.get(index).color = color; });
            reloadStars(Minecraft.getInstance().level);
        }
    }
}
