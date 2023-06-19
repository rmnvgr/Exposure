package io.github.mortuusars.exposure;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.camera.film.FilmType;
import io.github.mortuusars.exposure.menu.CameraMenu;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.FilmItem;
import io.github.mortuusars.exposure.storage.ExposureStorage;
import io.github.mortuusars.exposure.storage.IExposureStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Exposure.ID)
public class Exposure {
    public static final String ID = "exposure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Exposure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Items.ITEMS.register(modEventBus);
        MenuTypes.MENU_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static IExposureStorage getStorage() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER ?
                ExposureStorage.SERVER : ExposureStorage.CLIENT;
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return new ResourceLocation(ID, path);
    }

    public static class Items {
        private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

        public static final RegistryObject<CameraItem> CAMERA = ITEMS.register("camera",
                () -> new CameraItem(new Item.Properties().stacksTo(1)
                        .tab(CreativeModeTab.TAB_TOOLS)));

        public static final RegistryObject<FilmItem> SMALL_FORMAT_BLACK_AND_WHITE_FILM = ITEMS.register("small_format_black_and_white_film",
                () -> new FilmItem(FilmType.BLACK_AND_WHITE, 192, 36, new Item.Properties()
                        .stacksTo(16)
                        .tab(CreativeModeTab.TAB_TOOLS)));

        public static final RegistryObject<FilmItem> SMALL_FORMAT_COLOR_FILM = ITEMS.register("small_format_color_film",
                () -> new FilmItem(FilmType.COLOR, 192, 36, new Item.Properties()
                        .stacksTo(16)
                        .tab(CreativeModeTab.TAB_TOOLS)));

        public static final RegistryObject<FilmItem> LARGE_FORMAT_BLACK_AND_WHITE_FILM = ITEMS.register("large_format_black_and_white_film",
                () -> new FilmItem(FilmType.BLACK_AND_WHITE, 384, 8, new Item.Properties()
                        .stacksTo(16)
                        .tab(CreativeModeTab.TAB_TOOLS)));

        public static final RegistryObject<FilmItem> LARGE_FORMAT_COLOR_FILM = ITEMS.register("large_format_color_film",
                () -> new FilmItem(FilmType.COLOR, 384, 8, new Item.Properties()
                        .stacksTo(16)
                        .tab(CreativeModeTab.TAB_TOOLS)));
    }

    public static class MenuTypes {
        private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);

        public static final RegistryObject<MenuType<CameraMenu>> CAMERA = MENU_TYPES
                .register("camera", () -> IForgeMenuType.create(CameraMenu::fromBuffer));
    }
}
