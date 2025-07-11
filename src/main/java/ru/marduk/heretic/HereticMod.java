package ru.marduk.heretic;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.angelica.config.AngelicaConfig;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.marduk.heretic.client.HMDService;

@SuppressWarnings("unused") // Used implicitly
@Mod(modid = "heretic", name = "Heretic")
public class HereticMod {
    public static final Logger LOGGER = LogManager.getLogger("HereticVR");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient() && AngelicaConfig.enableHeretic) {
            HMDService.INSTANCE.init();
        }
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) throws ConfigException {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient() && AngelicaConfig.enableHeretic) {
            ConfigurationManager.registerConfig (HereticConfig.DebugOptions.class);
        }
    }
}
