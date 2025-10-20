package folk.sisby.starcaller.client;

import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.StardustTicker;
import folk.sisby.starcaller.item.StardustItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.joml.Math;

import static folk.sisby.starcaller.item.StardustItem.KEY_STAR_GROUNDED_TICK;

public class StardustTickerImpl implements StardustTicker {
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        Long remainingTicks = StardustItem.getRemainingTicks(stack, Minecraft.getInstance().level);
        return remainingTicks != null && remainingTicks > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains(KEY_STAR_GROUNDED_TICK) && Minecraft.getInstance().level != null) {
            long remainingTicks = StarcallerConfig.starGroundedTicks + nbt.getLong(KEY_STAR_GROUNDED_TICK) - Minecraft.getInstance().level.getDayTime();
            return (int) Math.clamp(0, 13.0F, (remainingTicks * 13.0F / StarcallerConfig.starGroundedTicks));

        }
        return StardustTicker.super.getItemBarStep(stack);
    }
}
