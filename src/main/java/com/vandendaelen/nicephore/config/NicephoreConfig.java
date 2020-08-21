package com.vandendaelen.nicephore.config;

import com.vandendaelen.nicephore.utils.Util;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class NicephoreConfig {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Client, ForgeConfigSpec> specClientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specClientPair.getRight();
        CLIENT = specClientPair.getLeft();
    }

    public static class Client {
        public final ForgeConfigSpec.DoubleValue compression;
        public final ForgeConfigSpec.BooleanValue makeJPEGs, optimisedOutput, showOptimisationStatus;
        public final ForgeConfigSpec.IntValue pngOptimisationLevel;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("Client settings");
            compression = builder
                    .comment("JPEG compression level, the higher the number, the better the quality." +
                            "\r\nNote that 1.0 is *not* lossless as JPEG is a lossy-only format, use the PNG files instead if you want lossless.")
                    .defineInRange("compression", 0.9, 0.0,1.0);
            makeJPEGs = builder
                    .comment("Enable to allow Nicephore to make lossy JPEGs of your screenshots for easier online sharing. Disable to only allow PNGs." +
                            "\r\nNote that PNGs will still be made regardless of this option.")
                    .define("makeJPEGs", true);

            optimisedOutput = builder
                    .comment("Enable to allow Nicephore to losslessly optimise the PNG and JPEG screenshots for smaller sized progressive files that are of identical quality to the files before optimisation." +
                            "\r\nNote: Enabling this will cause screenshots to take slightly longer to save as an optimisation step will have to be run first." +
                            "\r\nTip: In the rare case that a screenshot PNG is corrupted, run \"oxipng --fix (filename).png\" to attempt to fix it.")
                    .define("optimiseScreenshots", Util.getOS().equals(Util.OS.MAC) || Util.getOS().equals(Util.OS.WINDOWS));

            showOptimisationStatus = builder
                    .comment("If enabled, a message will appear above your hotbar telling you that has optimisation started and another when finished. Useful for very slow computers.")
                    .define("showOptimisationStatus", true);

            builder.push("PNG-specific settings");
            pngOptimisationLevel = builder
                    .comment("If optimiseScreenshots is enabled, use the following oxipng optimisation level, with higher numbers taking longer to process but give lower file sizes." +
                            "\r\nTip: I would avoid anything above 3 unless you have a lot of CPU cores and threads (e.g. 16c/32t+) as it starts taking significantly longer to process for vastly diminishing returns.")
                    .defineInRange("pngOptimisationLevel", 2, 0, 5);
            builder.pop();

            builder.pop();
        }
        public static float getCompressionLevel() {
            return CLIENT.compression.get().floatValue();
        }
        public static boolean getJPEGToggle() { return CLIENT.makeJPEGs.get(); }
        public static boolean getOptimisedOutputToggle() { return CLIENT.optimisedOutput.get(); }
        public static boolean getShouldShowOptStatus() { return CLIENT.showOptimisationStatus.get(); }
        public static byte getPNGOptimisationLevel() { return CLIENT.pngOptimisationLevel.get().byteValue(); }
    }
}
