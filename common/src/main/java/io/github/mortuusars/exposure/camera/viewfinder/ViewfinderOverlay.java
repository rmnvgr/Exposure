package io.github.mortuusars.exposure.camera.viewfinder;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.CameraClient;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.FilmRollItem;
import io.github.mortuusars.exposure.util.GuiUtil;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.Rect2f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.Optional;

public class ViewfinderOverlay {
    public static final ResourceLocation VIEWFINDER_TEXTURE = Exposure.resource("textures/gui/viewfinder/viewfinder.png");
    public static final ResourceLocation NO_FILM_ICON_TEXTURE = Exposure.resource("textures/gui/viewfinder/icon/no_film.png");
    public static final ResourceLocation REMAINING_FRAMES_ICON_TEXTURE = Exposure.resource("textures/gui/viewfinder/icon/remaining_frames.png");

    public static Rect2f opening = new Rect2f(0, 0, 0, 0);

    private static final PoseStack POSE_STACK = new PoseStack();
    private static Minecraft minecraft = Minecraft.getInstance();
    private static Player player = minecraft.player;

    private static int backgroundColor;

    private static float scale = 1f;

    private static Float xRot = null;
    private static Float yRot = null;
    private static Float xRot0 = null;
    private static Float yRot0 = null;

    public static void setup() {
        minecraft = Minecraft.getInstance();
        player = minecraft.player;

        backgroundColor = Config.Client.getBackgroundColor();
        scale = 0.5f;
    }

    public static float getScale() {
        return scale;
    }

    public static void render() {
        final int width = minecraft.getWindow().getGuiScaledWidth();
        final int height = minecraft.getWindow().getGuiScaledHeight();

        scale = Mth.lerp(Math.min(0.5f * minecraft.getDeltaFrameTime(), 0.5f), scale, 1f);
        float openingSize = Math.min(width, height);

        opening = new Rect2f((width - openingSize) / 2f, (height - openingSize) / 2f, openingSize, openingSize);

        if (minecraft.options.hideGui)
            return;

        Optional<Camera<?>> cameraOpt = CameraClient.getCamera();
        if (cameraOpt.isEmpty()) {
            return;
        }

        Camera<?> camera = cameraOpt.get();
        CameraItem cameraItem = camera.get().getItem();
        ItemStack cameraStack = camera.get().getStack();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (xRot == null || yRot == null || xRot0 == null || yRot0 == null) {
            xRot = player.getXRot();
            yRot = player.getYRot();
            xRot0 = xRot;
            yRot0 = yRot;
        }
        float delta = Math.min(0.7f * minecraft.getDeltaFrameTime(), 0.8f);
        xRot0 = Mth.lerp(delta, xRot0, xRot);
        yRot0 = Mth.lerp(delta, yRot0, yRot);
        xRot = player.getXRot();
        yRot = player.getYRot();
        float xDelay = (xRot - xRot0);
        float yDelay = (yRot - yRot0);
        // Adjust for gui scale:
        xDelay *= width / (float)Minecraft.getInstance().getWindow().getWidth();
        yDelay *= height / (float)Minecraft.getInstance().getWindow().getHeight();
        xDelay *= 3.5f;
        yDelay *= 3.5f;

        PoseStack poseStack = POSE_STACK;
        poseStack.pushPose();
        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(scale, scale, scale);

        float attackAnim = player.getAttackAnim(minecraft.getFrameTime());
        if (attackAnim > 0.5f)
            attackAnim = 1f - attackAnim;
        poseStack.scale(1f - attackAnim * 0.4f, 1f - attackAnim * 0.6f, 1f - attackAnim * 0.4f);
        poseStack.translate(width / 16f * attackAnim, width / 5f * attackAnim, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(attackAnim, 0, 10)));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(attackAnim, 0, 100)));

        poseStack.translate(-width / 2f - yDelay, -height / 2f - xDelay, 0);

        if (minecraft.options.bobView().get())
            bobView(poseStack, minecraft.getFrameTime());

        // -9999 to cover all screen when poseStack is scaled down.
        // Left
        drawRect(poseStack, -9999, opening.y, opening.x, opening.y + opening.height, backgroundColor);
        // Right
        drawRect(poseStack, opening.x + opening.width, opening.y, width + 9999, opening.y + opening.height, backgroundColor);
        // Top
        drawRect(poseStack, -9999, -9999, width + 9999, opening.y, backgroundColor);
        // Bottom
        drawRect(poseStack, -9999, opening.y + opening.height, width + 9999, height + 9999, backgroundColor);

        // Shutter
        if (cameraItem.isShutterOpen(cameraStack))
            drawRect(poseStack, opening.x, opening.y, opening.x + opening.width, opening.y + opening.height, 0xfa1f1d1b);

        // Opening Texture
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, VIEWFINDER_TEXTURE);
        GuiUtil.blit(poseStack, opening.x, opening.x + opening.width, opening.y, opening.y + opening.height, 0f, 0f, 1f, 0f, 1f);

        // Guide
        RenderSystem.setShaderTexture(0, Exposure.resource("textures/gui/viewfinder/composition_guide/" +
                cameraItem.getCompositionGuide(cameraStack).getId() + ".png"));
        GuiUtil.blit(poseStack, opening.x, opening.x + opening.width, opening.y, opening.y + opening.height, -1f, 0f, 1f, 0f, 1f);

        if (!(minecraft.screen instanceof ViewfinderControlsScreen)) {
            renderIcons(poseStack, cameraItem, cameraStack);
        }

        poseStack.popPose();
    }

    private static void renderIcons(PoseStack poseStack, CameraItem cameraItem, ItemStack cameraStack) {
        Optional<ItemAndStack<FilmRollItem>> film = cameraItem.getFilm(cameraStack);
        if (film.isEmpty() || !film.get().getItem().canAddFrame(film.get().getStack())) {
            RenderSystem.setShaderTexture(0, NO_FILM_ICON_TEXTURE);
            GuiUtil.blit(poseStack, (opening.x + (opening.width / 2) - 12), opening.y + opening.height - 19,
                    24, 19, 0, 0, 24, 19, 0);
        }
        else {
            ItemAndStack<FilmRollItem> f = film.get();
            int maxFrames = f.getItem().getMaxFrameCount(f.getStack());
            int exposedFrames = f.getItem().getExposedFramesCount(f.getStack());
            int remainingFrames = Math.max(0, maxFrames - exposedFrames);
            if (maxFrames > 5 && remainingFrames <= 3) {
                RenderSystem.setShaderTexture(0, REMAINING_FRAMES_ICON_TEXTURE);
                GuiUtil.blit(poseStack, (opening.x + (opening.width / 2) - 17), opening.y + opening.height - 15,
                        33, 15, 0, (remainingFrames - 1) * 15, 33, 45, 0);
            }
        }
    }

    public static void drawRect(PoseStack poseStack, float minX, float minY, float maxX, float maxY, int color) {
        if (minX < maxX) {
            float temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minY < maxY) {
            float temp = minY;
            minY = maxY;
            maxY = temp;
        }

        float alpha = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(matrix, minX, maxY, 0.0F).color(r, g, b, alpha).endVertex();
        bufferbuilder.vertex(matrix, maxX, maxY, 0.0F).color(r, g, b, alpha).endVertex();
        bufferbuilder.vertex(matrix, maxX, minY, 0.0F).color(r, g, b, alpha).endVertex();
        bufferbuilder.vertex(matrix, minX, minY, 0.0F).color(r, g, b, alpha).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
    }

    public static void bobView(PoseStack poseStack, float partialTicks) {
        if (minecraft.getCameraEntity() instanceof Player pl) {
            float f = pl.walkDist - pl.walkDistO;
            float f1 = -(pl.walkDist + f * partialTicks);
            float f2 = Mth.lerp(partialTicks, pl.oBob, pl.bob);
            poseStack.translate((Mth.sin(f1 * (float) Math.PI) * f2 * 16F), (-Math.abs(Mth.cos(f1 * (float) Math.PI) * f2 * 32F)), 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F));
        }
    }
}

