package io.github.mortuusars.exposure.forge.event;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.capture.CaptureManager;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.client.ExposureClientReloadListener;
import io.github.mortuusars.exposure.client.MouseHandler;
import io.github.mortuusars.exposure.data.filter.Filters;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.gui.screen.ItemRenameScreen;
import io.github.mortuusars.exposure.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.gui.screen.album.LecternAlbumScreen;
import io.github.mortuusars.exposure.gui.screen.camera.CameraAttachmentsScreen;
import io.github.mortuusars.exposure.gui.screen.LightroomScreen;
import io.github.mortuusars.exposure.render.ItemFramePhotographRenderer;
import io.github.mortuusars.exposure.render.PhotographEntityRenderer;
import io.github.mortuusars.exposure.render.PhotographFrameEntityRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents {
    public static class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ExposureClient.init();
                MenuScreens.register(Exposure.MenuTypes.CAMERA.get(), CameraAttachmentsScreen::new);
                MenuScreens.register(Exposure.MenuTypes.ALBUM.get(), AlbumScreen::new);
                MenuScreens.register(Exposure.MenuTypes.LECTERN_ALBUM.get(), LecternAlbumScreen::new);
                MenuScreens.register(Exposure.MenuTypes.LIGHTROOM.get(), LightroomScreen::new);
                MenuScreens.register(Exposure.MenuTypes.ITEM_RENAME.get(), ItemRenameScreen::new);
            });
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Exposure.EntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
            event.registerEntityRenderer(Exposure.EntityTypes.PHOTOGRAPH_FRAME.get(), PhotographFrameEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(PhotographTooltip.class, photographTooltip -> photographTooltip);
        }

        @SubscribeEvent
        public static void registerResourceReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new ExposureClientReloadListener());
            event.registerReloadListener(new Filters.Loader());
        }

        @SubscribeEvent
        public static void registerModels(ModelEvent.RegisterAdditional event) {
            event.register(ExposureClient.Models.CAMERA_GUI);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL_STRIPPED);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM_STRIPPED);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE);
            event.register(ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE_STRIPPED);
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            ExposureClient.registerKeymappings(key -> {
                event.register(key);
                return key;
            });
        }
    }

    public static class ForgeBus {
        @SubscribeEvent
        public static void onLevelClear(LevelEvent.Unload event) {
            ExposureClient.getExposureRenderer().clearData();
        }

        @SubscribeEvent
        public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ExposureClient.getExposureStorage().clear();
        }

        @SubscribeEvent
        public static void renderOverlay(RenderGuiOverlayEvent.Pre event) {
            if (Viewfinder.isLookingThrough())
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            if (Viewfinder.handleMouseScroll(event.getScrollDelta() > 0d ? ZoomDirection.IN : ZoomDirection.OUT))
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void computeFOV(ViewportEvent.ComputeFov event) {
            if (!event.usedConfiguredFov())
                return;

            double prevFov = event.getFOV();
            double modifiedFov = Viewfinder.modifyFov(prevFov);
            if (prevFov != modifiedFov)
                event.setFOV(modifiedFov);
        }

        @SubscribeEvent
        public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
            if (MouseHandler.handleMouseButtonPress(event.getButton(), event.getAction(), event.getModifiers()))
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void renderItemFrameItem(RenderItemInFrameEvent event) {
            if (ItemFramePhotographRenderer.render(event.getItemFrameEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()))
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (!event.phase.equals(TickEvent.Phase.END))
                return;

            CaptureManager.onRenderTickEnd();
        }
    }
}
