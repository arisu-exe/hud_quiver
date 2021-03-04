package io.github.fallOut015.hud_quiver;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    static {
        interpolation = 0;
        lastHeld = null;
        lastReadyArrows = null;
        WIDGETS = new ResourceLocation("hud_quiver","textures/gui/widgets.png");
    }

    public MainHUDQuiver() {
        MinecraftForge.EVENT_BUS.register(this);
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
        @SuppressWarnings("unused")
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            @Nullable ClientPlayerEntity player = Minecraft.getInstance().player;

            if(event.getType() == RenderGameOverlayEvent.ElementType.ALL && player != null) {
                ItemStack playerHand;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemMainhand();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f) {
                        interpolation += 0.01f;
                    }
                } else if(player.getHeldItemOffhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemOffhand();
                    lastHeld = playerHand;

                    if(interpolation < 1.0f) {
                        interpolation += 0.01f;
                    }
                } else {
                    if(interpolation > 0) {
                        interpolation -= 0.01f;
                    }

                    if(lastHeld == null) {
                        return;
                    } else {
                        playerHand = lastHeld;
                    }
                }

                event.getMatrixStack().push();
                event.getMatrixStack().translate(16, bezier(interpolation, -16f, 16f), 0);
                Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS);
                AbstractGui.blit(event.getMatrixStack(), -12, -12, 0, 0, 24, 24, 36, 24);
                event.getMatrixStack().pop();

                List<ItemStack> readyArrows;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem || player.getHeldItemOffhand().getItem() instanceof ShootableItem) {
                    readyArrows = findAmmos(player, playerHand);
                    lastReadyArrows = readyArrows;
                } else {
                    readyArrows = lastReadyArrows;
                }
                List<Integer> skips = Lists.newLinkedList();
                int xMultiplier = 0;

                // TODO render multiple arrows for each stack being combined

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
                            float x = 24 * xMultiplier + 16, y = bezier(interpolation, -16f, 16f);

                            event.getMatrixStack().push();
                            event.getMatrixStack().translate(x, y, i + 1);
                            event.getMatrixStack().scale(16, -16, 1);
                            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                            // TODO brighten
                            // TODO half opacity other arrows
                            Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.GUI, 16777215, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                            buffer.finish();
                            event.getMatrixStack().pop();

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
                                AbstractGui.blit(event.getMatrixStack(), -6, -4, 24, 0, 12, 8, 36, 24);
                            } else {
                                boolean using = player.getItemInUseCount() > 0 && readyArrow == player.findAmmo(playerHand) && player.getActiveItemStack().getItem() instanceof ShootableItem;
                                String displayCount = using ? String.valueOf(count - 1) : String.valueOf(count);
                                int length = displayCount.length();
                                int color = using ? (count - 1 == 0 ? 16733525 : 16777045) : 16777215;
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
}