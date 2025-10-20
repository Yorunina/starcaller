package folk.sisby.starcaller.item;

import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.util.StarUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SpearItem extends Item {
	public static final int DRAW_TIME = 10;
	public static final int COOLDOWN_TICKS = 10;

	public SpearItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(itemStack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (!level.isClientSide) {
			return;
		}
		if (entity instanceof Player player && isSelected) {
			Optional<StarcallerLevel> optLevel = StarcallerLevel.of(level);
			Starcaller.LOGGER.info("SpearItem Tick");
			if (optLevel.isEmpty()) return;
			StarcallerLevel scw = optLevel.get();
			Starcaller.LOGGER.info("SpearItem Tick 1");
			if (player.pick(12 * 16, 1.0F, false).getType() == HitResult.Type.MISS) {
				Starcaller.LOGGER.info("SpearItem Tick 2");
				List<Star> stars = scw.starcaller$getStars();
				Vec3 cursorCoordinates = StarUtil.correctForSkyAngle(StarUtil.getStarCursor(player.getYHeadRot(), player.getXRot()), level.getSunAngle(1.0F));
				Optional<Star> closestStarOpt = stars.stream().filter(s -> s.groundedTick == -1 || s.groundedTick + StarcallerConfig.starGroundedTicks < level.getDayTime()).min(Comparator.comparingDouble(s -> s.pos.distanceToSqr(cursorCoordinates)));
				if (closestStarOpt.isPresent() && cursorCoordinates.distanceToSqr(closestStarOpt.get().pos) < 4 * 4) {
					Star closestStar = closestStarOpt.get();
					int i = stars.indexOf(closestStar);
					player.displayClientMessage(Component.translatable("messages.starcaller.star.info", Component.translatable("star.starcaller.overworld.%s".formatted(i)).setStyle(Style.EMPTY.applyFormat(ChatFormatting.ITALIC).withColor(closestStar.color))), true);
				}
			}
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
		if (livingEntity instanceof Player player) {
			int j = this.getUseDuration(stack) - timeCharged;
			if (j >= DRAW_TIME) {
				level.playSound(null, player, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
				if (level instanceof StarcallerLevel scw) {
					if (player.pick(12 * 16, 1.0F, false).getType() == HitResult.Type.MISS) {
						Vec3 cursorCoordinates = StarUtil.correctForSkyAngle(StarUtil.getStarCursor(player.getYHeadRot(), player.getXRot()), level.getSunAngle(1.0F));
						Optional<Star> closestStarOpt = scw.starcaller$getStars().stream().min(Comparator.comparingDouble(s -> s.pos.distanceToSqr(cursorCoordinates)));
						if (closestStarOpt.isPresent() && cursorCoordinates.distanceToSqr(closestStarOpt.get().pos) < 4 * 4) {
							Star closestStar = closestStarOpt.get();
							int starIndex = scw.starcaller$getStars().indexOf(closestStar);
							scw.starcaller$groundStar(player, closestStar);
							player.getInventory().add(StardustItem.fromStar(starIndex, closestStar));
						}
					}
				}
				player.awardStat(Stats.ITEM_USED.get(this));
				player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
			}
		}
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}
}
