package folk.sisby.starcaller.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.starcaller.item.StardustItem;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(ArmorDyeRecipe.class)
public class MixinArmorDyeRecipe {
	@ModifyReturnValue(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"))
	public boolean dontMatchBadStardust(boolean original, CraftingContainer inventory, Level world) {
		if (original) {
			for (int i = 0; i < inventory.getContainerSize(); ++i) {
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty()) {
					if (stack.getItem() instanceof StardustItem) {
						Long remainingTicks = StardustItem.getRemainingTicks(stack, world);
						return remainingTicks != null && remainingTicks > 0 && (world.isClientSide || Objects.equals(remainingTicks, StardustItem.getWorldRemainingTicks(stack, world)));
					}
				}
			}
		}
		return original;
	}

	@ModifyReturnValue(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"))
	public boolean dontMatchDarkDyes(boolean original, CraftingContainer inventory, Level world) {
		if (original) {
			boolean hasStardust = false;
			boolean hasDarkDye = false;
			for (int i = 0; i < inventory.getContainerSize(); ++i) {
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty()) {
					if (stack.getItem() instanceof StardustItem) {
						hasStardust = true;
					} else if (stack.getItem() instanceof DyeItem di && (
						di.getDyeColor() == DyeColor.BLACK ||
							di.getDyeColor() == DyeColor.BROWN ||
							di.getDyeColor() == DyeColor.GRAY
					)) {
						hasDarkDye = true;
					}
				}
			}
			return !(hasStardust && hasDarkDye);
		}
		return original;
	}
}
