package folk.sisby.starcaller.mixin.client;

import folk.sisby.starcaller.Starcaller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class MixinInGameHud {

	private static final ResourceLocation SPEAR_CROSSHAIR_TEXTURE = Starcaller.id("textures/gui/sprites/hud/crosshair.png");
    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"), index = 0)
    public ResourceLocation useThrowCrosshair(ResourceLocation original) {
		Player player = Minecraft.getInstance().player;
        if (player.getMainHandItem().is(Starcaller.SPEAR.get()) || player.getOffhandItem().is(Starcaller.SPEAR.get())) {
            return SPEAR_CROSSHAIR_TEXTURE;
        }
        return original;
    }
}
