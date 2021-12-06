package io.github.fallOut015.hud_quiver;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.github.fallOut015.hud_quiver.client.gui.screen.ConfigScreenHUDQuiver;
import io.github.fallOut015.hud_quiver.client.ConfigHUDQuiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

    public static List<ItemStack> findAmmos(Player player, ItemStack shootable) {
        if (!(shootable.getItem() instanceof ProjectileWeaponItem)) {
            return Lists.newLinkedList();
        } else {
            List<ItemStack> list = Lists.newLinkedList();

            Predicate<ItemStack> predicate = ((ProjectileWeaponItem) shootable.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(player, predicate);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
            }
            predicate = ((ProjectileWeaponItem) shootable.getItem()).getAllSupportedProjectiles();

            for(int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack1 = player.getInventory().getItem(i);
                if (predicate.test(itemstack1) && itemstack1 != itemstack) {
                    list.add(itemstack1);
                }
            }

            if(list.isEmpty() && player.isCreative()) {
                list.add(new ItemStack(Items.ARROW));
            }

            return list;
        }
    }
    public static float bezier(float x, float min, float max) {
        return Mth.clamp(((x * x) * (3 - 2 * x)) / (1 / (max - min)) + min, min, max);
    }

    // TODO add hover descriptions for config values (same as config file comments)
    // TODO fix spacing between hud quiver queue entries
    // TODO nyf's quiver interoperability
    // TODO effects on arrows
    // TODO effects on crossbow bolts
    // TODO automatically determine docking on top or bottom
    // TODO render multiple arrow stacks
    // TODO make sure text renders on top of item stacks (just translate Z depending on readyArrow length)
    // TODO remove calls to Minecraft class

    @Mod.EventBusSubscriber
    public static class Events {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            @Nullable LocalPlayer player = Minecraft.getInstance().player;

            if(event.getType() == RenderGameOverlayEvent.ElementType.ALL && player != null) {
                ItemStack playerHand;

                boolean paused = Minecraft.getInstance().isPaused();

                if(player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                    playerHand = player.getMainHandItem();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !paused && ConfigHUDQuiver.hides()) {
                        interpolation += 0.002f * (float) ConfigHUDQuiver.getSpeed();
                    }
                } else if(player.getOffhandItem().getItem() instanceof ProjectileWeaponItem) {
                    playerHand = player.getOffhandItem();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f && !paused && ConfigHUDQuiver.hides()) {
                        interpolation += 0.002f * (float) ConfigHUDQuiver.getSpeed();
                    }
                } else {
                    if(!ConfigHUDQuiver.animates() && ConfigHUDQuiver.hides()) {
                        return;
                    }
                    
                    if(interpolation > 0 && !paused && ConfigHUDQuiver.hides()) {
                        interpolation -= 0.002f * (float) ConfigHUDQuiver.getSpeed();
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

                PoseStack poseStack = event.getMatrixStack();

                poseStack.pushPose();
                poseStack.translate(left, ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top, 0);
                poseStack.scale((float) ConfigHUDQuiver.getSize() / 24f, (float) ConfigHUDQuiver.getSize() / 24f, 1);
                Minecraft.getInstance().getTextureManager().bind(WIDGETS);
                AbstractGui.blit(poseStack, -12, -12, 0, 0, 24, 24, 36, 24);

                List<ItemStack> readyArrows;
                if(player.getMainHandItem().getItem() instanceof ProjectileWeaponItem || player.getOffhandItem().getItem() instanceof ProjectileWeaponItem || !ConfigHUDQuiver.hides()) {
                    readyArrows = findAmmos(player, playerHand);
                    lastReadyArrows = readyArrows;
                } else {
                    readyArrows = lastReadyArrows;
                }
                List<Integer> skips = Lists.newLinkedList();
                int xMultiplier = 0;

                if(readyArrows != null) {
                    if(readyArrows.size() == 0) {
                        poseStack.translate(3, 1, 0);
                        AbstractGui.drawString(poseStack, Minecraft.getInstance().font, new StringTextComponent("0"), 0, 0, 16733525);
                    } else {
                        for(int i = 0; i < readyArrows.size(); ++ i) {
                            if(skips.contains(i)) {
                                continue;
                            }

                            ItemStack readyArrow = readyArrows.get(i);
                            float x = ConfigHUDQuiver.getSize() * xMultiplier, y = 0 /*ConfigHUDQuiver.animates() ? bezier(interpolation, -top, top) : top*/;

                            poseStack.pushPose();
                            poseStack.translate(x, y, i + 1);
                            poseStack.scale(16, -16, 1);
                            poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
                            poseStack.mulPose(Vector3f.XP.rotationDegrees(360));
                            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().renderBuffers().bufferSource();
                            RenderSystem.enableDepthTest();
                            RenderSystem.disableCull();
                            Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 || !ConfigHUDQuiver.queueFades() ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, poseStack, buffer);
                            buffer.endBatch();
                            poseStack.popPose();
                            RenderSystem.enableCull();
                            RenderSystem.disableDepthTest();
                            if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, playerHand) > 0) {
                                poseStack.pushPose();
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    poseStack.translate(x - 5, y + 3, i + 1);
                                } else {
                                    poseStack.translate(x - 4, y - 1, i + 1);
                                }
                                poseStack.scale(10, -10, 1);
                                poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
                                poseStack.mulPose(Vector3f.XP.rotationDegrees(360));
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    poseStack.mulPose(Vector3f.ZP.rotationDegrees(-20));
                                } else {
                                    poseStack.mulPose(Vector3f.ZP.rotationDegrees(-30));
                                }
                                IRenderTypeBuffer.Impl buffer2 = Minecraft.getInstance().renderBuffers().bufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 || !ConfigHUDQuiver.queueFades() ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, poseStack, buffer);
                                buffer2.endBatch();
                                poseStack.popPose();
                                RenderSystem.enableCull();
                                RenderSystem.disableDepthTest();

                                poseStack.pushPose();
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    poseStack.translate(x + 5, y + 3, i + 1);
                                } else {
                                    poseStack.translate(x + 1, y + 4, i + 1);
                                }
                                poseStack.scale(10, -10, 1);
                                poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
                                poseStack.mulPose(Vector3f.XP.rotationDegrees(360));
                                if(readyArrow.getItem() == Items.FIREWORK_ROCKET) {
                                    poseStack.mulPose(Vector3f.ZP.rotationDegrees(20));
                                } else {
                                    poseStack.mulPose(Vector3f.ZP.rotationDegrees(30));
                                }
                                IRenderTypeBuffer.Impl buffer3 = Minecraft.getInstance().renderBuffers().bufferSource();
                                RenderSystem.enableDepthTest();
                                RenderSystem.disableCull();
                                Minecraft.getInstance().getItemRenderer().renderStatic(readyArrow, ItemCameraTransforms.TransformType.FIXED, i == 0 || !ConfigHUDQuiver.queueFades() ? 15728880 : 14540253, OverlayTexture.NO_OVERLAY, poseStack, buffer);
                                buffer3.endBatch();
                                poseStack.popPose();
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

                            poseStack.pushPose();
                            if(hasCrossbowCeaseless || player.isCreative() || (readyArrow.getItem() instanceof ArrowItem && ((ArrowItem) readyArrow.getItem()).isInfinite(readyArrow, playerHand, player))) {
                                poseStack.translate(x + 3, y + 5, i + 1 + readyArrows.size());
                                Minecraft.getInstance().getTextureManager().bind(WIDGETS);
                                AbstractGui.blit(poseStack, -6, -4, 24, i == 0 || !ConfigHUDQuiver.queueFades() ? 0 : 8, 12, 8, 36, 24);
                            } else {
                                boolean using = player.getUseItemRemainingTicks() > 0 && readyArrow == player.getProjectile(playerHand) && player.getUseItem().getItem() instanceof ProjectileWeaponItem;
                                String displayCount = using ? String.valueOf(count - 1) : String.valueOf(count);
                                int length = displayCount.length();
                                int color = i == 0 || !ConfigHUDQuiver.queueFades() ? (using ? (count - 1 == 0 ? 16733525 /*red*/ : 16777045 /*yellow*/) : 16777215 /*white*/) : 10066329 /*gray*/; // yay for ternaries
                                poseStack.translate(Math.round(x + 9 - (6 * length)), Math.round(y + 1), 0);
                                AbstractGui.drawString(poseStack, Minecraft.getInstance().font, new StringTextComponent(displayCount), 0, 0, color);
                            }
                            poseStack.popPose();

                            ++ xMultiplier;
                        }
                    }
                }

                poseStack.popPose();
            }
        }
    }
}