package dev.tr7zw.graphutil.mixin;

import java.text.DecimalFormat;
import java.util.Objects;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Transformation;

import dev.tr7zw.graphutil.GraphUtilModBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.Mth;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private Font font;

    private DecimalFormat df = new DecimalFormat("0.00");

    @Inject(method = "render", at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (GraphUtilModBase.instance.config.alwaysShowGraph) {
            this.minecraft.options.renderFpsChart = true;
        }
    }

    @Inject(method = "drawChart", at = @At("HEAD"), cancellable = true)
    private void drawChart(GuiGraphics guiGraphics, FrameTimer frameTimer, int i, int j, boolean fpsGraph,
            CallbackInfo ci) {
        if (!fpsGraph) {
            if (!GraphUtilModBase.instance.config.hideTPS) {
                drawServerChart(guiGraphics, frameTimer, i, j);
            }
            ci.cancel();
            return;
        }
        RenderSystem.disableDepthTest();

        int k = frameTimer.getLogStart();
        int l = frameTimer.getLogEnd();
        long[] ls = frameTimer.getLog();
        int m = k;
        int n = i;
        int o = Math.max(0, ls.length - j);
        int p = ls.length - o;

        m = frameTimer.wrapIndex(m + o);

        float totalMs = 0f;
        float minMs = Integer.MAX_VALUE;
        float maxMs = Integer.MIN_VALUE;
        int t;
        for (t = 0; t < p; t++) {
            float u = ls[frameTimer.wrapIndex(m + t)] / 1000000f;
            minMs = Math.min(minMs, u);
            maxMs = Math.max(maxMs, u);
            totalMs += u;

        }
        t = this.minecraft.getWindow().getGuiScaledHeight();
        guiGraphics.fill(RenderType.guiOverlay(), i, t - 60, i + p, t, -1873784752);

        boolean vanillaScale = GraphUtilModBase.instance.config.vanillaScale;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix4f = Transformation.identity().getMatrix();

        boolean preventClipping = GraphUtilModBase.instance.config.preventClipping;
        float scaled = maxMs - minMs;
        while (m != l) {
            float ms = ls[m] / 1000000f;
            ms -= minMs;
            int v = vanillaScale ? frameTimer.scaleSampleTo(ls[m], 30, 60) : (int) (ms / scaled * 60f);
            int w = 100;
            int x = getSampleColor(Mth.clamp(v, 0, w), 0, w / 2, w);

            int y = x >> 24 & 0xFF;
            int z = x >> 16 & 0xFF;
            int aa = x >> 8 & 0xFF;
            int ab = x & 0xFF;

            int size = preventClipping ? Math.min(v, 60) : v;
            bufferBuilder.vertex(matrix4f, (n + 1), t, 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, (n + 1), (t - size + 1), 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, n, (t - size + 1), 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, n, t, 0.0F).color(z, aa, ab, y).endVertex();

            n++;
            m = frameTimer.wrapIndex(m + 1);

        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();

        if (vanillaScale) {
            guiGraphics.fill(RenderType.guiOverlay(), i + 1, t - 30 + 1, i + 14, t - 30 + 10, -1873784752);
            guiGraphics.drawString(this.font, "60 FPS", (i + 2), (t - 30 + 2), 14737632);
            guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 30, -1);
            guiGraphics.fill(RenderType.guiOverlay(), i + 1, t - 60 + 1, i + 14, t - 60 + 10, -1873784752);
            guiGraphics.drawString(this.font, "30 FPS", (i + 2), (t - 60 + 2), 14737632);
            guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 60, -1);
        }

        guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 1, -1);
        guiGraphics.vLine(RenderType.guiOverlay(), i, t - 60, t, -1);
        guiGraphics.vLine(RenderType.guiOverlay(), i + p - 1, t - 60, t, -1);

        if (!vanillaScale) {
            guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 1 - (int) (((totalMs / p) - minMs) / scaled * 60f), -16711681);
            guiGraphics.drawString(this.font, "avg", (i + p), t - 6 - (int) (((totalMs / p) - minMs) / scaled * 60f),
                    14737632);
        } else {
            int avgValue = frameTimer.scaleSampleTo((long) (totalMs * 1000000 / p), 30, 60);
            if (!(preventClipping && avgValue > 60)) {
                guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 1 - avgValue, -16711681);
                guiGraphics.drawString(this.font, "avg", (i + p), t - 6 - avgValue, 14737632);
            }
        }

        String string = "" + df.format(minMs) + " ms min";
        String string2 = "" + df.format(totalMs / p) + " ms avg";
        String string3 = "" + df.format(maxMs) + " ms max";
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string, (i + 2), (t - 60 - 9), 14737632);
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string2, (i + p / 2 - this.font.width(string2) / 2), (t - 60 - 9), 14737632);
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string3, (i + p - this.font.width(string3)), (t - 60 - 9), 14737632);

        RenderSystem.enableDepthTest();
        ci.cancel();
    }

    private void drawServerChart(GuiGraphics guiGraphics, FrameTimer frameTimer, int i, int j) {
        RenderSystem.disableDepthTest();
        int k = frameTimer.getLogStart();
        int l = frameTimer.getLogEnd();
        long[] ls = frameTimer.getLog();
        int m = k;
        int n = i;
        int o = Math.max(0, ls.length - j);
        int p = ls.length - o;
        m = frameTimer.wrapIndex(m + o);
        float totalMs = 0f;
        float minMs = Integer.MAX_VALUE;
        float maxMs = Integer.MIN_VALUE;
        int count = 0;
        int t;
        for (t = 0; t < p; t++) {
            float u = ls[frameTimer.wrapIndex(m + t)] / 1000000f;
            if (u == 0)
                continue;
            minMs = Math.min(minMs, u);
            maxMs = Math.max(maxMs, u);
            totalMs += u;
            count++;
        }
        t = this.minecraft.getWindow().getGuiScaledHeight();
        guiGraphics.fill(RenderType.guiOverlay(), i, t - 60, i + p, t, -1873784752);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f = Transformation.identity().getMatrix();

        boolean preventClipping = GraphUtilModBase.instance.config.preventClipping;

        while (m != l) {
            int i1 = frameTimer.scaleSampleTo(ls[m], 60, 20);
            int w = 60;
            int x = getSampleColor(Mth.clamp(i1, 0, w), 0, w / 2, w);
            int y = x >> 24 & 0xFF;
            int z = x >> 16 & 0xFF;
            int aa = x >> 8 & 0xFF;
            int ab = x & 0xFF;
            int size = preventClipping ? Math.min(i1, 60) : i1;
            bufferBuilder.vertex(matrix4f, (n + 1), t, 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, (n + 1), (t - size + 1), 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, n, (t - size + 1), 0.0F).color(z, aa, ab, y).endVertex();
            bufferBuilder.vertex(matrix4f, n, t, 0.0F).color(z, aa, ab, y).endVertex();
            n++;
            m = frameTimer.wrapIndex(m + 1);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();

        guiGraphics.fill(RenderType.guiOverlay(), i + 1, t - 60 + 1, i + 14, t - 60 + 10, -1873784752);
        guiGraphics.drawString(this.font, "20 TPS", (i + 2), (t - 60 + 2), 14737632);
        guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 60, -1);

        guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 1, -1);
        guiGraphics.vLine(RenderType.guiOverlay(), i, t - 60, t, -1);
        guiGraphics.vLine(RenderType.guiOverlay(), i + p - 1, t - 60, t, -1);

        int avgValue = frameTimer.scaleSampleTo((long) (totalMs * 1000000 / count), 60, 20);
        if (!(preventClipping && avgValue > 60)) {
            guiGraphics.hLine(RenderType.guiOverlay(), i, i + p - 1, t - 1 - avgValue, -16711681);
            guiGraphics.drawString(this.font, "avg", (i - 18), t - 6 - avgValue, 14737632);
        }

        String string = "" + df.format(minMs) + " ms min";
        String string2 = "" + df.format(totalMs / p) + " ms avg";
        String string3 = "" + df.format(maxMs) + " ms max";
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string, (i + 2), (t - 60 - 9), 14737632);
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string2, (i + p / 2 - this.font.width(string2) / 2), (t - 60 - 9), 14737632);
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, string3, (i + p - this.font.width(string3)), (t - 60 - 9), 14737632);
        RenderSystem.enableDepthTest();
    }

    @Shadow
    private int getSampleColor(int i, int j, int k, int l) {
        return 0;
    }

}
