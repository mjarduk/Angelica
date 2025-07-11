package ru.marduk.heretic.client.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.marduk.heretic.client.HMDService;
import ru.marduk.heretic.client.gui.config.HereticGuiConfig;

import java.util.function.Consumer;

public class VRMenuScreen extends GuiScreen {
    private static final ResourceLocation WIDGETS = new ResourceLocation("heretic", "textures/widgets.png");
    private static final HMDService HMD = HMDService.INSTANCE;

    private final Int2ObjectMap<Consumer<GuiButton>> buttonMap = new Int2ObjectOpenHashMap<>();

    private int logoAnim = 0;

    @Override
    public void initGui() {
        addButton(new GuiButton(1, this.width / 2 - 100, this.height / 3 + 60, getStatusDisplayText()), button -> {
            if (HMD.isAreWeVRYet())
                HMD.initHMD();
            else
                HMD.stopHMD();

            button.displayString = getStatusDisplayText();
        });
        buttonList.getFirst().enabled = HMD.canWeVRToBeginWith();

        addButton(new GuiButton(2, this.width / 2 - 100, this.height / 3 + 85, I18n.format("heretic.options.configure")), button -> {
            try {
                Minecraft.getMinecraft().displayGuiScreen(new HereticGuiConfig(this));
            } catch (ConfigException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawLogo();
        if (!HMD.canWeVRToBeginWith()) {
            this.drawCenteredString(this.fontRendererObj, HMD.whyCantWeVR(), width / 2, height / 3 + 45, 0xFFBB0000);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        buttonMap.get(button.id).accept(button);
    }

    private void addButton(GuiButton button, Consumer<GuiButton> callback) {
        buttonList.add(button);
        buttonMap.put(button.id, callback);
    }

    private void drawLogo() {
        mc.getTextureManager().bindTexture(WIDGETS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(this.width / 2 - 75, this.height / 3, 20, (50 * logoAnim), 140, 50);

        logoAnim++;
        if (logoAnim == 5) logoAnim = 0;
    }

    private String getStatusDisplayText() {
        return I18n.format("heretic.options.toggle", I18n.format(HMD.isAreWeVRYet() ? "heretic.options.on" : "heretic.options.off"));
    }
}
