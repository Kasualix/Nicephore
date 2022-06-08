package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.enums.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.FilterListener;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class GalleryScreen extends Screen implements FilterListener {
    private static final Component TITLE = Component.translatable("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDirectory, "screenshots");
    private final int ROW = getRows();
    private final int COLUMN = 4;
    private final int IMAGES_TO_DISPLAY = ROW * COLUMN;
    private static ArrayList<DynamicTexture> SCREENSHOT_TEXTURES = new ArrayList<>();
    private ArrayList<File> screenshots;
    private ArrayList<List<File>> pagesOfScreenshots;
    private int index;
    private float aspectRatio;

    public GalleryScreen(int index) {
        super(TITLE);
        this.index = index;
    }

    public GalleryScreen() {
        this(0);
    }

    public static boolean canBeShow() {
        return SCREENSHOTS_DIR.exists() && SCREENSHOTS_DIR.list().length > 0;
    }

    private static int getRows() {
        if (Minecraft.getInstance().getWindow().getGuiScale() >= 3.0) {
            return 2;
        }
        return 3;
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();

        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());
        pagesOfScreenshots = (ArrayList<List<File>>) Util.batches(screenshots, IMAGES_TO_DISPLAY).collect(Collectors.toList());
        index = getIndex();
        aspectRatio = 1.7777F;

        if (!screenshots.isEmpty()) {

            try (ImageInputStream in = ImageIO.createImageInputStream(screenshots.get(index))) {
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        aspectRatio = reader.getWidth(0) / (float) reader.getHeight(0);
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            SCREENSHOT_TEXTURES.forEach(DynamicTexture::close);
            SCREENSHOT_TEXTURES.clear();

            List<File> filesToLoad = pagesOfScreenshots.get(index);
            if (!filesToLoad.isEmpty()) {
                filesToLoad.forEach(file -> SCREENSHOT_TEXTURES.add(Util.fileToTexture(file)));
            } else {
                closeScreen("nicephore.screenshots.loading.error");
                return;
            }
        }
    }

    private void changeFilter() {
        ScreenshotFilter nextFilter = NicephoreConfig.Client.getScreenshotFilter().next();
        NicephoreConfig.Client.setScreenshotFilter(nextFilter);
        init();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        final int centerX = this.width / 2;
        final int imageWidth = (int) (this.width * 1.0 / 5);
        final int imageHeight = (int) (imageWidth / aspectRatio);
        final int bottomLine = this.minecraft.getWindow().getGuiScaledHeight() - 30;


        this.renderBackground(matrixStack);

        this.clearWidgets();
        this.addRenderableWidget(new Button(10, 10, 100, 20, Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()), button -> changeFilter()));
        this.addRenderableWidget(new Button(this.width - 60, 10, 50, 20, Component.translatable("nicephore.screenshot.exit"), button -> onClose()));
        this.addRenderableWidget(new Button(this.width - 120, 10, 50, 20, Component.translatable("nicephore.gui.settings"), button -> openSettingsScreen()));

        if (!screenshots.isEmpty()) {
            this.addRenderableWidget(new Button(this.width / 2 - 80, bottomLine, 20, 20, Component.literal("<"), button -> modIndex(-1)));
            this.addRenderableWidget(new Button(this.width / 2 + 60, bottomLine, 20, 20, Component.literal(">"), button -> modIndex(1)));
        }

        if (pagesOfScreenshots.isEmpty()) {
            drawCenteredString(matrixStack, Minecraft.getInstance().font, Component.translatable("nicephore.screenshots.empty"), centerX, 50, Color.RED.getRGB());
        } else {
            final List<File> currentPage = pagesOfScreenshots.get(index);
            if (currentPage.stream().allMatch(File::exists)) {
                SCREENSHOT_TEXTURES.forEach(TEXTURE -> {

                    final int imageIndex = SCREENSHOT_TEXTURES.indexOf(TEXTURE);
                    final String name = currentPage.get(imageIndex).getName();
                    final Component text = Component.literal(StringUtils.abbreviate(name, 13));

                    int x = centerX - (15 - (imageIndex % 4) * 10) - (2 - (imageIndex % 4)) * imageWidth;
                    int y = 50 + (imageIndex / 4 * (imageHeight + 30));

                    RenderSystem.setShaderTexture(0, TEXTURE.getId());
                    RenderSystem.enableBlend();
                    blit(matrixStack, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                    RenderSystem.disableBlend();

                    drawExtensionBadge(matrixStack, FilenameUtils.getExtension(name), x - 10, y + 14);
                    this.addRenderableWidget(new Button(x, y + 5 + imageHeight, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                });

                drawCenteredString(matrixStack, Minecraft.getInstance().font, Component.translatable("nicephore.gui.gallery.pages", index + 1, pagesOfScreenshots.size()), centerX, bottomLine + 5, Color.WHITE.getRGB());
            }
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawExtensionBadge(PoseStack matrixStack, String extension, int x, int y) {
        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
            drawString(matrixStack, Minecraft.getInstance().font, extension.toUpperCase(), x + 12, y - 12, Color.WHITE.getRGB());
        }
    }

    private void modIndex(int value) {
        final int max = pagesOfScreenshots.size();
        if (index + value >= 0 && index + value < max) {
            index += value;
        } else {
            if (index + value < 0) {
                index = max - 1;
            } else {
                index = 0;
            }
        }
        init();
    }

    private void openScreenshotScreen(int value) {
        Minecraft.getInstance().pushGuiLayer(new ScreenshotScreen(value, index, this));
    }

    private void openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(new SettingsScreen());
    }

    private int getIndex() {
        if (index >= pagesOfScreenshots.size() || index < 0) {
            index = pagesOfScreenshots.size() - 1;
        }
        return index;
    }

    private void closeScreen(String textComponentId) {
        this.onClose();
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId));
    }

    @Override
    public void onClose() {
        SCREENSHOT_TEXTURES.forEach(DynamicTexture::close);
        SCREENSHOT_TEXTURES.clear();

        super.onClose();
    }

    @Override
    public void onFilterChange(ScreenshotFilter filter) {
        NicephoreConfig.Client.setScreenshotFilter(filter);
        init();
    }
}
