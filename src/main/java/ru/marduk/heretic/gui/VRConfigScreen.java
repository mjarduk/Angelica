package ru.marduk.heretic.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

public class VRConfigScreen extends GuiScreen {
    private static final ResourceLocation WIDGETS = new ResourceLocation("heretic", "textures/widgets.png");
    private final Int2ObjectMap<Consumer<GuiButton>> buttonMap = new Int2ObjectOpenHashMap<>();
    private int logoAnim = 0;
    private GuiLabel errorLabel;

    @Override
    public void initGui() {
        addButton(new GuiButton(1, this.width / 2 - 100, this.height / 3 + 100, I18n.format("heretic.options.toggle")), button -> {

        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawLogo();
        drawErrorFrame();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        buttonMap.get(button.id).accept(button);
    }

    private void addButton(GuiButton button, Consumer<GuiButton> callback) {
        buttonList.add(button);
        buttonMap.put(button.id, callback);
    }

    private void drawErrorFrame() {
        int x = (width / 2) - 75;
        int y = (height / 2) + 50;

        drawRect(x, y, x + 150, y + 175, 0xEFEFEFFF);
        drawRect(x + 1, y + 1, x + 149, y + 174, Integer.MIN_VALUE);
        // this.fontRendererObj.drawSplitString();
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
}
