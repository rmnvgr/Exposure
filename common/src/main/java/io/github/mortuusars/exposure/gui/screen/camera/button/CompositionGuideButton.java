package io.github.mortuusars.exposure.gui.screen.camera.button;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.CameraClient;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuide;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuides;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CompositionGuideButton extends CycleButton {
    private final List<CompositionGuide> guides;

    public CompositionGuideButton(Screen screen, int x, int y, int width, int height, int u, int v,  ResourceLocation texture) {
        super(screen, x, y, width, height, u, v, height, texture);
        guides = CompositionGuides.getGuides();

        Camera<?> camera = CameraClient.getCamera().orElseThrow();

        CompositionGuide guide = camera.get().getItem().getCompositionGuide(camera.get().getStack());

        int currentGuideIndex = 0;

        for (int i = 0; i < guides.size(); i++) {
            if (guides.get(i).getId().equals(guide.getId())) {
                currentGuideIndex = i;
                break;
            }
        }

        setupButtonElements(guides.size(), currentGuideIndex);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(Exposure.SoundEvents.CAMERA_BUTTON_CLICK.get(),
                Objects.requireNonNull(Minecraft.getInstance().level).random.nextFloat() * 0.15f + 0.93f, 0.7f));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        // Icon
        guiGraphics.blit(Exposure.resource("textures/gui/viewfinder/icon/composition_guide/" + guides.get(currentIndex).getId() + ".png"),
                getX(), getY() + 4, 0, 0, 0, 15, 14, 15, 14);
    }

    @Override
    public void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderTooltip(Minecraft.getInstance().font, List.of(Component.translatable("gui.exposure.viewfinder.composition_guide.tooltip"),
                ((MutableComponent) getMessage()).withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
    }

    @Override
    public @NotNull Component getMessage() {
        return guides.get(currentIndex).translate();
    }

    @Override
    protected void onCycle() {
        CameraClient.setCompositionGuide(guides.get(currentIndex));
    }
}
