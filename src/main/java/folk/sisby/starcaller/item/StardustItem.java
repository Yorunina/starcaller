package folk.sisby.starcaller.item;


import folk.sisby.starcaller.Star;
import folk.sisby.starcaller.Starcaller;
import folk.sisby.starcaller.StarcallerConfig;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StardustItem extends Item {
	public static final String KEY_STAR_INDEX = "star";
	public static final String KEY_STAR_GROUNDED_TICK = "groundedTick";
	public static final String KEY_EDITOR = "editor";
	public static final String KEY_EDITOR_COLOR = "editorColor";

	public static final String DISPLAY_KEY = "display";
	public static final String COLOR_KEY = "color";

	public StardustItem(Properties settings) {
		super(settings);
	}

	public static ItemStack fromStar(int index, Star star) {
		ItemStack stack = Starcaller.STARDUST.get().getDefaultInstance().copy();
		CompoundTag nbt = stack.getOrCreateTag();
		nbt.putInt(KEY_STAR_INDEX, index);
		nbt.putLong(KEY_STAR_GROUNDED_TICK, star.groundedTick);
		CompoundTag display = new CompoundTag();
		display.putInt(COLOR_KEY, star.color);
		nbt.put(DISPLAY_KEY, display);
		if (star.editor != null) nbt.putString(KEY_EDITOR, star.editor);
		nbt.putInt(KEY_EDITOR_COLOR, star.editorColor);
		return stack;
	}

	public static @Nullable Integer getStarIndex(ItemStack stack) {
		return stack.getTag() != null && stack.getTag().contains(KEY_STAR_INDEX) ? stack.getTag().getInt(KEY_STAR_INDEX) : null;
	}

	public static @Nullable Long getGroundedTick(ItemStack stack) {
		return stack.getTag() != null && stack.getTag().contains(KEY_STAR_GROUNDED_TICK) ? stack.getTag().getLong(KEY_STAR_GROUNDED_TICK) : null;
	}

	public static @Nullable MutableComponent getEditor(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(KEY_EDITOR) && tag.contains(KEY_EDITOR_COLOR) ? Component.literal(tag.getString(KEY_EDITOR)).setStyle(Style.EMPTY.withColor(tag.getInt(KEY_EDITOR_COLOR))) : null;
	}

	public static @Nullable Star getStar(ItemStack stack, Level level) {
		Integer starIndex = getStarIndex(stack);
		if (level instanceof StarcallerLevel scw && starIndex != null) {
			List<Star> stars = scw.starcaller$getStars();
			if (starIndex < stars.size()) {
				return stars.get(starIndex);
			}
		}
		return null;
	}

	public static @Nullable Long getRemainingTicks(ItemStack stack, Level level) {
		Long groundedTick = getGroundedTick(stack);
		return level != null && groundedTick != null ? StarcallerConfig.starGroundedTicks + groundedTick - level.getDayTime() : null;
	}

	public static @Nullable Long getWorldRemainingTicks(ItemStack stack, Level level) {
		Star star = getStar(stack, level);
		return star != null ? StarcallerConfig.starGroundedTicks + star.groundedTick - level.getDayTime() : null;
	}

	public static Component getCountdown(long remainingTicks) {
		return Component.translatable("item.starcaller.stardust.countdown", Component.literal(String.valueOf((int) Math.ceil(remainingTicks / 20.0F))).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.DARK_GRAY);
	}

	@Override
	public Component getName(ItemStack stack) {
		MutableComponent name = Component.translatable(this.getDescriptionId(stack));
		Integer starIndex = getStarIndex(stack);
		if (starIndex != null) {
			name = Component.translatable("star.starcaller.overworld.%s".formatted(starIndex)).withStyle(ChatFormatting.ITALIC);
		}
		if (hasColor(stack)) {
			name = name.withStyle(style -> style.withColor(getColor(stack)));
		}
		return starIndex != null ? Component.translatable("item.starcaller.stardust.named", name).withStyle(ChatFormatting.GRAY) : name;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipContext) {
		super.appendHoverText(stack, level, list, tooltipContext);
		Long remainingTicks = getRemainingTicks(stack, level);
		if (remainingTicks != null) {
			if (remainingTicks < 0) {
				list.clear();
				return;
			}
			list.add(getCountdown(remainingTicks));
		}
		MutableComponent editor = getEditor(stack);
		if (editor != null) {
			list.add(Component.translatable("item.starcaller.stardust.editor", editor).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return Starcaller.TICKER.isItemBarVisible(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Starcaller.TICKER.getItemBarStep(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		if (hasColor(stack)) {
			return getColor(stack);
		}
		return 0xFFFFFF;
	}

	private void dissipateEffect(ItemStack stack, Level level, Vec3 pos, int count) {
		if (level instanceof ServerLevel sw) {
			sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 3.0F, 2.0F);
			sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.8F, 2.0F);
			sw.sendParticles(new DustParticleOptions(ColorUtil.colorToComponents(getBarColor(stack)), 0.6f), pos.x, pos.y, pos.z, count, 0.5f, 0.125f, 0.5f, 0); // shoutouts to Yttr, obviously
		}
	}

	private void forceDissipate(ItemStack stack, Level level, Vec3 pos) {
		dissipateEffect(stack, level, pos, 40);
		Star star = getStar(stack, level);
		if (level instanceof StarcallerLevel scw && star != null) {
			scw.starcaller$freeStar(null, star);
		}
	}

	private void tick(ItemStack stack, Level level, Vec3 pos) {
		Long remainingTicks = getRemainingTicks(stack, level);
		if (remainingTicks == null || remainingTicks <= 0) {
			dissipateEffect(stack, level, pos, 20);
			stack.shrink(stack.getCount());
		}
	}

	@Override
	public void onCraftedBy(ItemStack stack, Level level, Player player) {
		super.onCraftedBy(stack, level, player);
		Long remainingTicks = getRemainingTicks(stack, level);
		Star star = getStar(stack, level);
		if (level instanceof StarcallerLevel scw && star != null && remainingTicks != null && remainingTicks > 0) {
			if (!level.isClientSide && !Objects.equals(remainingTicks, StardustItem.getWorldRemainingTicks(stack, level))) return;
			scw.starcaller$colorStar(player, star, 0xFF000000 | getColor(stack));
		}
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player) {
		tick(stack, player.level(), player.getEyePosition());
		return super.overrideStackedOnOther(stack, slot, clickAction, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickAction, Player player, net.minecraft.world.entity.SlotAccess cursorStackReference) {
		tick(stack, player.level(), player.getEyePosition());
		return super.overrideOtherStackedOnMe(stack, otherStack, slot, clickAction, player, cursorStackReference);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		tick(stack, level, entity.getEyePosition());
		Long remainingTicks = getRemainingTicks(stack, level);
		if (level.isClientSide && selected && entity instanceof Player player && remainingTicks != null) {
			player.displayClientMessage(Component.translatable("item.starcaller.stardust.status", getCountdown(remainingTicks)).withStyle(ChatFormatting.AQUA), true);
		}
	}


	public int getColor(ItemStack stack) {
		CompoundTag compoundTag = stack.getTagElement(DISPLAY_KEY);
		if (compoundTag != null && compoundTag.contains(COLOR_KEY, 99)) {
			return compoundTag.getInt(COLOR_KEY);
		}
		return 0xFFFFFF; // Default color
	}

	public boolean hasColor(ItemStack stack) {
		CompoundTag compoundTag = stack.getTagElement(DISPLAY_KEY);
		return compoundTag != null && compoundTag.contains(COLOR_KEY, 99);
	}

	public void setColor(ItemStack stack, int color) {
		stack.getOrCreateTagElement(DISPLAY_KEY).putInt(COLOR_KEY, color);
	}

	public void clearColor(ItemStack stack) {
		CompoundTag compoundTag = stack.getTagElement(DISPLAY_KEY);
		if (compoundTag != null && compoundTag.contains(COLOR_KEY)) {
			compoundTag.remove(COLOR_KEY);
		}
	}
}
