package io.github.fallOut015.hud_quiver;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fallOut015.hud_quiver.common.ConfigHUDQuiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mod(MainHUDQuiver.MODID)
public class MainHUDQuiver {
    public static final String MODID = "hud_quiver";
    private static float interpolation;
    private static @Nullable ItemStack lastHeld;
    private static @Nullable List<ItemStack> lastReadyArrows;
    private static final ResourceLocation WIDGETS;

    // TODO everything configurable
    // TODO slot switching with ctrl scroll and ctrl num keys (ctrl is configurable)
    // TODO effects on arrow for flame, punch, and power
    // TODO effects on arrow for piercing
    // TODO render multiple arrows for each stack being combined
    // TODO ugh i have to do localization now
    // TODO config GUI

    static {
        interpolation = 0;
        lastHeld = null;
        lastReadyArrows = null;
        WIDGETS = new ResourceLocation("hud_quiver","textures/gui/widgets.png");
    }

    public MainHUDQuiver() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHUDQuiver.CLIENT_SPEC);
    }

    public static List<ItemStack> findAmmos(PlayerEntity player, ItemStack shootable) {
        if (!(shootable.getItem() instanceof ShootableItem)) {
            return Lists.newLinkedList();
        } else {
            List<ItemStack> list = Lists.newLinkedList();

            Predicate<ItemStack> predicate = ((ShootableItem) shootable.getItem()).getAmmoPredicate();
            ItemStack itemstack = ShootableItem.getHeldAmmo(player, predicate);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
            }
            predicate = ((ShootableItem) shootable.getItem()).getInventoryAmmoPredicate();

            for(int i = 0; i < player.inventory.getSizeInventory(); ++i) {
                ItemStack itemstack1 = player.inventory.getStackInSlot(i);
                if (predicate.test(itemstack1) && itemstack1 != itemstack) {
                    list.add(itemstack1);
                }
            }

            if(list.isEmpty() && player.abilities.isCreativeMode) {
                list.add(new ItemStack(Items.ARROW));
            }

            return list;
        }
    }
    public static float bezier(float x, float min, float max) {
        return MathHelper.clamp(((x * x) * (3 - 2 * x)) / (1 / (max - min)) + min, min, max);
    }

    @Mod.EventBusSubscriber
    public static class Events {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            @Nullable ClientPlayerEntity player = Minecraft.getInstance().player;

            if(event.getType() == RenderGameOverlayEvent.ElementType.ALL && player != null) {
                ItemStack playerHand;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemMainhand();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !Minecraft.getInstance().isGamePaused() && ConfigHUDQuiver.hides()) {
                        interpolation += 0.01f;
                    }
                } else if(player.getHeldItemOffhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemOffhand();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !Minecraft.getInstance().isGamePaused() && ConfigHUDQuiver.hides()) {
                        interpolation += 0.01f;
                    }
                } else {
                    if(!ConfigHUDQuiver.animates() && ConfigHUDQuiver.hides()) {
                        return;
                    }
                    
                    if(interpolation > 0 && !Minecraft.getInstance().isGamePaused() && ConfigHUDQuiver.hides()) {
                        interpolation -= 0.01f;
                    }

                    if(ConfigHUDQuiver.hides()) {
                        if(lastHeld == null) {
                            return;
                        } else {
                            playerHand = lastHeld;
                        }
                    } else {
                        playerHand = lastHeld = new ItemStack(Items.BOW);
                    }
                }

                if(!ConfigHUDQuiver.hides()) {
                    interpolation = 1.0f;
                }

                float left = ConfigHUDQuiver.getHorizontalOffset();
                float top = ConfigHUDQuiver.getVerticalOffset();

                event.getMatrixStack().push();
                event.getMatrixStack().translate(left, ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top, 0);
                Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS);
                AbstractGui.blit(event.getMatrixStack(), -12, -12, 0, 0, 24, 24, 36, 24);
                event.getMatrixStack().pop();

                List<ItemStack> readyArrows;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem || player.getHeldItemOffhand().getItem() instanceof ShootableItem || !ConfigHUDQuiver.hides()) {
                    readyArrows = findAmmos(player, playerHand);
                    lastReadyArrows = readyArrows;
                } else {
                    readyArrows = lastReadyArrows;
                }
                List<Integer> skips = Lists.newLinkedList();
                int xMultiplier = 0;

                if(readyArrows != null) {
                    if(readyArrows.size() == 0) {
                        event.getMatrixStack().push();
                        event.getMatrixStack().translate(0, 0, 1);
                        AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent("0"), 19, 17, 16733525);
                        event.getMatrixStack().pop();
                    } else {
                        for(int i = 0; i < readyArrows.size(); ++ i) {
                            if(skips.contains(i)) {
                                continue;
                            }

                            ItemStack readyArrow = readyArrows.get(i);
                            float x = 24 * xMultiplier + left, y = ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top;

                            event.getMatrixStack().push();
                            event.getMatrixStack().translate(x, y, i + 1);
                            event.getMatrixStack().scale(16, -16, 1);
                            event.getMatrixStack().rotate(Vector3f.YP.rotationDegrees(180));
                            event.getMatrixStack().rotate(Vector3f.XP.rotationDegrees(360));
                            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                            RenderSystem.enableDepthTest();
                            RenderSystem.disableCull();
                            Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                            buffer.finish();
                            event.getMatrixStack().pop();
                            RenderSystem.enableCull();
                            RenderSystem.disableDepthTest();
                            if(EnchantmentHelper.getEnchantmentLevel(Enchantments.MULTISHOT, playerHand) > 0) {
                                event.getMatrixStack().push();
                                event.getMatrixStack().translate(x - 4, y - 1, i + 1);
                                event.getMatrixStack().scale(10, -10, 1);
                                event.getMatrixStack().rotate(Vector3f.YP.rotationDegrees(180));
                                event.getMatrixStack().rotate(Vector3f.XP.rotationDegrees(360));
                                event.getMatrixStack().rotate(Vector3f.ZP.rotationDegrees(-30));
                                IRenderTypeBuffer.Impl buffer2 = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                                buffer2.finish();
                                event.getMatrixStack().pop();
                                RenderSystem.enableCull();
                                RenderSystem.disableDepthTest();

                                event.getMatrixStack().push();
                                event.getMatrixStack().translate(x + 1, y + 4, i + 1);
                                event.getMatrixStack().scale(10, -10, 1);
                                event.getMatrixStack().rotate(Vector3f.YP.rotationDegrees(180));
                                event.getMatrixStack().rotate(Vector3f.XP.rotationDegrees(360));
                                event.getMatrixStack().rotate(Vector3f.ZP.rotationDegrees(30));
                                IRenderTypeBuffer.Impl buffer3 = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                                buffer3.finish();
                                event.getMatrixStack().pop();
                                RenderSystem.enableCull();
                                RenderSystem.disableDepthTest();
                            }

                            int count = readyArrow.getCount();

                            for(int j = i + 1; j < readyArrows.size(); ++ j) {
                                ItemStack nextArrow = readyArrows.get(j);
                                if(nextArrow.isItemEqual(readyArrow) && ItemStack.areItemStackTagsEqual(nextArrow, readyArrow)) {
                                    count += nextArrow.getCount();
                                    skips.add(j);
                                } else {
                                    break;
                                }
                            }

                            event.getMatrixStack().push();
                            if(player.isCreative() || readyArrow.getItem() instanceof ArrowItem && ((ArrowItem) readyArrow.getItem()).isInfinite(readyArrow, playerHand, player)) {
                                event.getMatrixStack().translate(x + 3, y + 5, i + 1 + readyArrows.size());
                                Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS);
                                AbstractGui.blit(event.getMatrixStack(), -6, -4, 24, i == 0 ? 0 : 8, 12, 8, 36, 24);
                            } else {
                                boolean using = player.getItemInUseCount() > 0 && readyArrow == player.findAmmo(playerHand) && player.getActiveItemStack().getItem() instanceof ShootableItem;
                                String displayCount = using ? String.valueOf(count - 1) : String.valueOf(count);
                                int length = displayCount.length();
                                int color = i == 0 ? (using ? (count - 1 == 0 ? 16733525 /*red*/ : 16777045 /*yellow*/) : 16777215 /*white*/) : 10066329 /*gray*/;
                                event.getMatrixStack().translate(0, 0, i + 1 + readyArrows.size());
                                AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent(displayCount), Math.round(x + 9 - (6 * length)), Math.round(y + 1), color);
                            }
                            event.getMatrixStack().pop();

                            ++ xMultiplier;
                        }
                    }
                }
            }
        }
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = MODID)
    public static class ModEvents {
        @SubscribeEvent
        public static void onModConfigEvent(final ModConfig.ModConfigEvent event) {
            if(event.getConfig().getSpec() == ConfigHUDQuiver.CLIENT_SPEC) {
                ConfigHUDQuiver.bakeConfig();
            }
        }
    }
}