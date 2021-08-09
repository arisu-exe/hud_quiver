package io.github.fallOut015.hud_quiver;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fallOut015.hud_quiver.client.gui.screen.ConfigScreenHUDQuiver;
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
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Mod(MainHUDQuiver.MODID)
public class MainHUDQuiver {
    public static final String MODID = "hud_quiver";
    private static float interpolation;
    private static @Nullable ItemStack lastHeld;
    private static @Nullable List<ItemStack> lastReadyArrows;
    private static final ResourceLocation WIDGETS;

    static {
        interpolation = 0;
        lastHeld = null;
        lastReadyArrows = null;
        WIDGETS = new ResourceLocation(MODID,"textures/gui/widgets.png");
    }

    public MainHUDQuiver() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHUDQuiver.CLIENT_SPEC);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreenHUDQuiver(screen));
    }

    public static List<ItemStack> findAmmos(PlayerEntity player, ItemStack shootable) {
        if (!(shootable.getItem() instanceof ShootableItem)) {
            return Lists.newLinkedList();
        } else {
            List<ItemStack> list = Lists.newLinkedList();

            Predicate<ItemStack> predicate = ((ShootableItem) shootable.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ShootableItem.getHeldProjectile(player, predicate);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
            }
            predicate = ((ShootableItem) shootable.getItem()).getAllSupportedProjectiles();

            for(int i = 0; i < player.inventory.getContainerSize(); ++i) {
                ItemStack itemstack1 = player.inventory.getItem(i);
                if (predicate.test(itemstack1) && itemstack1 != itemstack) {
                    list.add(itemstack1);
                }
            }

            if(list.isEmpty() && player.abilities.instabuild) {
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
                if(player.getMainHandItem().getItem() instanceof ShootableItem) {
                    playerHand = player.getMainHandItem();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !Minecraft.getInstance().isPaused() && ConfigHUDQuiver.hides()) {
                        interpolation += 0.01f;
                    }
                } else if(player.getOffhandItem().getItem() instanceof ShootableItem) {
                    playerHand = player.getOffhandItem();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !Minecraft.getInstance().isPaused() && ConfigHUDQuiver.hides()) {
                        interpolation += 0.01f;
                    }
                } else {
                    if(!ConfigHUDQuiver.animates() && ConfigHUDQuiver.hides()) {
                        return;
                    }
                    
                    if(interpolation > 0 && !Minecraft.getInstance().isPaused() && ConfigHUDQuiver.hides()) {
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

                event.getMatrixStack().pushPose();
                event.getMatrixStack().translate(left, ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top, 0);
                event.getMatrixStack().scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1);
                Minecraft.getInstance().getTextureManager().bind(WIDGETS);
                AbstractGui.blit(event.getMatrixStack(), -12, -12, 0, 0, 24, 24, 36, 24);
                event.getMatrixStack().popPose();

                List<ItemStack> readyArrows;
                if(player.getMainHandItem().getItem() instanceof ShootableItem || player.getOffhandItem().getItem() instanceof ShootableItem || !ConfigHUDQuiver.hides()) {
                    readyArrows = findAmmos(player, playerHand);
                    lastReadyArrows = readyArrows;
                } else {
                    readyArrows = lastReadyArrows;
                }
                List<Integer> skips = Lists.newLinkedList();
                int xMultiplier = 0;

                if(readyArrows != null) {
                    if(readyArrows.size() == 0) {
                        event.getMatrixStack().pushPose();
                        event.getMatrixStack().translate(0, 1, 1);
                        AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().font, new StringTextComponent("0"), Math.round(3 + left), Math.round(ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top), 16733525);
                        event.getMatrixStack().popPose();
                    } else {
                        for(int i = 0; i < readyArrows.size(); ++ i) {
                            if(skips.contains(i)) {
                                continue;
                            }

                            ItemStack readyArrow = readyArrows.get(i);
                            float x = ConfigHUDQuiver.getSize() * xMultiplier + left, y = ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top;

                            event.getMatrixStack().pushPose();
                            event.getMatrixStack().translate(x, y, i + 1);
                            event.getMatrixStack().scale(16, -16, 1);
                            event.getMatrixStack().scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1f);
                            event.getMatrixStack().mulPose(Vector3f.YP.rotationDegrees(180));
                            event.getMatrixStack().mulPose(Vector3f.XP.rotationDegrees(360));
                            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().renderBuffers().bufferSource();
                            RenderSystem.enableDepthTest();
                            RenderSystem.disableCull();
                            Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                            buffer.endBatch();
                            event.getMatrixStack().popPose();
                            RenderSystem.enableCull();
                            RenderSystem.disableDepthTest();
                            if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, playerHand) > 0) {
                                event.getMatrixStack().pushPose();
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    event.getMatrixStack().translate(x - 5, y + 3, i + 1);
                                } else {
                                    event.getMatrixStack().translate(x - 4, y - 1, i + 1);
                                }
                                event.getMatrixStack().scale(10, -10, 1);
                                event.getMatrixStack().scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1f);
                                event.getMatrixStack().mulPose(Vector3f.YP.rotationDegrees(180));
                                event.getMatrixStack().mulPose(Vector3f.XP.rotationDegrees(360));
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    event.getMatrixStack().mulPose(Vector3f.ZP.rotationDegrees(-20));
                                } else {
                                    event.getMatrixStack().mulPose(Vector3f.ZP.rotationDegrees(-30));
                                }
                                IRenderTypeBuffer.Impl buffer2 = Minecraft.getInstance().renderBuffers().bufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                                buffer2.endBatch();
                                event.getMatrixStack().popPose();
                                RenderSystem.enableCull();
                                RenderSystem.disableDepthTest();

                                event.getMatrixStack().pushPose();
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    event.getMatrixStack().translate(x + 5, y + 3, i + 1);
                                } else {
                                    event.getMatrixStack().translate(x + 1, y + 4, i + 1);
                                }
                                event.getMatrixStack().scale(10, -10, 1);
                                event.getMatrixStack().mulPose(Vector3f.YP.rotationDegrees(180));
                                event.getMatrixStack().mulPose(Vector3f.XP.rotationDegrees(360));
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    event.getMatrixStack().mulPose(Vector3f.ZP.rotationDegrees(20));
                                } else {
                                    event.getMatrixStack().mulPose(Vector3f.ZP.rotationDegrees(30));
                                }
                                IRenderTypeBuffer.Impl buffer3 = Minecraft.getInstance().renderBuffers().bufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                                buffer3.endBatch();
                                event.getMatrixStack().popPose();
                                RenderSystem.enableCull();
                                RenderSystem.disableDepthTest();
                            }

                            // Reminder: sharpshooter just does more damage for crossbows.

                            int count = readyArrow.getCount();

                            for(int j = i + 1; j < readyArrows.size(); ++ j) {
                                ItemStack nextArrow = readyArrows.get(j);
                                if(nextArrow.sameItem(readyArrow) && ItemStack.tagMatches(nextArrow, readyArrow)) {
                                    count += nextArrow.getCount();
                                    skips.add(j);
                                } else {
                                    break;
                                }
                            }
                            boolean hasCrossbowCeaseless = playerHand.getItem() == Items.CROSSBOW && EnchantmentHelper.getEnchantments(playerHand).keySet().stream().map(ForgeRegistryEntry::getRegistryName).filter(Objects::nonNull).anyMatch(name -> name.toString().equals("enigmaticlegacy:ceaseless"));

                            event.getMatrixStack().pushPose();
                            if(hasCrossbowCeaseless || player.isCreative() || readyArrow.getItem() instanceof ArrowItem && ((ArrowItem) readyArrow.getItem()).isInfinite(readyArrow, playerHand, player)) {
                                event.getMatrixStack().translate(x + 3, y + 5, i + 1 + readyArrows.size());
                                Minecraft.getInstance().getTextureManager().bind(WIDGETS);
                                event.getMatrixStack().scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1f);
                                AbstractGui.blit(event.getMatrixStack(), -6, -4, 24, i == 0 ? 0 : 8, 12, 8, 36, 24);
                            } else {
                                boolean using = player.getUseItemRemainingTicks() > 0 && readyArrow == player.getProjectile(playerHand) && player.getUseItem().getItem() instanceof ShootableItem;
                                String displayCount = using ? String.valueOf(count - 1) : String.valueOf(count);
                                int length = displayCount.length();
                                int color = i == 0 ? (using ? (count - 1 == 0 ? 16733525 /*red*/ : 16777045 /*yellow*/) : 16777215 /*white*/) : 10066329 /*gray*/;
                                event.getMatrixStack().scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1f);
                                event.getMatrixStack().translate(0, 0, i + 1 + readyArrows.size());
                                AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().font, new StringTextComponent(displayCount), Math.round(x + 9 - (6 * length)), Math.round(y + 1), color);
                            }
                            event.getMatrixStack().popPose();

                            ++ xMultiplier;
                        }
                    }
                }
            }
        }
    }
}