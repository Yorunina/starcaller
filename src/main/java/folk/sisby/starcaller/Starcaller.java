package folk.sisby.starcaller;

import com.mojang.logging.LogUtils;
import folk.sisby.starcaller.client.StardustTickerImpl;
import folk.sisby.starcaller.client.StarcallerClient;
import folk.sisby.starcaller.duck.StarcallerLevel;
import folk.sisby.starcaller.item.SpearItem;
import folk.sisby.starcaller.item.StardustItem;
import folk.sisby.starcaller.network.PacketHandler;
import folk.sisby.starcaller.network.S2CStarcallerPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Map;

@Mod(Starcaller.ID)
public class Starcaller {
	public static final String ID = "starcaller";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final String STATE_KEY = "starcaller_stars";

	// Deferred Register for Items
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

	// Item Registry Objects
	public static final RegistryObject<Item> STARDUST = ITEMS.register("stardust", () -> new StardustItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> SPEAR = ITEMS.register("spear", () -> new SpearItem(new Item.Properties().stacksTo(1)));


	public static StardustTicker TICKER;

	public Starcaller(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();

		// Register DeferredRegister to the mod event bus
		ITEMS.register(modEventBus);

		// Register event handlers
		modEventBus.addListener(this::addCreative);
		modEventBus.addListener(this::clientSetup);

		PacketHandler.register();

		MinecraftForge.EVENT_BUS.register(this);


		LOGGER.info("[Starcaller] Initialized.");
	}

	private void clientSetup(final FMLClientSetupEvent event) {
		TICKER = new StardustTickerImpl();

		event.enqueueWork(() -> {
			// 注册模型谓词 (Model Predicates)
			ItemProperties.register(SPEAR.get(), ResourceLocation.parse("throwing"),
				(stack, world, entity, i) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
			ItemProperties.register(STARDUST.get(), ResourceLocation.parse("star_expired"),
				(stack, world, entity, i) -> StarcallerClient.isStardustExpired(stack, world));
		});

		MinecraftForge.EVENT_BUS.register(StarcallerClient.class);
	}


	private void addCreative(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(SPEAR.get());
		}
	}

	@SubscribeEvent
	public void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
			level.getDataStorage().computeIfAbsent(
				(nbt) -> StarState.load(nbt, level.getSeed()),
				() -> new StarState(level.getSeed()),
				STATE_KEY
			);
		}
	}

	@SubscribeEvent
	public void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && event.level instanceof StarcallerLevel scw) {
			for (Star star : scw.starcaller$getStars()) {
				if (level.getDayTime() > star.groundedTick + StarcallerConfig.starGroundedTicks) {
					star.groundedTick = -1;
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sendInitialStarState(player);
		}
	}

	@SubscribeEvent
	public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sendInitialStarState(player);
		}
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	public static void groundStar(Player cause, ServerLevel level, Star star) {
		updateStarGrounded(cause, level, star, level.getDayTime());
	}

	public static void freeStar(Player cause, ServerLevel level, Star star) {
		updateStarGrounded(cause, level, star, -1);
	}

	private static void updateStarGrounded(Player cause, ServerLevel level, Star star, long time) {
		if (star.groundedTick != time) {
			star.groundedTick = time;
			level.getDataStorage().get(nbt -> StarState.load(nbt, level.getSeed()), STATE_KEY).setDirty();
			syncStarGrounded(cause, level, star);
		}
	}

	public static void colorStar(Player cause, ServerLevel level, Star star, int color) {
		if (star.color != color) {
			star.color = color;
			syncStarColor(cause, level, star);
		}
		var nameColor = cause.getDisplayName().getStyle().getColor();
		star.editor = cause.getDisplayName().getString();
		star.editorColor = nameColor != null ? nameColor.getValue() : 0xFFFFFF;
		level.getDataStorage().get(nbt -> StarState.load(nbt, level.getSeed()), STATE_KEY).setDirty();
	}

	public static void sendInitialStarState(ServerPlayer player) {
		if (player.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level instanceof StarcallerLevel scw) {
			Map<Integer, Long> groundedMap = new Int2ObjectArrayMap<>();
			Map<Integer, Integer> colorMap = new Int2ObjectArrayMap<>();
			for (Star star : scw.starcaller$getStars()) {
				if (star.groundedTick != Star.DEFAULT_GROUNDED_TICK) {
					groundedMap.put(scw.starcaller$getStars().indexOf(star), star.groundedTick);
				}
				if (star.color != Star.DEFAULT_COLOR) {
					colorMap.put(scw.starcaller$getStars().indexOf(star), star.color);
				}
			}
			S2CStarcallerPacket packet = new S2CStarcallerPacket(scw.starcaller$getSeed(), scw.starcaller$getIterations(), groundedMap, colorMap);
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		}
	}

	public static void syncStarGrounded(Player cause, ServerLevel level, Star star) {
		int starIndex = ((StarcallerLevel) level).starcaller$getStars().indexOf(star);
		S2CStarcallerPacket packet = new S2CStarcallerPacket(Map.of(starIndex, star.groundedTick));
		level.getPlayers((pPlayer) -> true).forEach(player -> {
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		});
	}

	public static void syncStarColor(Player cause, ServerLevel level, Star star) {
		int starIndex = ((StarcallerLevel) level).starcaller$getStars().indexOf(star);
		S2CStarcallerPacket packet = new S2CStarcallerPacket(Map.of(starIndex, star.color), true);
		level.getPlayers((pPlayer) -> true).forEach(player -> {
			PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
		});
	}

	@SubscribeEvent
	public void onTooltip(ItemTooltipEvent event) {
		if (event.getItemStack().is(STARDUST.get())) {
			event.getToolTip().removeIf(component ->
				component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatableContents &&
					translatableContents.getKey().equals("item.dyed")
			);
		}
	}

	@SubscribeEvent
	public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
		ItemStack stack = event.getCrafting();
		if (stack.is(STARDUST.get())) {
			Player player = event.getEntity();
			CompoundTag nbt = stack.getOrCreateTag();

			var name = player.getDisplayName();
			var nameColor = name.getStyle().getColor();
			nbt.putString(StardustItem.KEY_EDITOR, name.getString());
			nbt.putInt(StardustItem.KEY_EDITOR_COLOR, nameColor != null ? nameColor.getValue() : 0xFFFFFF);
		}
	}
}
