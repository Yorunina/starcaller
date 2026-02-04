package folk.sisby.starcaller.mixin.client;


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.duck.StarcallerLevel;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class MixinLevelRenderer {
    @Shadow private @Nullable ClientLevel level;
    @Unique private int starIndex = -1;

    @Inject(method = "drawStars", at = @At("HEAD"))
    public void resetStarDebug(BufferBuilder bufferBuilder, CallbackInfoReturnable<BufferBuilder> cir) {
        starIndex = -1;
    }

    @Inject(method = "drawStars", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextDouble()D"))
    public void countSuccessfulStars(BufferBuilder bufferBuilder, CallbackInfoReturnable<BufferBuilder> cir) {
        starIndex++;
    }

    @ModifyArg(method = "drawStars", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create(J)Lnet/minecraft/util/RandomSource;"))
    public long useCustomSeed(long original) {
        if (level instanceof StarcallerLevel scw) {
            return scw.starcaller$getSeed();
        }
        return original;
    }

	@ModifyExpressionValue(method = "drawStars", at = @At(value = "CONSTANT", args = "intValue=1500"))
    public int useCustomLimit(int constant) {
        if (level instanceof StarcallerLevel scw) {
            return scw.starcaller$getIterations();
        }
        return constant;
    }

    @ModifyReceiver(method = "drawStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;endVertex()V"))
    public VertexConsumer setColorPerStar(VertexConsumer instance, BufferBuilder builder) {
        int color = Star.DEFAULT_COLOR;
        if (level instanceof StarcallerLevel scw) {
            List<Star> stars = scw.starcaller$getStars();
            if (starIndex < stars.size()) {
                Star star = stars.get(starIndex);
                boolean grounded = star.groundedTick != 0 && level.getDayTime() - star.groundedTick < StarcallerConfig.starGroundedTicks;
                if (grounded) {
                    color = 0x00FFFF00;
                } else {
                    color = star.color;
                }
            }
        }
        return instance.color(color);
    }

    @ModifyArg(method = "createStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"), index = 0)
    public Supplier<ShaderInstance> useColorSupplier(Supplier<ShaderInstance> supplier) {
        return GameRenderer::getPositionColorShader;
    }

    @ModifyArg(method = "drawStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"), index = 1)
    public VertexFormat useColorBuffer(VertexFormat vertexFormat) {
        return DefaultVertexFormat.POSITION_COLOR;
    }

	@ModifyArg(method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", ordinal = 1), index = 2)
	public ShaderInstance useColorProgram(ShaderInstance shaderProgram) {
		return GameRenderer.getPositionColorShader();
	}
}
