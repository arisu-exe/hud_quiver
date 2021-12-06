package io.github.fallOut015.hud_quiver.client.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fallOut015.hud_quiver.client.ConfigHUDQuiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

// From https://leo3418.github.io/2021/03/31/forge-mod-config-screen-1-16.html

public class ConfigScreenHUDQuiver extends Screen {
    private static final ITextComponent GUI_DONE = new TranslationTextComponent("gui.done");

    private final Screen parent;
    private OptionsRowList optionsRowList;

    public ConfigScreenHUDQuiver(Screen parent) {
        super(new TranslationTextComponent("hud_quiver.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if(this.minecraft != null) {
            this.optionsRowList = new OptionsRowList(this.minecraft, this.width, this.height, 24, this.height - 32, 25);

            this.optionsRowList.addSmall(new BooleanOption("hud_quiver.config.animate", new TranslationTextComponent("hud_quiver.config.animate.desc"), u -> ConfigHUDQuiver.CLIENT.ANIMATE.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.ANIMATE.set(newValue)), new BooleanOption("hud_quiver.config.hide", new TranslationTextComponent("hud_quiver.config.hide.desc"), u -> ConfigHUDQuiver.CLIENT.HIDE.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.HIDE.set(newValue)));
            this.optionsRowList.addBig(new BooleanOption("hud_quiver.config.fade_queue", new TranslationTextComponent("hud_quiver.config.fade_queue.desc"), u -> ConfigHUDQuiver.CLIENT.FADE_QUEUE.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.FADE_QUEUE.set(newValue)));
            this.optionsRowList.addSmall(new SliderPercentageOption("hud_quiver.config.horizontal_offset", 0, 512, 8,u -> (double) ConfigHUDQuiver.CLIENT.HORIZONTAL_OFFSET.get(),(u, newValue) -> ConfigHUDQuiver.CLIENT.HORIZONTAL_OFFSET.set(newValue.intValue()),(gs, option) -> new StringTextComponent(I18n.get("hud_quiver.config.horizontal_offset") + ": " + (int) option.get(gs))), new SliderPercentageOption("hud_quiver.config.vertical_offset", 0, 512, 8, u -> (double) ConfigHUDQuiver.CLIENT.VERTICAL_OFFSET.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.VERTICAL_OFFSET.set(newValue.intValue()), (gs, option) -> new StringTextComponent(I18n.get("hud_quiver.config.vertical_offset") + ": " + (int) option.get(gs))));
            this.optionsRowList.addBig(new SliderPercentageOption("hud_quiver.config.size", 24, 192, 24, u -> (double) ConfigHUDQuiver.CLIENT.SIZE.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.SIZE.set(newValue.intValue()), (gs, option) -> new StringTextComponent(I18n.get("hud_quiver.config.size") + ": " + (int) option.get(gs))));
            this.optionsRowList.addBig(new SliderPercentageOption("hud_quiver.config.speed", 1, 10, 1, u -> (double) ConfigHUDQuiver.CLIENT.SPEED.get(), (u, newValue) -> ConfigHUDQuiver.CLIENT.SPEED.set(newValue.intValue()), (gs, option) -> new StringTextComponent(I18n.get("hud_quiver.config.speed") + ": " + (int) option.get(gs))) {
                @Override
                public Optional<List<IReorderingProcessor>> getTooltip() {
                    return Optional.of(Lists.newArrayList(Minecraft.getInstance().font.split(new TranslationTextComponent("hud_quiver.config.speed.desc"), 200)));
                }
            });

            this.children.add(this.optionsRowList);
        }

        this.addButton(new Button((this.width - 200) / 2, this.height - 26, 200, 20, GUI_DONE, button -> this.onClose()));
    }
    @Override
    public void onClose() {
        ConfigHUDQuiver.bakeConfig();
        if(this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        this.optionsRowList.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, 8, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}