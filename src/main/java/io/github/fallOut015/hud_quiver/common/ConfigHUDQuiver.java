package io.github.fallOut015.hud_quiver.common;

import io.github.fallOut015.hud_quiver.MainHUDQuiver;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigHUDQuiver {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    private static boolean animate = true;
    private static boolean hide = true;
    private static int horizontal_offset = 16;
    private static int vertical_offset = 16;
    // enum for docking side
    // size

    public static boolean animates() {
        return animate;
    }
    public static boolean hides() {
        return hide;
    }
    public static int getHorizontalOffset() {
        return horizontal_offset;
    }
    public static int getVerticalOffset() {
        return vertical_offset;
    }

    public static void bakeConfig() {
        animate = CLIENT.ANIMATE.get();
        hide = CLIENT.HIDE.get();
        horizontal_offset = CLIENT.HORIZONTAL_OFFSET.get();
        vertical_offset = CLIENT.VERTICAL_OFFSET.get();
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue ANIMATE;
        public final ForgeConfigSpec.BooleanValue HIDE;
        public final ForgeConfigSpec.IntValue HORIZONTAL_OFFSET;
        public final ForgeConfigSpec.IntValue VERTICAL_OFFSET;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            //builder.push("hud_quiver");
            this.ANIMATE = builder.comment("Animate the HUD Quiver showing and hiding.").translation("hud_quiver.config.animate").define("ANIMATE", true);
            this.HIDE = builder.comment("Hide the HUD Quiver when not selecting a shootable item.").translation("hud_quiver.config.hide").define("HIDE", true);
            this.HORIZONTAL_OFFSET = builder.comment("The margin on the left of the HUD Quiver.").translation("hud_quiver.config.horizontal_offset").defineInRange("HORIZONTAL_OFFSET", 16, 0, 2048);
            this.VERTICAL_OFFSET = builder.comment("The margin on the top of the HUD Quiver.").translation("hud_quiver.config.vertical_offset").defineInRange("VERTICAL_OFFSET", 16, 0, 1024);
            //builder.pop();
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = MainHUDQuiver.MODID)
    public static class ModEvents {
        @SubscribeEvent
        public static void onModConfigEvent(final ModConfig.ModConfigEvent event) {
            if(event.getConfig().getSpec() == ConfigHUDQuiver.CLIENT_SPEC) {
                ConfigHUDQuiver.bakeConfig();
            }
        }
    }
}