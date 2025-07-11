package ru.marduk.heretic.client.gui.config;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import net.minecraft.client.gui.GuiScreen;
import ru.marduk.heretic.HereticConfig;

public class HereticGuiConfig extends SimpleGuiConfig {
    public HereticGuiConfig(GuiScreen parent) throws ConfigException {
        super(parent, "heretic", "Heretic", true,
            HereticConfig.DebugOptions.class
        );
    }
}
