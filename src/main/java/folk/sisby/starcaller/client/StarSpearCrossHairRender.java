package folk.sisby.starcaller.client;

import folk.sisby.starcaller.Starcaller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Starcaller.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StarSpearCrossHairRender {

    public static final ResourceLocation CROSSHAIR_TEXTURE = Starcaller.id("textures/gui/sprites/hud/crosshair.png");

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiOverlayEvent.Pre event) {
        // 检查当前是否正在渲染准星
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            // 检查玩家是否手持长矛
            if (player != null && (player.getMainHandItem().is(Starcaller.SPEAR.get()) || player.getOffhandItem().is(Starcaller.SPEAR.get()))) {
                // 取消原版准星的渲染
                event.setCanceled(true);

                // 渲染我们自己的准星
                GuiGraphics guiGraphics = event.getGuiGraphics();
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();
                int crosshairSize = 15;

                int x = (screenWidth - crosshairSize) / 2;
                int y = (screenHeight - crosshairSize) / 2;

                guiGraphics.blit(CROSSHAIR_TEXTURE, x, y, 0, 0, crosshairSize, crosshairSize, crosshairSize, crosshairSize);
            }
        }
    }
}
