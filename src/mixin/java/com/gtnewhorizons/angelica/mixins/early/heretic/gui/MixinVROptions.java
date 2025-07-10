package com.gtnewhorizons.angelica.mixins.early.heretic.gui;

import com.seibel.distanthorizons.common.wrappers.gui.TexturedButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.marduk.heretic.gui.VRConfigScreen;

/**
 * Adds a small button next to difficulty for VR settings
 */
@Mixin(GuiOptions.class)
public class MixinVROptions extends GuiScreen {
    @Unique
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("heretic","textures/widgets.png");
    @Unique
    private static final int BUTTON_ID = 253; // chosen randomly with 3 dice rolls

    @Inject(at = @At("HEAD"), method = "initGui")
    private void heretic$init(CallbackInfo ci) {
        this.buttonList.add(
            (new TexturedButtonWidget(
                // Where the button is on the screen
                this.width / 2 + 160, this.height / 6 - 12,
                // Width and height of the button
                20, 20,
                // Offset
                0, 0, 20,
                // Some textuary stuff
                ICON_TEXTURE, 20, 20,
                // Create the button and tell it where to go
                // For now it goes to the client option by default
                BUTTON_ID,
                // Add a title to the button
                "VR" /* ModInfo.ID + ".title" */)));
    }

    @Inject(at = @At("HEAD"), method = "actionPerformed", cancellable = true)
    private void onAction(GuiButton button, CallbackInfo ci) {
        if (button.id == BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new VRConfigScreen());
            ci.cancel();
        }
    }
}
