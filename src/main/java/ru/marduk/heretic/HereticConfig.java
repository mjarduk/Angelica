package ru.marduk.heretic;

import com.gtnewhorizon.gtnhlib.config.Config;

public class HereticConfig {
    @Config.Comment("Debugging options used for development.")
    @Config(modid = "heretic", category = "debug_options")
    public static class DebugOptions {
        @Config.Comment("Enable error checking for most OpenXR calls")
        @Config.DefaultBoolean(true)
        public static boolean errorChecking;

        @Config.Comment("Whether to use XR_EXT_debug_utils")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        public static boolean useDebugUtils;

        @Config.Comment("Whether to use the validation layers (XR_APILAYER_LUNARG_core_validation)")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        public static boolean useValidationLayers;
    }
}
